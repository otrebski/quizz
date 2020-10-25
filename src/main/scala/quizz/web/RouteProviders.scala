package quizz.web

import cats.effect.IO
import cats.implicits._
import io.circe
import mindmup.Parser
import quizz.data.MindmupStore
import quizz.feedback.FeedbackSender
import quizz.model.Quizz
import quizz.web.Api.{ AddQuizzResponse, FeedbackResponse, Quizzes }

import scala.concurrent.Future

object RouteProviders {

  def routeWithoutPathProvider(
      store: MindmupStore[IO]
  )(request: Api.QuizzId): Future[Either[String, Api.QuizzState]] = {
    import mindmup._
    val a: IO[Either[String, Api.QuizzState]] = for {
      quizzString <- store.load(request.id)
      quizzOrError =
        Parser.parseInput(request.id, quizzString).map(_.toQuizz).left.map(_.getMessage)
      result = quizzOrError.flatMap(q => Logic.calculateStateOnPathStart(q.firstStep))
    } yield result

    a.unsafeToFuture()
  }

  def routeWithPathProvider(
      store: MindmupStore[IO]
  )(request: Api.QuizzQuery): Future[Either[String, Api.QuizzState]] = {
    import mindmup._
    val r: IO[Either[String, Api.QuizzState]] = for {
      q <-
        store
          .load(request.id)
          .map(s => Parser.parseInput(request.id, s).map(_.toQuizz).left.map(_.getMessage))
      result = q.flatMap(quizzes => Logic.calculateStateOnPath(request, Map(request.id -> quizzes)))
    } yield result
    r.unsafeToFuture()
  }

  def quizListProvider(quizzStore: MindmupStore[IO]): Unit => Future[Either[Unit, Api.Quizzes]] = {
    _ =>
      import mindmup._
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

  def feedbackProvider(store: MindmupStore[IO], feedbackSenders: List[FeedbackSender[IO]])(
      feedback: Api.FeedbackSend
  ): Future[Either[Unit, FeedbackResponse]] = {

    val request = Api.QuizzQuery(feedback.quizzId, feedback.path)
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
