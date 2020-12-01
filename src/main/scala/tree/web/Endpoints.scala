package tree.web

import java.text.SimpleDateFormat
import java.util.Date

import io.circe.Decoder.Result
import io.circe.generic.auto._
import tree.web.Api.DeleteDecisionTree
import sttp.model.{ Cookie, CookieValueWithMeta }
import sttp.tapir.json.circe._
import sttp.tapir.{ path, setCookie, _ }

object Endpoints {
  import io.circe.{ Json, _ }
  implicit val TimestampFormat: Encoder[Date] with Decoder[Date] =
    new Encoder[Date] with Decoder[Date] {

      override def apply(a: Date): Json =
        Encoder.encodeString.apply(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(a))

      override def apply(c: HCursor): Result[Date] =
        Decoder.decodeString.map(s => new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(s)).apply(c)
    }

  val routeEndpoint: Endpoint[
    (Api.DecisionTreeQuery, List[Cookie]),
    String,
    (Api.DecisionTreeState, CookieValueWithMeta),
    Nothing
  ] = endpoint.get
    .in(
      ("api" / "tree" / path[String]("id") / "path" / path[String]("treePath"))
        .mapTo((id, path) => Api.DecisionTreeQuery(id, path, version = None))
    )
    .in(cookies)
    .errorOut(stringBody)
    .out(jsonBody[Api.DecisionTreeState])
    .out(setCookie("session"))

  val routeEndpointStart: Endpoint[
    (Api.DecisionTreeQuery, List[Cookie]),
    String,
    (Api.DecisionTreeState, CookieValueWithMeta),
    Nothing
  ] = endpoint.get
    .in(
      ("api" / "tree" / path[String]("id") / "path").mapTo(id =>
        Api.DecisionTreeQuery(id, "", None)
      )
    )
    .in(cookies)
    .errorOut(stringBody)
    .out(jsonBody[Api.DecisionTreeState])
    .out(setCookie("session"))

  val listTrees: Endpoint[List[Cookie], Unit, Api.DecisionTrees, Nothing] = endpoint.get
    .in("api" / "tree")
    .in(cookies)
    .out(jsonBody[Api.DecisionTrees])

  val addTree: Endpoint[Api.AddDecisionTree, Unit, Api.AddQDecisionTreeResponse, Nothing] =
    endpoint.put
      .in("api" / "tree" / path[String](name = "id").description("Id of tree to add/replace"))
      .in(stringBody("UTF-8"))
      .mapIn(idAndContent => Api.AddDecisionTree(idAndContent._1, idAndContent._2))(a =>
        (a.id, a.mindmupSource)
      )
      .out(jsonBody[Api.AddQDecisionTreeResponse])

  val deleteTree: Endpoint[DeleteDecisionTree, Unit, Unit, Nothing] = endpoint.delete
    .in(
      "api" / "tree" / path[String](name = "id")
        .description("Id of tree to delete") / "version" / path[Int](name = "version")
    )
    .mapIn(idAndVersion => DeleteDecisionTree(idAndVersion._1, idAndVersion._2))(d =>
      (d.id, d.version)
    )
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
    .in("api" / "tree" / "validate" / "mindmup")
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
        "tree" /
        path[String](name = "tree id").description("Tree id")
      )
      .mapInTo(Api.TrackingSessionHistoryQuery)
      .errorOut(stringBody)
      .out(jsonBody[Api.TrackingSessionHistory])

  val trackingHistoryStep
      : Endpoint[Api.TrackingHistoryStepQuery, String, Api.TrackingHistoryStep, Nothing] =
    endpoint.get
      .in(
        "api" / "tracking" / "step" /
        "tree" / path[String](name = "tree id") /
        "version" / path[Int]("tree version").description("tree version") /
        "path" / path[String](name = "path").description("Path in tree")
      )
      .mapInTo(Api.TrackingHistoryStepQuery)
      .errorOut(stringBody)
      .out(jsonBody[Api.TrackingHistoryStep])

  val allEndpoints: Seq[Endpoint[_, _, _, _]] = Seq(
    routeEndpoint,
    routeEndpointStart,
    listTrees,
    addTree,
    deleteTree,
    feedback,
    validateEndpoint,
    trackingSessions,
    trackingSession,
    trackingHistoryStep
  )

}
