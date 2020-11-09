package quizz.web

import java.util.NoSuchElementException

import cats.effect.IO
import cats.syntax.either._
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import quizz.data.MemoryMindmupStore
import quizz.feedback.LogFeedbackSender
import quizz.tracking.TrackingConsole
import quizz.web.Api.{ AddQuizzResponse, FeedbackResponse, QuizzInfo, Quizzes }
import sttp.model.CookieValueWithMeta

import scala.concurrent.Future
import scala.io.Source

class RouteProvidersTest extends AsyncFlatSpec with Matchers {

  private val validMindmup = Source.fromResource("mindmup/mindmup_with_notes.mup.json").mkString

  "validateProvider" should "valid correct mindmup" in {
    val future = RouteProviders.validateProvider.apply(validMindmup)
    future map { result =>
      result shouldBe Api.ValidationResult(valid = true, List.empty[String]).asRight
    }
  }

  "validateProvider" should "find error in incorrect mindmup" in {
    val future = RouteProviders.validateProvider.apply(
      Source.fromResource("mindmup/invalid_mindmup.json").mkString
    )
    future map { result =>
      result shouldBe Api
        .ValidationResult(
          valid = false,
          List(
            "Attempt to decode value on failed cursor: DownField(id),DownField(1),DownField(ideas)"
          )
        )
        .asRight
    }
  }

  "quizListProvider" should "list empty quizzes" in {
    val mindmups = for {
      store <- MemoryMindmupStore[IO].unsafeToFuture()

      mindmups <- RouteProviders.quizListProvider(store).apply(List.empty)
    } yield mindmups
    mindmups map {
      case Right(q)    => q shouldBe Quizzes()
      case Left(error) => fail(error.toString)
    }
  }

  "quizListProvider" should "list all quizzes" in {
    val mindmups = for {
      store    <- MemoryMindmupStore[IO].unsafeToFuture()
      _        <- store.store("a", validMindmup).unsafeToFuture()
      _        <- store.store("b", validMindmup).unsafeToFuture()
      mindmups <- RouteProviders.quizListProvider(store).apply(List.empty)
    } yield mindmups
    mindmups map {
      case Right(q) =>
        q shouldBe Quizzes(List(QuizzInfo("a", "Starting point"), QuizzInfo("b", "Starting point")))
      case Left(error) => fail(error.toString)
    }
  }

  "addQuizzProvider" should "add quizz" in {
    val future = for {
      store       <- MemoryMindmupStore[IO].unsafeToFuture()
      addResponse <- RouteProviders.addQuizzProvider(store)(Api.AddQuizz("a", validMindmup))
      mindmups    <- store.listNames().unsafeToFuture()
    } yield (addResponse, mindmups)
    future map {
      _ shouldBe (AddQuizzResponse("added").asRight, Set("a"))
    }
  }

  "addQuizzProvider" should "return error on invalid quizz" in {
    val future = for {
      store <- MemoryMindmupStore[IO].unsafeToFuture()
      addResponse <-
        RouteProviders
          .addQuizzProvider(store)(Api.AddQuizz("a", "[]"))
          .recover(_ => ().asLeft[Api.Quizzes])
      mindmups <- store.listNames().unsafeToFuture()
    } yield (addResponse, mindmups)
    future map {
      _ shouldBe (().asLeft, Set.empty[String])
    }
  }

  "routeWithPathProvider" should " return correct quizz state for starting point" in {
    val future = for {
      store <- MemoryMindmupStore[IO].unsafeToFuture()
      _     <- store.store("a", validMindmup).unsafeToFuture()
      result <- RouteProviders.routeWithPathProvider(store)(
        Api.QuizzQuery("a", "")
      )
    } yield result
    future
      .map(_.map(_.currentStep.question))
      .map {
        _ shouldBe "Starting point".asRight
      }
  }
  "routeWithPathProvider" should " return correct quizz state" in {
    val future = for {
      store <- MemoryMindmupStore[IO].unsafeToFuture()
      _     <- store.store("a", validMindmup).unsafeToFuture()
      result <- RouteProviders.routeWithPathProvider(store)(
        Api.QuizzQuery("a", "root;3.eeff.d297c2367-0c3d.6aa7f21a")
      )
    } yield result
    future
      .map(_.map(_.currentStep.question))
      .map {
        _ shouldBe "Node2".asRight
      }
  }

  "routeWithPathProvider" should " return error for wrong path" in {
    val future = for {
      store  <- MemoryMindmupStore[IO].unsafeToFuture()
      _      <- store.store("a", validMindmup).unsafeToFuture()
      result <- RouteProviders.routeWithPathProvider(store)(Api.QuizzQuery("a", "root;wrong path"))
    } yield result
    future.map(_.map(_.currentStep.question)).map {
      _ shouldBe "Wrong selection".asLeft
    }
  }

  "feedbackProvider" should "send feedback" in {
    val feedbackSender = new LogFeedbackSender[IO]
    val feedbackSend   = Api.FeedbackSend("a", "root;3.eeff.d297c2367-0c3d.6aa7f21a", 0, "comment")
    val future = for {
      store <- MemoryMindmupStore[IO].unsafeToFuture()
      _     <- store.store("a", validMindmup).unsafeToFuture()
      result <-
        RouteProviders.feedbackProvider(store, List(feedbackSender))(feedbackSend)
    } yield result
    future.map {
      _ shouldBe FeedbackResponse("OK").asRight
    }
  }

}
