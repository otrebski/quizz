package quizz.web

import java.time.Instant
import java.util.{Date, UUID}

import cats.effect.IO
import cats.implicits._
import mindmup.Parser
import quizz.data.MindmupStore
import quizz.feedback.FeedbackSender
import quizz.model.Quizz
import quizz.tracking.Tracking
import quizz.web.Api.{AddQuizzResponse, FeedbackResponse, QuizzQuery, Quizzes}
import sttp.model.{Cookie, CookieValueWithMeta}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

object RouteProviders {

  private def generateCookie(maybeCookie: Option[Cookie]): CookieValueWithMeta =
    CookieValueWithMeta(
      value = maybeCookie.map(_.value).getOrElse(UUID.randomUUID().toString),
      expires = Some(Instant.now().plusSeconds(10 * 60)),
      maxAge = Some(10 * 60),
      domain = None,
      path = Some("/"),
      secure = false,
      httpOnly = true,
      otherDirectives = Map.empty
    )

  implicit val idConvert: QuizzQuery => QuizzQuery = q => q
  implicit val feedbackConvert: Api.FeedbackSend => QuizzQuery = fs =>
    QuizzQuery(fs.quizzId, fs.path)

  def track[In, Out, Error](tracking: Tracking[IO], f: In => Future[Either[Error, Out]])(
      requestAndCookie: (In, List[Cookie])
  )(implicit convert: In => QuizzQuery): Future[Either[Error, (Out, CookieValueWithMeta)]] = {
    val (request, cookies)                    = requestAndCookie
    val cookie                                = generateCookie(cookies.find(_.name == "session"))
    implicit val ec: ExecutionContextExecutor = ExecutionContext.global
    val quizzQuery                            = convert.apply(request)
    val result = for {
      _ <-
        tracking
          .step(quizzQuery.id, quizzQuery.path, Instant.now(), cookie.value, none[String])
          .unsafeToFuture()
      r <- f.apply(request)
      withCookie = r.map(x => (x, cookie))
    } yield withCookie
    result
  }

  def routeWithPathProvider(
      store: MindmupStore[IO]
  )(
      request: Api.QuizzQuery
  ): Future[Either[String, Api.QuizzState]] = {
    import mindmup._
    (for {
      q <-
        store
          .load(request.id)
          .map(s => Parser.parseInput(request.id, s).flatMap(_.toQuizz))
      result = q.flatMap(quizzes => Logic.calculateState(request, Map(request.id -> quizzes)))
    } yield result).unsafeToFuture()
  }

  def quizListProvider(
      quizzStore: MindmupStore[IO]
  ): List[Cookie] => Future[Either[Unit, Api.Quizzes]] = { cookie =>
    import mindmup._
    val r: IO[Quizzes] = for {
      ids <- quizzStore.listNames()
      errorOrQuizzList <-
        ids.toList
          .traverse(id =>
            quizzStore
              .load(id)
              .map(string => Parser.parseInput(id, string).flatMap(_.toQuizz))
              .map {
                case Left(error)  => Left(Api.QuizzErrorInfoInfo(id, error))
                case Right(value) => Right(Api.QuizzInfo(id, value.name))
              }
          )
      (errors, quizzes) = errorOrQuizzList.partitionMap(identity)
    } yield Quizzes(quizzes, errors)
    r.redeem(error => Left(()), v => Right(v))
      .unsafeToFuture()
  }

  def addQuizzProvider(
      store: MindmupStore[IO]
  )(request: Api.AddQuizz): Future[Either[Unit, AddQuizzResponse]] = {
    import mindmup._
    val id: Either[String, V3IdString.Mindmap] =
      Parser.parseInput(request.id, request.mindmupSource)
    val newQuizzOrError: Either[String, Quizz] = id.flatMap(_.toQuizz).map(_.copy(id = request.id))

    newQuizzOrError match {
      case Left(error) => Future.failed(new Exception(error))
      case Right(_) =>
        store
          .store(request.id, request.mindmupSource)
          .map(_ => AddQuizzResponse("added").asRight[Unit])
          .unsafeToFuture()
    }
  }

  def deleteQuizzProvider(
      store: MindmupStore[IO]
  )(request: Api.DeleteQuizz): Future[Either[Unit, Unit]] =
    store
      .delete(request.id)
      .map(_.asRight[Unit])
      .unsafeToFuture()

  def feedbackProvider(store: MindmupStore[IO], feedbackSenders: List[FeedbackSender[IO]])(
      feedback: Api.FeedbackSend
  ): Future[Either[String, FeedbackResponse]] = {
    val request = Api.QuizzQuery(feedback.quizzId, feedback.path)
    val quizzState: IO[Either[String, Api.QuizzState]] = store
      .load(request.id)
      .map(string =>
        Parser
          .parseInput(request.id, string)
          .flatMap(_.toQuizz)
          .flatMap(quizz => Logic.calculateState(request, Map(request.id -> quizz)))
      )
    //TODO pass cookie to feedback
    val p: IO[Either[String, FeedbackResponse]] = quizzState.flatMap {
      case Right(quizzState) =>
        feedbackSenders
          .traverse(_.send(feedback, quizzState))
          .map(_ => FeedbackResponse("OK").asRight)
      case Left(error) => IO.raiseError(new Exception(s"Can't process feedback: $error"))
    }
    p.unsafeToFuture()
  }

  val validateProvider: String => Future[Either[Unit, Api.ValidationResult]] = { s =>
    val result = mindmup.Parser.parseInput("validation_test", s) match {
      case Left(error) => Api.ValidationResult(valid = false, List(error))
      case Right(_)    => Api.ValidationResult(valid = true, List.empty[String])
    }
    Future.successful(Right(result))
  }

  def trackingSessionsProvider(
      tracking: Tracking[IO]
  ): Unit => Future[Either[String, Api.TrackingSessions]] = { _ =>
    tracking
      .listSessions()
      .map(s =>
        s.map(ts =>
          Api
            .TrackingSession(
              quizzId = ts.quizzId,
              session = ts.session,
              date = ts.date,
              duration = ts.duration
            )
        )
      )
      .map(Api.TrackingSessions)
      .map(_.asRight[String])
      .unsafeToFuture()
  }

  def trackingSessionProvider(
      tracking: Tracking[IO]
  )(query: Api.TrackingSessionHistoryQuery): Future[Either[String, Api.TrackingSessionHistory]] =
    tracking
      .session(query.session, query.quizzId)
      .map { list =>
        val dates = list.map(_.date)
        val min   = dates.minOption.getOrElse(new Date(0))
        val max   = dates.maxOption.getOrElse(new Date(0))
        val details = Api.TrackingSession(
          session = query.session,
          date = min,
          quizzId = query.quizzId,
          duration = max.getTime - min.getTime
        )
        val steps =
          list.map(ts => Api.TrackingStep(ts.quizzId, ts.path, ts.date, ts.session, ts.username))
        Api.TrackingSessionHistory(details, steps).asRight[String]
      }
      .unsafeToFuture()

}
