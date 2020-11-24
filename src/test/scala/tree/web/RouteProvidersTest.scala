package tree.web

import java.time.Instant
import java.util.Date

import cats.Id
import cats.effect.IO
import cats.implicits.none
import cats.syntax.either._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import tree.data.MemoryMindmupStore
import tree.feedback.LogFeedbackSender
import tree.tracking.MemoryTracking
import tree.web.Api._

import scala.io.Source

class RouteProvidersTest extends AnyFlatSpec with Matchers {

  private val validMindmup = Source.fromResource("mindmup/mindmup_with_notes.mup.json").mkString

  "validateProvider" should "valid correct mindmup" in {
    RouteProviders.validateProvider[Id].apply(validMindmup) shouldBe Api
      .ValidationResult(valid = true, List.empty[String])
      .asRight

  }

  "validateProvider" should "find error in incorrect mindmup" in {
    val result: Either[Unit, Api.ValidationResult] = RouteProviders.validateProvider.apply(
      Source.fromResource("mindmup/invalid_mindmup.json").mkString
    )
    result shouldBe Api
      .ValidationResult(
        valid = false,
        List(
          "Attempt to decode value on failed cursor: DownField(id),DownField(1),DownField(ideas)"
        )
      )
      .asRight

  }

  "treeListProvider" should "list empty trees" in {
    val mindmups: IO[Either[Unit, DecisionTrees]] = for {
      store    <- MemoryMindmupStore[IO]
      mindmups <- RouteProviders.treeListProvider(store).apply(List.empty)
    } yield mindmups
    mindmups.unsafeRunSync() match {
      case Right(q)    => q shouldBe DecisionTrees()
      case Left(error) => fail(error.toString)
    }
  }

  "treeListProvider" should "list all trees" in {
    val mindmups: IO[Either[Unit, DecisionTrees]] = for {
      store    <- MemoryMindmupStore[IO]
      _        <- store.store("a", validMindmup)
      _        <- store.store("b", validMindmup)
      _        <- store.store("c", "invalid")
      mindmups <- RouteProviders.treeListProvider(store).apply(List.empty)
    } yield mindmups
    mindmups.unsafeRunSync() match {
      case Right(decisionTrees) =>
        decisionTrees.trees should contain theSameElementsAs List(
          DecisionTreeInfo("a", "Starting point", 1),
          DecisionTreeInfo("b", "Starting point", 1)
        )
        decisionTrees.treesWithErrors.size shouldBe 1
      case Left(error) => fail(error.toString)
    }
  }

  "addTreeProvider" should "add tree" in {
    val io = for {
      store       <- MemoryMindmupStore[IO]()
      addResponse <- RouteProviders.addTreeProvider(store)(Api.AddDecisionTree("a", validMindmup))
      mindmups    <- store.listNames()
    } yield (addResponse, mindmups)
    io.unsafeRunSync() shouldBe (AddQDecisionTreeResponse("added").asRight, Set("a"))

  }

  "addTreeProvider" should "return error on invalid tree" in {
    val io = for {
      store <- MemoryMindmupStore[IO]
      addResponse <-
        RouteProviders
          .addTreeProvider(store)(Api.AddDecisionTree("a", "[]"))
          .redeem(_ => ().asLeft[Api.DecisionTrees], identity)
      mindmups <- store.listNames()
    } yield (addResponse, mindmups)
    io.unsafeRunSync() shouldBe (().asLeft, Set.empty[String])

  }

  "routeWithPathProvider" should " return correct tree state for starting point" in {
    val io = for {
      store <- MemoryMindmupStore[IO]
      _     <- store.store("a", validMindmup)
      result <- RouteProviders.routeWithPathProvider(store)(
        Api.DecisionTreeQuery("a", "")
      )
    } yield result
    io.map(_.map(_.currentStep.question)).unsafeRunSync() shouldBe "Starting point".asRight
  }

  "routeWithPathProvider" should " return correct tree state" in {
    val io = for {
      store <- MemoryMindmupStore[IO]
      _     <- store.store("a", validMindmup)
      result <- RouteProviders.routeWithPathProvider(store)(
        Api.DecisionTreeQuery("a", "root;3.eeff.d297c2367-0c3d.6aa7f21a")
      )
    } yield result
    io.map(_.map(_.currentStep.question)).unsafeRunSync() shouldBe "Node2".asRight
  }

  "routeWithPathProvider" should " return error for wrong path" in {
    val io = for {
      store <- MemoryMindmupStore[IO]
      _     <- store.store("a", validMindmup)
      result <-
        RouteProviders.routeWithPathProvider(store)(Api.DecisionTreeQuery("a", "root;wrong path"))
    } yield result
    io.map(_.map(_.currentStep.question)).unsafeRunSync() shouldBe "Wrong selection".asLeft
  }

  "feedbackProvider" should "send feedback" in {
    val feedbackSender = new LogFeedbackSender[IO]
    val feedbackSend   = Api.FeedbackSend("a", "root;3.eeff.d297c2367-0c3d.6aa7f21a", 0, "comment")
    val io = for {
      store  <- MemoryMindmupStore[IO]
      _      <- store.store("a", validMindmup)
      result <- RouteProviders.feedbackProvider(store, List(feedbackSender))(feedbackSend)
    } yield result
    io.unsafeRunSync() shouldBe FeedbackResponse("OK").asRight
  }

  "tracking sessions" should "list all sessions" in {
    val tracking = for {
      tracking <- MemoryTracking[IO]()
      _        <- tracking.step("q1", "a", Instant.ofEpochSecond(0), "s1", none[String])
      _        <- tracking.step("q1", "a;2", Instant.ofEpochSecond(100), "s1", none[String])
      _        <- tracking.step("q2", "a", Instant.ofEpochSecond(0), "s2", none[String])
      _        <- tracking.step("q2", "a;2", Instant.ofEpochSecond(0), "s2", none[String])
      _        <- tracking.step("q2", "a;2;3", Instant.ofEpochSecond(200), "s2", none[String])
    } yield tracking

    val x = for {
      t <- tracking
      r <- RouteProviders.trackingSessionsProvider(t).apply(())
    } yield r

    x.unsafeRunSync().map(_.sessions.toSet) shouldBe Set(
      Api.TrackingSession("s1", new Date(0), "q1", 100 * 1000),
      Api.TrackingSession("s2", new Date(0), "q2", 200 * 1000)
    ).asRight

  }

  "tracking session" should "list single session" in {
    val tracking = for {
      tracking <- MemoryTracking[IO]()
      _        <- tracking.step("q1", "a", Instant.ofEpochSecond(0), "s1", none[String])
      _        <- tracking.step("q1", "a;2", Instant.ofEpochSecond(100), "s1", none[String])
      _        <- tracking.step("q2", "a", Instant.ofEpochSecond(0), "s2", none[String])
      _        <- tracking.step("q2", "a;2", Instant.ofEpochSecond(0), "s2", none[String])
      _        <- tracking.step("q2", "a;2;3", Instant.ofEpochSecond(200), "s2", none[String])
    } yield tracking

    (for {
      t <- tracking
      r <- RouteProviders.trackingSessionProvider(t)(Api.TrackingSessionHistoryQuery("s1", "q1"))
    } yield r)
      .unsafeRunSync() shouldBe TrackingSessionHistory(
      details = Api.TrackingSession("s1", new Date(0), "q1", 100 * 1000),
      steps = List(
        Api.TrackingStep("q1", "a", new Date(0), "s1", none[String]),
        Api.TrackingStep("q1", "a;2", new Date(100 * 1000), "s1", none[String])
      )
    ).asRight
  }

}
