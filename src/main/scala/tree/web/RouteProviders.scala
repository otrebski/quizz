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

  def track[In, Out, Error, F[_]: Sync](
      tracking: Tracking[F],
      treeStore: MindmupStore[F],
      f: In => F[Either[Error, Out]]
  )(
      requestAndCookie: (In, List[Cookie])
  )(implicit convert: In => DecisionTreeQuery): F[Either[Error, (Out, CookieValueWithMeta)]] = {
    val (request, cookies) = requestAndCookie
    val cookie             = generateCookie(cookies.find(_.name == "session"))
    val treeQuery          = convert.apply(request)
    val result = for {
      version <- treeStore.latestVersion(treeQuery.id)
      _ <-
        tracking
          .step(treeQuery.id, version, treeQuery.path, Instant.now(), cookie.value, none[String])
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
    val request = Api.DecisionTreeQuery(feedback.treeId, feedback.path)

    def treeState(version: Int): F[Either[String, FeedbackResponse]] =
      store
        .load(request.id, version)
        .map(mindmup =>
          Parser
            .parseInput(request.id, mindmup.content)
            .flatMap(_.toDecisionTree)
            .flatMap(tree => Logic.calculateState(request, Map(request.id -> tree)))
        )
        .flatMap {
          case Right(treeState) =>
            feedbackSenders
              .traverse(_.send(feedback, version, treeState))
              .map(_ => FeedbackResponse("OK").asRight[String])
          case Left(error) => Sync[F].raiseError(new Exception(s"Can't process feedback: $error"))

        }

    for {
      version <- store.latestVersion(request.id)
      state   <- treeState(version)
    } yield state
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
              version = ts.version,
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
      tracking: Tracking[F],
      store: MindmupStore[F]
  )(query: Api.TrackingSessionHistoryQuery): F[Either[String, Api.TrackingSessionHistory]] = {

    def tree(name: String, version: Int): F[Either[String, DecisionTree]] =
      store
        .load(name, version)
        .map(mindmupString =>
          for {
            mindmup <- Parser.parseInput(query.treeId, mindmupString.content)
            tree    <- mindmup.toDecisionTree
          } yield tree
        )
    case class StateAt(date: Date, state: Api.DecisionTreeState)

    val filterDuplicates = (list: List[StateAt]) =>
      list match {
        case StateAt(_, prev) :: StateAt(_, next) :: Nil => prev.path != next.path
        case _                                           => true
      }

    val mapStates = (list: List[StateAt]) => {
      list match {
        case StateAt(
              prevDate,
              Api.DecisionTreeState(prevPath, Api.Step(_, question, _, _), _)
            ) :: StateAt(
              next,
              state
            ) :: Nil =>
          val duration     = next.getTime - prevDate.getTime
          val prevDepth    = prevPath.count(_ == ';')
          val currentDepth = state.path.count(_ == ';')
          val answers =
            if (prevDepth < currentDepth || (prevDepth == currentDepth && prevPath != state.path))
              state.history.lastOption.map(_.answers).getOrElse(List.empty)
            else
              List.empty
          Api.TrackingHistoryStep(
            date = next,
            question = question,
            answers = answers,
            duration = duration
          )
        case StateAt(date, state) :: Nil =>
          Api.TrackingHistoryStep(state.currentStep.question, date, duration = 0)
        case _ => Api.TrackingHistoryStep("Just to make compiler happy", new Date(), 0)
      }
    }

    val trackingSessionHistory = for {
      steps <- tracking.session(query.session, query.treeId) //List[TrackingSession]
      dates   = steps.map(_.date)
      version = steps.map(_.version).max
      min     = dates.minOption.getOrElse(new Date(0))
      max     = dates.maxOption.getOrElse(new Date(0))
      details = Api.TrackingSession(
        session = query.session,
        date = min,
        version = version,
        treeId = query.treeId,
        duration = max.getTime - min.getTime
      )
      treeOrError <- tree(query.treeId, version)
      statesOrError = for {
        tree <- treeOrError
        states <-
          steps
            .map(step =>
              Logic
                .calculateState(
                  DecisionTreeQuery(query.treeId, step.path, version.some),
                  Map(query.treeId -> tree)
                )
                .map(s => StateAt(step.date, s))
            )
            .sequence
      } yield states

      historySteps = statesOrError.map(_.sliding(2).filter(filterDuplicates).toList.map(mapStates))
      r = for {
        hs <- historySteps
      } yield Api.TrackingSessionHistory(details, hs)
    } yield r
    //TODO add text of last step
    trackingSessionHistory
  }

  def trackingHistoryStepProvider[F[_]: Sync](store: MindmupStore[F])(
      query: Api.TrackingHistoryStepQuery
  ): F[Either[String, Api.TrackingHistoryStep]] =
    store.load(query.treeId, query.version).map { mindmupString =>
      for {
        mindmup <- Parser.parseInput(query.treeId, mindmupString.content)
        tree    <- mindmup.toDecisionTree
        state <- Logic.calculateState(
          DecisionTreeQuery(query.treeId, query.path, query.version.some),
          Map(query.treeId -> tree)
        )
        step = state.history.last
      } yield Api.TrackingHistoryStep(
        question = step.question,
        date = new Date(0),
        duration = 0,
        answers = step.answers,
        success = step.success
      )
    }

}
