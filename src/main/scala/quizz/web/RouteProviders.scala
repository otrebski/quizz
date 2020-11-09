package quizz.web

import java.time.Instant
import java.util.UUID

import cats.effect.IO
import cats.implicits._
import io.circe
import mindmup.Parser
import quizz.data.MindmupStore
import quizz.feedback.FeedbackSender
import quizz.model.Quizz
import quizz.tracking.Tracking
import quizz.web.Api.{ AddQuizzResponse, FeedbackResponse, Quizzes }
import sttp.model.{ Cookie, CookieValueWithMeta }

import scala.concurrent.Future

object RouteProviders {

  def generateCookie(maybeCookie: Option[Cookie]): CookieValueWithMeta =
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

  def routeWithoutPathProvider(
      store: MindmupStore[IO],
      tracking: Tracking[IO]
  )(
      requestAndCookie: (Api.QuizzId, List[Cookie])
  ): Future[Either[String, (Api.QuizzState, CookieValueWithMeta)]] = {
    import mindmup._
    val (request, cookies) = requestAndCookie
    val cookie             = generateCookie(cookies.find(_.name == "session"))
    val r = for {
      _ <- tracking.step(request.id, "", Instant.now(), cookie.value, none[String])
      quizzString <- store.load(request.id)
      quizzOrError =
        Parser.parseInput(request.id, quizzString).map(_.toQuizz).left.map(_.getMessage)
      result          = quizzOrError.flatMap(q => Logic.calculateStateOnPathStart(q.firstStep))
      resultAndCookie = result.map(q => (q, cookie))
    } yield resultAndCookie
    r.unsafeToFuture()
  }

  def routeWithPathProvider(
      store: MindmupStore[IO],
      tracking: Tracking[IO]
  )(
      requestAndCookie: (Api.QuizzQuery, List[Cookie])
  ): Future[Either[String, (Api.QuizzState, CookieValueWithMeta)]] = {
    import mindmup._
    val (request, cookies) = requestAndCookie
    val cookie             = generateCookie(cookies.find(_.name == "session"))
    val r: IO[Either[String, (Api.QuizzState, CookieValueWithMeta)]] = for {
      _ <- tracking.step(request.id, request.path, Instant.now(), cookie.value, none[String])
      q <-
        store
          .load(request.id)
          .map(s => Parser.parseInput(request.id, s).map(_.toQuizz).left.map(_.getMessage))
      result = q.flatMap(quizzes => Logic.calculateStateOnPath(request, Map(request.id -> quizzes)))
      x      = result.map(q => (q, cookie))
    } yield x
    r.unsafeToFuture()
  }

  def quizListProvider(
      quizzStore: MindmupStore[IO]
  ): List[Cookie] => Future[Either[Unit, Api.Quizzes]] = { cookie =>
    import mindmup._
    println(cookie)
    val r: IO[Quizzes] = for {
      ids <- quizzStore.listNames()
      errorOrQuizzList <-
        ids.toList
          .traverse(id =>
            quizzStore
              .load(id)
              .map(string => Parser.parseInput(id, string).map(_.toQuizz))
              .map {
                case Left(error)  => Left(Api.QuizzErrorInfoInfo(id, error.getMessage))
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
    val newQuizzOrError: Either[circe.Error, Quizz] =
      Parser.parseInput(request.id, request.mindmupSource).map(_.toQuizz.copy(id = request.id))

    newQuizzOrError match {
      case Left(error) => Future.failed(new Exception(error.toString))
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
      //          .store(request.id, request.mindmupSource)
      //          .map(_ => AddQuizzResponse("added").asRight[Unit])
      .map(_.asRight[Unit])
      .unsafeToFuture()

  def feedbackProvider(store: MindmupStore[IO], feedbackSenders: List[FeedbackSender[IO]])(
      feedbackAndCookies: (Api.FeedbackSend, List[Cookie])
  ): Future[Either[Unit, FeedbackResponse]] = {
    val (feedback, cookies) = feedbackAndCookies
    val request             = Api.QuizzQuery(feedback.quizzId, feedback.path)
    val quizzState: IO[Either[String, Api.QuizzState]] = store
      .load(request.id)
      .map(string =>
        Parser
          .parseInput(request.id, string)
          .map(_.toQuizz)
          .left
          .map(_.toString)
          .flatMap(quizz => Logic.calculateStateOnPath(request, Map(request.id -> quizz)))
      )
    //TODO pass cookie to feedback
    val p: IO[Either[Unit, FeedbackResponse]] = quizzState.flatMap {
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
      case Left(error) => Api.ValidationResult(valid = false, List(error.getMessage))
      case Right(_)    => Api.ValidationResult(valid = true, List.empty[String])
    }
    Future.successful(Right(result))
  }
}
