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

import scala.concurrent.{ ExecutionContextExecutor, Future }

import io.circe.generic.auto._
import quizz.data.ExamplesData
import quizz.engine.QuizzEngine
import quizz.model.{ FailureStep, Question, SuccessStep }
import tapir.json.circe._
import tapir.server.akkahttp._
import tapir.{ path, _ }

object WebApp extends App {

  object Api {
    case class QuizzQuery(id: String, path: String)
    case class QuizzId(id: String)
    case class QuizzState(path: String, currentStep: Step, history: List[Step] = List.empty)
    case class Step(id: String,
                    question: String,
                    answers: List[Answer] = List.empty,
                    success: Option[Boolean] = None)
    case class Answer(id: String, text: String, selected: Boolean = false)

    case class QuizzInfo(id: String, title: String)
    case class Quizzes(quizzes: List[QuizzInfo])

    case class Feedback(quizzId: String, path: String, rate: Int, comment: String)

  }

  val stateCodec    = jsonBody[Api.QuizzState]
  val codecQuizInfo = jsonBody[Api.Quizzes]
  val codecFeedback = jsonBody[Api.Feedback]

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

  val feedback = endpoint.post
    .in("api" / "feedback")
    .in(jsonBody[Api.Feedback].description("Feedback from user"))
    .out(stringBody)

  import akka.actor.ActorSystem
  import akka.http.scaladsl.Http
  import akka.stream.ActorMaterializer

  def routeProvider3(request: Api.QuizzId): Future[Either[String, Api.QuizzState]] = {
    val answers = ExamplesData.quiz.answers.map(kv => Api.Answer(kv._2.id, kv._1)).toList
    val r = Right(
      Api.QuizzState(
        path = "",
        currentStep =
          Api.Step(id = ExamplesData.quiz.id, question = ExamplesData.quiz.text, answers = answers)
      )
    )
    Future.successful(r)
  }

  def routeProvider2(request: Api.QuizzQuery): Future[Either[String, Api.QuizzState]] = {
    val path = request.path.split(";").toList.reverse
    val history = QuizzEngine
      .history(ExamplesData.quiz, path)
      .map(
        h =>
          h.map {
            case q: Question =>
              val answers = q.answers.map { a =>
                Api.Answer(a._2.id, a._2.text, path.contains(a._2.id))
              }.toList
              Api.Step(q.id, q.text, answers)
            case f: FailureStep => Api.Step(f.id, f.text, success = Some(false))
            case s: SuccessStep  => Api.Step(s.id, s.text, success = Some(true))
        }
      )
      .getOrElse(List.empty)

    val x: Either[String, Api.QuizzState] = path match {
      case head :: Nil if head == "" =>
        val answers = ExamplesData.quiz.answers.map(kv => Api.Answer(kv._2.id, kv._1)).toList
        Right(
          Api.QuizzState(
            path = "",
            currentStep = Api.Step(id = ExamplesData.quiz.id,
                                   question = ExamplesData.quiz.text,
                                   answers = answers)
          )
        )

      case head :: tail =>
        val r: Either[String, QuizzEngine.SelectionResult] =
          QuizzEngine.process(head, ExamplesData.quiz, tail)

        r.map(_.current)
          .map {
            case q: Question =>
              val currentStep = Api.Step(
                id = q.id,
                question = q.text,
                answers = q.answers.map(kv => Api.Answer(kv._2.id, kv._1)).toList
              )
              Api.QuizzState(
                path = request.path,
                currentStep = currentStep
              )
            case f: FailureStep =>
              Api
                .QuizzState(path = request.path,
                            currentStep =
                              Api.Step(id = f.id, question = f.text, success = Some(false)))
            case f: SuccessStep =>
              Api
                .QuizzState(
                  path = request.path,
                  currentStep = Api.Step(id = f.id, question = f.text, success = Some(true))
                )
          }

    }
    Future.successful(x.map(_.copy(history = history)))

  }

  val quizListProvider: Unit => Future[Either[Unit, Api.Quizzes]] = _ => {
    Future.successful(
      Right(Api.Quizzes(quizzes = ExamplesData.quizzes.map(q => Api.QuizzInfo(q.id, q.name))))
    )
  }

  def feedbackProvider(feedback: Api.Feedback): Future[Either[Unit, String]] = {
    println(s"Have feedback: $feedback")
    Future.successful(Right("OK"))
  }

  import akka.http.scaladsl.server.Directives._
  val route         = routeEndpoint.toRoute(routeProvider2)
  val routeStart    = routeEndpointStart.toRoute(routeProvider3)
  val routeList     = listQuizzes.toRoute(quizListProvider)
  val routeFeedback = feedback.toRoute(feedbackProvider)

  implicit val system: ActorSystem                        = ActorSystem("my-system")
  implicit val materializer: ActorMaterializer            = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val bindingFuture =
    Http().bindAndHandle(route ~ routeStart ~ routeList ~ routeFeedback, "0.0.0.0", 8080)

  println(s"Server is online")

}
