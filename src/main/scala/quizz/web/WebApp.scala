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

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.`Content-Type`
import akka.http.scaladsl.model.{ ContentTypes, HttpEntity, HttpResponse }
import akka.http.scaladsl.server.RejectionHandler
import cats.effect.{ ExitCode, IO, IOApp }
import cats.implicits._
import com.typesafe.config.{ Config, ConfigFactory }
import com.typesafe.scalalogging.LazyLogging
import quizz.data.{ DbMindMupStore, FileMindmupStore, MemoryMindmupStore, MindmupStore }
import quizz.db.DatabaseInitializer
import quizz.feedback.{ FeedbackDBSender, FeedbackSender, LogFeedbackSender, SlackFeedbackSender }
import quizz.tracking.{ DbTracking, FileTracking, MemoryTracking, Tracking }
import sttp.tapir.server.akkahttp._

import scala.concurrent.{ ExecutionContextExecutor, Future }
import scala.io.Source
import scala.util.{ Failure, Try }
import sttp.tapir._
import sttp.tapir.docs.openapi._
import sttp.tapir.openapi.circe.yaml.RichOpenAPI
import sttp.tapir.swagger.akkahttp.SwaggerAkka

object WebApp extends IOApp with LazyLogging {

  private val config: Config = ConfigFactory.load()

  override def run(args: List[String]): IO[ExitCode] = {

    import akka.http.scaladsl.server.Directives._
    val port = 8080

    def bindingFuture(
        store: MindmupStore[IO],
        tracking: Tracking[IO],
        feedbackSenders: List[FeedbackSender[IO]]
    ): IO[Future[Http.ServerBinding]] = {
      import Endpoints._
      import RouteProviders._
      import akka.actor.ActorSystem

      IO {
        val route      = routeEndpoint.toRoute(track(tracking, routeWithPathProvider(store)))
        val routeStart = routeEndpointStart.toRoute(track(tracking, routeWithPathProvider(store)))
        val routeList  = listQuizzes.toRoute(quizListProvider(store))
        val routeFeedback =
          feedback.toRoute(track(tracking, feedbackProvider(store, feedbackSenders)))
        val add                                    = addQuizz.toRoute(addQuizzProvider(store))
        val delete                                 = deleteQuizz.toRoute(deleteQuizzProvider(store))
        val validateRoute                          = validateEndpoint.toRoute(validateProvider)
        val trackingSessionsRoute                  = trackingSessions.toRoute(trackingSessionsProvider(tracking))
        val trackingSessionRoute                   = trackingSession.toRoute(trackingSessionProvider(tracking))
        val static                                 = getFromResourceDirectory("gui")
        val myEndpoints: Seq[Endpoint[_, _, _, _]] = Endpoints.allEndpoints
        val docsAsYaml: String                     = myEndpoints.toOpenAPI("Quizz", "?").toYaml
        val swagger                                = new SwaggerAkka(docsAsYaml).routes
        import akka.http.scaladsl.model.StatusCodes._
        implicit val catchAll: RejectionHandler = RejectionHandler
          .newBuilder()
          .handleNotFound {
            val response = Try(Source.fromResource("gui/index.html").mkString) match {
              case scala.util.Success(value) =>
                HttpResponse(OK, Seq(), HttpEntity(ContentTypes.`text/html(UTF-8)`, value))
              case Failure(_) =>
                HttpResponse(
                  NotFound,
                  Seq(),
                  HttpEntity(ContentTypes.`text/plain(UTF-8)`, "Not found")
                )
            }
            respondWithHeaders(`Content-Type`(ContentTypes.`text/html(UTF-8)`)) {
              complete(response)
            }
          }
          .result()

        implicit val system: ActorSystem                        = ActorSystem("my-system")
        implicit val executionContext: ExecutionContextExecutor = system.dispatcher
        Http().bindAndHandle(
          route ~ routeStart ~ routeList
          ~ routeFeedback
          ~ add ~ delete ~ validateRoute
          ~ trackingSessionsRoute
          ~ trackingSessionRoute
          ~ swagger
          ~ static,
          "0.0.0.0",
          port
        )
      }
    }

    case class Services(
        mindmupStore: MindmupStore[IO],
        tracking: Tracking[IO],
        feedbackSender: List[FeedbackSender[IO]]
    )
    val storeType = config.getString("persistence.type")

    val createMindmupStore: IO[Services] = {
      val feedbackSlack =
        if (config.getBoolean("feedback.slack.use"))
          Some(new SlackFeedbackSender[IO](config.getString("feedback.slack.token")))
        else none[FeedbackSender[IO]]

      IO(logger.info(s"Will use $storeType for storing data")) *> {
        storeType match {
          case "file" =>
            import better.files.File.currentWorkingDirectory
            import better.files._
            val str = config.getString("filestorage.dir")
            val dir =
              if (str.startsWith("/")) File.apply(str)
              else currentWorkingDirectory / str
            for {
              store    <- FileMindmupStore[IO](dir / "mindmups")
              tracking <- FileTracking[IO](dir)
            } yield Services(
              store,
              tracking,
              List(new LogFeedbackSender[IO]) ::: feedbackSlack.toList
            )
          case "memory" =>
            for {
              store    <- MemoryMindmupStore[IO]()
              tracking <- MemoryTracking[IO]()
            } yield Services(
              store,
              tracking,
              List(new LogFeedbackSender[IO]) ::: feedbackSlack.toList
            )
          case "database" =>
            for {
              transactor <- DatabaseInitializer.createTransactor(quizz.db.databaseConfig(config))
              _          <- DatabaseInitializer.initDatabase(transactor)
              store      <- DbMindMupStore[IO](transactor)
              tracking   <- DbTracking[IO](transactor)
              feedback = new FeedbackDBSender(transactor)
            } yield Services(store, tracking, List(feedback) ::: feedbackSlack.toList)
        }
      }
    }
    logger.info(s"Starting server on $port")
    for {
      servicesIO <- createMindmupStore
      services = servicesIO
      _ <- bindingFuture(services.mindmupStore, services.tracking, services.feedbackSender)
      _ <- IO(logger.info("Server started"))
    } yield ExitCode.Success
  }
}
