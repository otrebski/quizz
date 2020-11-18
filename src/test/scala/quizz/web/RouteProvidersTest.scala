package quizz.web

import java.time.Instant
import java.util.Date

import cats.Id
import cats.effect.IO
import cats.implicits.none
import cats.syntax.either._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import quizz.data.MemoryMindmupStore
import quizz.feedback.LogFeedbackSender
import quizz.tracking.MemoryTracking
import quizz.web.Api.{
  AddQuizzResponse,
  FeedbackResponse,
  QuizzInfo,
  Quizzes,
  TrackingSessionHistory
}

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

  "quizListProvider" should "list empty quizzes" in {
    val mindmups: IO[Either[Unit, Quizzes]] = for {
      store    <- MemoryMindmupStore[IO]
      mindmups <- RouteProviders.quizListProvider(store).apply(List.empty)
    } yield mindmups
    mindmups.unsafeRunSync() match {
      case Right(q)    => q shouldBe Quizzes()
      case Left(error) => fail(error.toString)
    }
  }

  "quizListProvider" should "list all quizzes" in {
    val mindmups = for {
      store    <- MemoryMindmupStore[IO]
      _        <- store.store("a", validMindmup)
      _        <- store.store("b", validMindmup)
      mindmups <- RouteProviders.quizListProvider(store).apply(List.empty)
    } yield mindmups
    mindmups.unsafeRunSync() match {
      case Right(q) =>
        q shouldBe Quizzes(List(QuizzInfo("a", "Starting point"), QuizzInfo("b", "Starting point")))
      case Left(error) => fail(error.toString)
    }
  }

  "addQuizzProvider" should "add quizz" in {
    val future = for {
      store       <- MemoryMindmupStore[IO]()
      addResponse <- RouteProviders.addQuizzProvider(store)(Api.AddQuizz("a", validMindmup))
      mindmups    <- store.listNames()
    } yield (addResponse, mindmups)
    future.unsafeRunSync() shouldBe (AddQuizzResponse("added").asRight, Set("a"))

  }

  "addQuizzProvider" should "return error on invalid quizz" in {
    val io = for {
      store <- MemoryMindmupStore[IO]
      addResponse <-
        RouteProviders
          .addQuizzProvider(store)(Api.AddQuizz("a", "[]"))
          .redeem(_ => ().asLeft[Api.Quizzes], identity)
      mindmups <- store.listNames()
    } yield (addResponse, mindmups)
    io.unsafeRunSync() shouldBe (().asLeft, Set.empty[String])

  }

  "routeWithPathProvider" should " return correct quizz state for starting point" in {
    val io = for {
      store <- MemoryMindmupStore[IO]
      _     <- store.store("a", validMindmup)
      result <- RouteProviders.routeWithPathProvider(store)(
        Api.QuizzQuery("a", "")
      )
    } yield result
    io.map(_.map(_.currentStep.question))
      .map {
        _ shouldBe "Starting point".asRight
      }
      .unsafeToFuture()
  }
  "routeWithPathProvider" should " return correct quizz state" in {
    val io = for {
      store <- MemoryMindmupStore[IO]
      _     <- store.store("a", validMindmup)
      result <- RouteProviders.routeWithPathProvider(store)(
        Api.QuizzQuery("a", "root;3.eeff.d297c2367-0c3d.6aa7f21a")
      )
    } yield result
    io.map(_.map(_.currentStep.question))
      .map {
        _ shouldBe "Node2".asRight
      }
      .unsafeToFuture()
  }

  "routeWithPathProvider" should " return error for wrong path" in {
    val io = for {
      store  <- MemoryMindmupStore[IO]
      _      <- store.store("a", validMindmup)
      result <- RouteProviders.routeWithPathProvider(store)(Api.QuizzQuery("a", "root;wrong path"))
    } yield result
    io.map(_.map(_.currentStep.question))
      .map {
        _ shouldBe "Wrong selection".asLeft
      }
      .unsafeToFuture()
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
      .map {
        _ shouldBe TrackingSessionHistory(
          details = Api.TrackingSession("s1", new Date(0), "q1", 100 * 1000),
          steps = List(
            Api.TrackingStep("q1", "a", new Date(0), "s1", none[String]),
            Api.TrackingStep("q1", "a;2", new Date(100 * 1000), "s1", none[String])
          )
        ).asRight
      }
      .unsafeToFuture()
  }

}
