/*
 * Copyright 2019 k.otrebski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package quizz.web

import java.io.File

import cats.effect.concurrent.Ref
import cats.effect.{ ExitCode, IO, IOApp }
import cats.implicits._

import scala.concurrent.{ ExecutionContextExecutor, Future }
import com.typesafe.config.{ Config, ConfigFactory }
import com.typesafe.scalalogging.LazyLogging
import io.circe
import io.circe.generic.auto._
import mindmup.Parser
import quizz.data.{ ExamplesData, Loader }
import quizz.model.Quizz
import quizz.web.WebApp.Api.{ AddQuizzResponse, FeedbackResponse, Quizzes }
import tapir.json.circe._
import tapir.server.akkahttp._
import tapir.{ path, _ }

object WebApp extends IOApp with LazyLogging {

  object Api {

    case class QuizzQuery(id: String, path: String)

    case class QuizzId(id: String)

    case class QuizzState(path: String, currentStep: Step, history: List[HistoryStep] = List.empty)

    case class Step(
        id: String,
        question: String,
        answers: List[Answer] = List.empty,
        success: Option[Boolean] = None
    )

    case class HistoryStep(
        id: String,
        question: String,
        answers: List[Answer] = List.empty,
        path: List[String] = List.empty,
        success: Option[Boolean] = None
    )

    case class Answer(id: String, text: String, selected: Option[Boolean] = None)

    case class QuizzInfo(id: String, title: String)

    case class Quizzes(quizzes: List[QuizzInfo])

    case class Feedback(quizzId: String, path: String, rate: Int, comment: String)

    case class FeedbackResponse(status: String)

    case class AddQuizz(id: String, mindmupSource: String)

    case class AddQuizzResponse(status: String)

  }

  val stateCodec            = jsonBody[Api.QuizzState]
  val codecQuizInfo         = jsonBody[Api.Quizzes]
  val codecFeedback         = jsonBody[Api.Feedback]
  val codecFeedbackResponse = jsonBody[Api.FeedbackResponse]

  private val config: Config         = ConfigFactory.load()
  private val dirWithQuizzes: String = config.getString("quizz.loader.dir")
  logger.info(s"""Loading from "$dirWithQuizzes" """)

  val routeEndpoint = endpoint.get
    .in(
      ("api" / "quiz" / path[String]("id") / "path" / path[String]("quizPath"))
        .mapTo(Api.QuizzQuery)
    )
    .errorOut(stringBody)
    .out(jsonBody[Api.QuizzState])

  val routeEndpointStart = endpoint.get
    .in(("api" / "quiz" / path[String]("id") / "path").mapTo(Api.QuizzId))
    .errorOut(stringBody)
    .out(jsonBody[Api.QuizzState])

  val listQuizzes = endpoint.get
    .in("api" / "quiz")
    .out(jsonBody[Api.Quizzes])

  val addQuizz = endpoint.put
    .in("api" / "quizz" / path[String](name = "id").description("Id of quizz to add/replace"))
    .in(stringBody("UTF-8"))
    .mapIn(idAndContent => Api.AddQuizz(idAndContent._1, idAndContent._2))(a =>
      (a.id, a.mindmupSource)
    )
    .out(jsonBody[Api.AddQuizzResponse])

  val feedback = endpoint.post
    .in("api" / "feedback")
    .in(jsonBody[Api.Feedback].description("Feedback from user"))
    .out(jsonBody[Api.FeedbackResponse])

  import akka.actor.ActorSystem
  import akka.http.scaladsl.Http
  import akka.stream.ActorMaterializer

  private def routeWithoutPathProvider(
      quizzes: Ref[IO, Either[String, Map[String, Quizz]]]
  )(request: Api.QuizzId): Future[Either[String, Api.QuizzState]] = {
    val a: IO[Either[String, Api.QuizzState]] = for {
      quizzesMap <- quizzes.get
      quizz = quizzesMap.flatMap(q =>
        q.get(request.id).map(_.asRight[String]).getOrElse("Not found".asLeft[Quizz])
      )
      step = quizz.flatMap(q => Logic.calculateStateOnPathStart(q.firstStep))
    } yield step

    a.unsafeToFuture()
  }

  private def routeWithPathProvider(
      quizzes: Ref[IO, Either[String, Map[String, Quizz]]]
  )(request: Api.QuizzQuery) = {
    val r: IO[Either[String, Api.QuizzState]] = for {
      q <- quizzes.get
      result = q.flatMap(quizzes => Logic.calculateStateOnPath(request, quizzes))
    } yield result
    r.unsafeToFuture()
  }

  def quizListProvider(
      quizzes: Ref[IO, Either[String, Map[String, Quizz]]]
  ): Unit => Future[Either[Unit, Api.Quizzes]] = { _ =>
    (for {
      quizzesOrError <- quizzes.get
      quizzesInfo =
        quizzesOrError
          .map(q => q.values.toList.map(q1 => Api.QuizzInfo(q1.id, q1.name)))
          .map(Api.Quizzes)
          .leftMap(_ => ())
    } yield quizzesInfo).unsafeToFuture()
  }

  def addQuizzProvider(
      quizzes: Ref[IO, Either[String, Map[String, Quizz]]]
  )(request: Api.AddQuizz) = {
    import mindmup._
    val newQuizzOrError: Either[circe.Error, Quizz] =
      Parser.parseInput(request.mindmupSource).map(_.toQuizz.copy(id = request.id))

    newQuizzOrError match {
      case Left(error)  => Future.failed(new Exception(error.toString))
      case Right(quizz) =>
//        logger.info(s"Adding quizz $quizz")
        val io: IO[Either[String, Map[String, Quizz]]] =
          quizzes.getAndUpdate(old => old.map(_.updated(request.id, quizz)))
        io.map(_ => AddQuizzResponse("Added").asRight)
          .unsafeToFuture()
    }
  }

  def feedbackProvider(feedback: Api.Feedback): Future[Either[Unit, FeedbackResponse]] = {
    logger.info(s"Have feedback: $feedback")
    Future.successful(Right(FeedbackResponse("OK")))
  }

  override def run(args: List[String]): IO[ExitCode] = {
    import akka.http.scaladsl.server.Directives._

    val port = 8080

    def bindingFuture(
        quizzes: Ref[IO, Either[String, Map[String, Quizz]]]
    ): IO[Future[Http.ServerBinding]] =
      IO {
        val route                                               = routeEndpoint.toRoute(routeWithPathProvider(quizzes))
        val routeStart                                          = routeEndpointStart.toRoute(routeWithoutPathProvider(quizzes))
        val routeList                                           = listQuizzes.toRoute(quizListProvider(quizzes))
        val routeFeedback                                       = feedback.toRoute(feedbackProvider)
        val add                                                 = addQuizz.toRoute(addQuizzProvider(quizzes))
        implicit val system: ActorSystem                        = ActorSystem("my-system")
        implicit val materializer: ActorMaterializer            = ActorMaterializer()
        implicit val executionContext: ExecutionContextExecutor = system.dispatcher
        Http().bindAndHandle(route ~ routeStart ~ routeList ~ routeFeedback ~ add, "0.0.0.0", port)
      }

    def loadQuizzes(): IO[Either[String, Map[String, Quizz]]] =
      IO {
        if (dirWithQuizzes.nonEmpty) {
          val list = Loader.fromFolder(new File(dirWithQuizzes))
          list.map(errorOr => errorOr.map(q => q.id -> q).toMap)
        } else
          Right(ExamplesData.quizzes)
      }

    logger.info(s"Server is online on port $port")
    for {
      quizzesRef <-
        Ref.of[IO, Either[String, Map[String, Quizz]]]("Not yet loaded".asLeft[Map[String, Quizz]])
      quizzesOrError <- loadQuizzes()
      _              <- quizzesRef.set(quizzesOrError)
      _              <- bindingFuture(quizzesRef)
    } yield ExitCode.Success
  }
}
