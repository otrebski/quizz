package tree.web

import java.time.Instant
import java.util.{ Date, UUID }

import cats.Applicative
import cats.effect.Sync
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import mindmup.Parser
import tree.data.MindmupStore
import tree.feedback.FeedbackSender
import tree.model.DecisionTree
import tree.tracking.Tracking
import tree.web.Api.{ AddQDecisionTreeResponse, DecisionTreeQuery, DecisionTrees, FeedbackResponse }
import sttp.model.{ Cookie, CookieValueWithMeta }

import scala.language.higherKinds

object RouteProviders extends LazyLogging {

  private def generateCookie(maybeCookie: Option[Cookie]): CookieValueWithMeta =
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

  implicit val idConvert: DecisionTreeQuery => DecisionTreeQuery = q => q
  implicit val feedbackConvert: Api.FeedbackSend => DecisionTreeQuery = fs =>
    DecisionTreeQuery(fs.treeId, fs.path)

  def track[In, Out, Error, F[_]: Sync](tracking: Tracking[F], f: In => F[Either[Error, Out]])(
      requestAndCookie: (In, List[Cookie])
  )(implicit convert: In => DecisionTreeQuery): F[Either[Error, (Out, CookieValueWithMeta)]] = {
    val (request, cookies) = requestAndCookie
    val cookie             = generateCookie(cookies.find(_.name == "session"))
    val treeQuery          = convert.apply(request)
    val result = for {
      _ <-
        tracking
          .step(treeQuery.id, treeQuery.path, Instant.now(), cookie.value, none[String])
      r <- f.apply(request)
      withCookie = r.map(x => (x, cookie))
    } yield withCookie
    result
  }

  def routeWithPathProvider[F[_]: Sync](
      store: MindmupStore[F]
  )(
      request: Api.DecisionTreeQuery
  ): F[Either[String, Api.DecisionTreeState]] = {
    import mindmup._
    for {
      latestVersion <- store.latestVersion(request.id)
      q <-
        store
          .load(request.id, latestVersion)
          .map(s => Parser.parseInput(request.id, s.content).flatMap(_.toDecisionTree))
      result = q.flatMap(trees => Logic.calculateState(request, Map(request.id -> trees)))
    } yield result
  }

  def treeListProvider[F[_]: Sync](
      treeStore: MindmupStore[F]
  ): List[Cookie] => F[Either[Unit, Api.DecisionTrees]] = { _ =>
    import mindmup._
    val r = for {
      names <- treeStore.listNames()
      v     <- names.toList.traverse(name => treeStore.latestVersion(name).map(name -> _))
      errorOrTreeList <- v.traverse {
        case (name, version) =>
          treeStore
            .load(name, version)
            .map(mindmup => Parser.parseInput(mindmup.name, mindmup.content))
            .map {
              case Left(error)  => Left(Api.DecisionTreeErrorInfo(name, error))
              case Right(value) => Right(Api.DecisionTreeInfo(name, value.title, version))
            }
      }
      (errors, trees) = errorOrTreeList.partitionMap(identity)
    } yield DecisionTrees(trees, errors)

    r.redeem(
      error => {
        RouteProviders.logger.error("Error on request", error)
        Left(())
      },
      v => Right(v)
    )
  }

  def addTreeProvider[F[_]: Sync](
      store: MindmupStore[F]
  )(request: Api.AddDecisionTree): F[Either[Unit, AddQDecisionTreeResponse]] = {
    import mindmup._
    val id: Either[String, V3IdString.Mindmap] =
      Parser.parseInput(request.id, request.mindmupSource)
    val newTreeOrError: Either[String, DecisionTree] =
      id.flatMap(_.toDecisionTree).map(_.copy(id = request.id))

    newTreeOrError match {
      case Left(error) => Sync[F].raiseError(new Exception(error))
      case Right(_) =>
        store
          .store(request.id, request.mindmupSource)
          .map(_ => AddQDecisionTreeResponse("added").asRight[Unit])
    }
  }

  def deleteTreeProvider[F[_]: Sync](
      store: MindmupStore[F]
  )(request: Api.DeleteDecisionTree): F[Either[Unit, Unit]] =
    store
      .delete(request.id, request.version)
      .map(_.asRight[Unit])

  def feedbackProvider[F[_]: Sync](
      store: MindmupStore[F],
      feedbackSenders: List[FeedbackSender[F]]
  )(
      feedback: Api.FeedbackSend
  ): F[Either[String, FeedbackResponse]] = {
    val request = Api.DecisionTreeQuery(feedback.treeId, feedback.path, feedback.version.some)
    val treeState: F[Either[String, Api.DecisionTreeState]] = store
      .load(request.id, request.version.getOrElse(0))
      .map(mindmup =>
        Parser
          .parseInput(request.id, mindmup.content)
          .flatMap(_.toDecisionTree)
          .flatMap(tree => Logic.calculateState(request, Map(request.id -> tree)))
      )
    treeState.flatMap {
      case Right(treeState) =>
        feedbackSenders
          .traverse(_.send(feedback, treeState))
          .map(_ => FeedbackResponse("OK").asRight)
      case Left(error) => Sync[F].raiseError(new Exception(s"Can't process feedback: $error"))
    }
  }

  def validateProvider[F[_]: Applicative]: String => F[Either[Unit, Api.ValidationResult]] = { s =>
    val result = mindmup.Parser.parseInput("validation_test", s) match {
      case Left(error) => Api.ValidationResult(valid = false, List(error))
      case Right(_)    => Api.ValidationResult(valid = true, List.empty[String])
    }
    Applicative[F].pure(Right(result))
  }

  def trackingSessionsProvider[F[_]: Sync](
      tracking: Tracking[F]
  ): Unit => F[Either[String, Api.TrackingSessions]] = { _ =>
    tracking
      .listSessions()
      .map(s =>
        s.map(ts =>
          Api
            .TrackingSession(
              treeId = ts.treeId,
              session = ts.session,
              date = ts.date,
              duration = ts.duration
            )
        )
      )
      .map(Api.TrackingSessions)
      .map(_.asRight[String])
  }

  def trackingSessionProvider[F[_]: Sync](
      tracking: Tracking[F]
  )(query: Api.TrackingSessionHistoryQuery): F[Either[String, Api.TrackingSessionHistory]] =
    tracking
      .session(query.session, query.treeId)
      .map { list =>
        val dates = list.map(_.date)
        val min   = dates.minOption.getOrElse(new Date(0))
        val max   = dates.maxOption.getOrElse(new Date(0))
        val details = Api.TrackingSession(
          session = query.session,
          date = min,
          treeId = query.treeId,
          duration = max.getTime - min.getTime
        )
        val steps =
          list.map(ts => Api.TrackingStep(ts.treeId, ts.path, ts.date, ts.session, ts.username))
        Api.TrackingSessionHistory(details, steps).asRight[String]
      }

}
