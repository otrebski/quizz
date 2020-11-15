package quizz.web

import java.text.SimpleDateFormat
import java.util.Date

import io.circe.Decoder.Result
import io.circe.generic.auto._
import quizz.web.Api.DeleteQuizz
import sttp.model.{Cookie, CookieValueWithMeta}
import sttp.tapir.json.circe._
import sttp.tapir.model.UsernamePassword
import sttp.tapir.server.{PartialServerEndpoint, ServerEndpoint}
import sttp.tapir.path
import sttp.tapir.setCookie
import sttp.tapir._

import scala.collection.immutable.ListMap
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
object Endpoints {
  import io.circe.{ Json, _ }
  implicit val TimestampFormat: Encoder[Date] with Decoder[Date] =
    new Encoder[Date] with Decoder[Date] {

      override def apply(a: Date): Json =
        Encoder.encodeString.apply(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(a))

      override def apply(c: HCursor): Result[Date] =
        Decoder.decodeString.map(s => new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(s)).apply(c)
    }

  private implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  def authBasic(up: UsernamePassword): Future[Either[Int, String]] =
    Future {
      println(s"TOKEN: $up")
      if (up.username == "Aladdin") Right("Stefan")
      else Left(0)
    }


  def authOauth: String => Future[Either[Int, String]] = {
    s =>
      println(s"---------------\n$s")
      Future(Right("Aaa"))
    Future(Left(1))
  }

  val secureEndpointBase: PartialServerEndpoint[String, Unit, Int, Unit, Nothing, Future] = endpoint
//    .in(header[String]("X-AUTH-TOKEN"))
//    .in(auth.basic[UsernamePassword])
    .in(auth.oauth2.authorizationCode("http://www.onet.pl","http://token",ListMap.empty,None))
    .errorOut(plainBody[Int])
    .serverLogicForCurrent(authOauth)

  val secureEndpoint = secureEndpointBase.get
    .in("api" / "secure")
    .in(query[String]("x"))
    .out(stringBody)
    .serverLogic {
      case (user, header) => Future(Right(s"$user -> $header"))
    }


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

  val feedback: Endpoint[
    (Api.FeedbackSend, List[Cookie]),
    String,
    (Api.FeedbackResponse, CookieValueWithMeta),
    Nothing
  ] =
    endpoint.post
      .in("api" / "feedback")
      .in(jsonBody[Api.FeedbackSend].description("Feedback from user"))
      .in(cookies)
      .errorOut(stringBody)
      .out(jsonBody[Api.FeedbackResponse])
      .out(setCookie("session"))

  val validateEndpoint: Endpoint[String, Unit, Api.ValidationResult, Nothing] = endpoint.post
    .in("api" / "quizz" / "validate" / "mindmup")
    .in(stringBody("UTF-8"))
    .out(jsonBody[Api.ValidationResult])

  val trackingSessions: Endpoint[Unit, String, Api.TrackingSessions, Nothing] = endpoint.get
    .in("api" / "tracking" / "sessions")
    .errorOut(stringBody)
    .out(jsonBody[Api.TrackingSessions])

  val trackingSession
      : Endpoint[Api.TrackingSessionHistoryQuery, String, Api.TrackingSessionHistory, Nothing] =
    endpoint.get
      .in(
        "api" /
        "tracking" /
        "session" /
        path[String](name = "session").description("Session id") /
        "quizz" /
        path[String](name = "quizz id").description("Quizz id")
      )
      .mapInTo(Api.TrackingSessionHistoryQuery)
      .errorOut(stringBody)
      .out(jsonBody[Api.TrackingSessionHistory])

  val allEndpoints: Seq[Endpoint[_, _, _, _]] = Seq(
    routeEndpoint,
    routeEndpointStart,
    listQuizzes,
    addQuizz,
    deleteQuizz,
    feedback,
    validateEndpoint,
    trackingSessions,
    trackingSession
  )

}
