package quizz.web

import io.circe.generic.auto._
import quizz.web.Api.DeleteQuizz
import sttp.model.{ Cookie, CookieValueWithMeta }
import sttp.tapir.json.circe._
import sttp.tapir.{ path, _ }
import sttp.tapir.setCookie

object Endpoints {
  val routeEndpoint: Endpoint[
    (Api.QuizzQuery, List[Cookie]),
    String,
    (Api.QuizzState, CookieValueWithMeta),
    Nothing
  ] = endpoint.get
    .in(
      ("api" / "quiz" / path[String]("id") / "path" / path[String]("quizPath"))
        .mapTo(Api.QuizzQuery)
    )
    .in(cookies)
    .errorOut(stringBody)
    .out(jsonBody[Api.QuizzState])
    .out(setCookie("session"))

  val routeEndpointStart: Endpoint[
    (Api.QuizzQuery, List[Cookie]),
    String,
    (Api.QuizzState, CookieValueWithMeta),
    Nothing
  ] = endpoint.get
    .in(("api" / "quiz" / path[String]("id") / "path").mapTo(id => Api.QuizzQuery(id, "")))
    .in(cookies)
    .errorOut(stringBody)
    .out(jsonBody[Api.QuizzState])
    .out(setCookie("session"))

  val listQuizzes: Endpoint[List[Cookie], Unit, Api.Quizzes, Nothing] = endpoint.get
    .in("api" / "quiz")
    .in(cookies)
    .out(jsonBody[Api.Quizzes])

  val addQuizz: Endpoint[Api.AddQuizz, Unit, Api.AddQuizzResponse, Nothing] = endpoint.put
    .in("api" / "quiz" / path[String](name = "id").description("Id of quizz to add/replace"))
    .in(stringBody("UTF-8"))
    .mapIn(idAndContent => Api.AddQuizz(idAndContent._1, idAndContent._2))(a =>
      (a.id, a.mindmupSource)
    )
    .out(jsonBody[Api.AddQuizzResponse])

  val deleteQuizz: Endpoint[DeleteQuizz, Unit, Unit, Nothing] = endpoint.delete
    .in("api" / "quiz" / path[String](name = "id").description("Id of quizz to delete"))
    .mapIn(id => DeleteQuizz(id))(_.id)
    .out(emptyOutput)

  val feedback: Endpoint[(Api.FeedbackSend, List[Cookie]), Unit, Api.FeedbackResponse, Nothing] =
    endpoint.post
      .in("api" / "feedback")
      .in(jsonBody[Api.FeedbackSend].description("Feedback from user"))
      .in(cookies)
      .out(jsonBody[Api.FeedbackResponse])

  val validateEndpoint: Endpoint[String, Unit, Api.ValidationResult, Nothing] = endpoint.post
    .in("api" / "quizz" / "validate" / "mindmup")
    .in(stringBody("UTF-8"))
    .out(jsonBody[Api.ValidationResult])
}
