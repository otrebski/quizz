package quizz.web

import io.circe.generic.auto._
import tapir.json.circe._
import tapir.{ path, _ }

object Endpoints {
  val routeEndpoint: Endpoint[Api.QuizzQuery, String, Api.QuizzState, Nothing] = endpoint.get
    .in(
      ("api" / "quiz" / path[String]("id") / "path" / path[String]("quizPath"))
        .mapTo(Api.QuizzQuery)
    )
    .errorOut(stringBody)
    .out(jsonBody[Api.QuizzState])

  val routeEndpointStart: Endpoint[Api.QuizzId, String, Api.QuizzState, Nothing] = endpoint.get
    .in(("api" / "quiz" / path[String]("id") / "path").mapTo(Api.QuizzId))
    .errorOut(stringBody)
    .out(jsonBody[Api.QuizzState])

  val listQuizzes: Endpoint[Unit, Unit, Api.Quizzes, Nothing] = endpoint.get
    .in("api" / "quiz")
    .out(jsonBody[Api.Quizzes])

  val addQuizz: Endpoint[Api.AddQuizz, Unit, Api.AddQuizzResponse, Nothing] = endpoint.put
    .in("api" / "quizz" / path[String](name = "id").description("Id of quizz to add/replace"))
    .in(stringBody("UTF-8"))
    .mapIn(idAndContent => Api.AddQuizz(idAndContent._1, idAndContent._2))(a =>
      (a.id, a.mindmupSource)
    )
    .out(jsonBody[Api.AddQuizzResponse])

  val feedback: Endpoint[Api.FeedbackSend, Unit, Api.FeedbackResponse, Nothing] = endpoint.post
    .in("api" / "feedback")
    .in(jsonBody[Api.FeedbackSend].description("Feedback from user"))
    .out(jsonBody[Api.FeedbackResponse])

  val validateEndpoint: Endpoint[String, Unit, Api.ValidationResult, Nothing] = endpoint.post
    .in("api" / "quizz" / "validate" / "mindmup")
    .in(stringBody("UTF-8"))
    .out(jsonBody[Api.ValidationResult])
}
