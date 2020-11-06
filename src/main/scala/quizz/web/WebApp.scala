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
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse}
import akka.http.scaladsl.server.RejectionHandler
import cats.effect.{Clock, ExitCode, IO, IOApp}
import cats.implicits._
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import quizz.data.{DbMindMupStore, FileMindmupStore, MemoryMindmupStore, MindmupStore}
import quizz.db.DatabaseInitializer
import quizz.feedback.{FeedbackDBSender, FeedbackSender, LogFeedbackSender, SlackFeedbackSender}
import tapir.server.akkahttp._

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.io.Source
import scala.util.{Failure, Try}

object WebApp extends IOApp with LazyLogging {

  private val config: Config = ConfigFactory.load()

  override def run(args: List[String]): IO[ExitCode] = {

    val logSender = new LogFeedbackSender[IO].some
    val feedbackSlack =
      if (config.getBoolean("feedback.slack.use"))
        Some(new SlackFeedbackSender[IO](config.getString("feedback.slack.token")))
      else none[FeedbackSender[IO]]

    val databaseConfig = quizz.db.databaseConfig(config)
    val databaseFeedbackSender: Option[FeedbackDBSender] = {
      if (config.getBoolean("feedback.database.use")) {
        val a: Clock[IO] = implicitly[Clock[IO]]
        new FeedbackDBSender(databaseConfig)(a).some
      } else
        None
    }
    val feedbackSenders = List(logSender, feedbackSlack, databaseFeedbackSender).flatten

    import akka.http.scaladsl.server.Directives._
    val port = 8080

    def bindingFuture(store: MindmupStore[IO]): IO[Future[Http.ServerBinding]] = {
      import better.files.File.home
      import Endpoints._
      import RouteProviders._
      import akka.actor.ActorSystem
      import akka.stream.ActorMaterializer

      IO {
        val route         = routeEndpoint.toRoute(routeWithPathProvider(store))
        val routeStart    = routeEndpointStart.toRoute(routeWithoutPathProvider(store))
        val routeList     = listQuizzes.toRoute(quizListProvider(store))
        val routeFeedback = feedback.toRoute(feedbackProvider(store, feedbackSenders))
        val add           = addQuizz.toRoute(addQuizzProvider(store))
        val delete        = deleteQuizz.toRoute(deleteQuizzProvider(store))
        val validateRoute = validateEndpoint.toRoute(validateProvider)
        val static        = getFromResourceDirectory("gui")
        import akka.http.scaladsl.model.StatusCodes._
        implicit val catchAll: RejectionHandler = RejectionHandler
          .newBuilder()
          .handleNotFound {
            val response = Try(Source.fromResource("gui/index.html").mkString) match {
              case scala.util.Success(value) => HttpResponse(OK, Seq(), HttpEntity(ContentTypes.`text/html(UTF-8)`, value))
              case Failure(_) => HttpResponse(NotFound, Seq(), HttpEntity(ContentTypes.`text/plain(UTF-8)`, "Not found"))
            }
            respondWithHeader(RawHeader("Content-Typex", "text/html; charset=UTF-8")) {
              complete(response)
            }
          }
          .result()

        implicit val system: ActorSystem                        = ActorSystem("my-system")
        implicit val materializer: ActorMaterializer            = ActorMaterializer()
        implicit val executionContext: ExecutionContextExecutor = system.dispatcher
        Http().bindAndHandle(
          route ~ routeStart ~ routeList ~ routeFeedback ~ add ~ delete ~ validateRoute ~ static,
          "0.0.0.0",
          port
        )
      }
    }

    val mindmupStoreType = config.getString("mindmup.store-type")
    val initDb =
      if (config.getBoolean("feedback.database.use") || mindmupStoreType == "database")
        DatabaseInitializer.initDatabase(quizz.db.databaseConfig(config))
      else
        IO.unit

    val createMindmupStore: IO[MindmupStore[IO]] =
    IO(logger.info(s"Will use $mindmupStoreType for storing mindmups")) *> {
      mindmupStoreType match {
        case "file" =>
          import better.files.File.currentWorkingDirectory
          import better.files._
          val str = config.getString("filestorage.dir")
          val dir = if (str.startsWith("/")) File.apply(str) else currentWorkingDirectory / str
          FileMindmupStore[IO](dir)
        case "memory"   => MemoryMindmupStore[IO]
        case "database" => DbMindMupStore[IO](databaseConfig)
      }
    }

    logger.info(s"Server is online on port $port")
    for {
      _            <- initDb
      mindmupStore <- createMindmupStore
      _            <- bindingFuture(mindmupStore)
    } yield ExitCode.Success
  }
}
