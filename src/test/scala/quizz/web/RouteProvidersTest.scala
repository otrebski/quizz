package quizz.web

import java.util.NoSuchElementException

import cats.effect.IO
import cats.syntax.either._
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import quizz.data.MemoryMindmupStore
import quizz.web.Api.{AddQuizzResponse, QuizzInfo, Quizzes}

import scala.io.Source


class RouteProvidersTest extends AsyncFlatSpec with Matchers {

  private val validMindmup = Source.fromResource("mindmup/mindmup_with_notes.mup.json").mkString

  "validateProvider" should "valid correct mindmup" in {
    val future = RouteProviders.validateProvider.apply(validMindmup)
    future map { result => result shouldBe Api.ValidationResult(valid = true, List.empty[String]).asRight }
  }

  "validateProvider" should "find error in incorrect mindmup" in {
    val future = RouteProviders.validateProvider.apply(Source.fromResource("mindmup/invalid_mindmup.json").mkString)
    future map { result => result shouldBe Api.ValidationResult(valid = false, List("Attempt to decode value on failed cursor: DownField(id),DownField(1),DownField(ideas)")).asRight }
  }

  "quizListProvider" should "list empty quizzes" in {
    val mindmups = for {
      store <- MemoryMindmupStore[IO].unsafeToFuture()
      mindmups <- RouteProviders.quizListProvider(store).apply()
    } yield mindmups
    mindmups map {
      case Right(q) => q shouldBe Quizzes()
      case Left(error) => fail(error.toString)
    }
  }

  "quizListProvider" should "list all quizzes" in {
    val mindmups = for {
      store <- MemoryMindmupStore[IO].unsafeToFuture()
      _ <- store.store("a", validMindmup).unsafeToFuture()
      _ <- store.store("b", validMindmup).unsafeToFuture()
      mindmups <- RouteProviders.quizListProvider(store).apply()
    } yield mindmups
    mindmups map {
      case Right(q) => q shouldBe Quizzes(List(QuizzInfo("a", "Starting point"), QuizzInfo("b", "Starting point")))
      case Left(error) => fail(error.toString)
    }
  }

  "addQuizzProvider" should "add quizz" in {
    val future = for {
      store <- MemoryMindmupStore[IO].unsafeToFuture()
      addResponse <- RouteProviders.addQuizzProvider(store)(Api.AddQuizz("a", validMindmup))
      mindmups <- store.listNames().unsafeToFuture()
    } yield (addResponse, mindmups)
    future map {
      _ shouldBe(AddQuizzResponse("added").asRight, Set("a"))
    }
  }

  "addQuizzProvider" should "return error on invalid quizz" in {
    val future = for {
      store <- MemoryMindmupStore[IO].unsafeToFuture()
      addResponse <- RouteProviders.addQuizzProvider(store)(Api.AddQuizz("a", "[]")).recover(_ => ().asLeft[Api.Quizzes])
      mindmups <- store.listNames().unsafeToFuture()
    } yield (addResponse, mindmups)
    future map {
      _ shouldBe(().asLeft, Set.empty[String])
    }
  }

  "routeWithoutPathProvider" should "return rood node" in {
    val future = for {
      store <- MemoryMindmupStore[IO].unsafeToFuture()
      _ <- store.store("a", validMindmup).unsafeToFuture()
      result <- RouteProviders.routeWithoutPathProvider(store)(Api.QuizzId("a"))
    } yield (result)
    future.map (_.map(_.currentStep.question)) .map {
      _ shouldBe "Starting point".asRight
    }
  }

  "routeWithoutPathProvider" should "return error if path not find" in {
    val future = for {
      store <- MemoryMindmupStore[IO].unsafeToFuture()
      result <- RouteProviders.routeWithoutPathProvider(store)(Api.QuizzId("a")).recover(e => e)
    } yield (result)
    future
      .map(_.getClass)
      .map  {
      _ shouldBe classOf[NoSuchElementException]
    }
  }

  "routeWithPathProvider" should " return correct quizz state" in {
    val future = for {
      store <- MemoryMindmupStore[IO].unsafeToFuture()
      _ <- store.store("a", validMindmup).unsafeToFuture()
      result <- RouteProviders.routeWithPathProvider(store)(Api.QuizzQuery("a","root;1;3.eeff.d297c2367-0c3d.6aa7f21a"))
    } yield result
    future
      .map (_.map(_.currentStep.question))
      .map {
      _ shouldBe "Node2".asRight
    }
  }

  "routeWithPathProvider" should " return error for wrong path" in {
    val future = for {
      store <- MemoryMindmupStore[IO].unsafeToFuture()
      _ <- store.store("a", validMindmup).unsafeToFuture()
      result <- RouteProviders.routeWithPathProvider(store)(Api.QuizzQuery("a","root;wrong path"))
    } yield result
    future.map (_.map(_.currentStep.question)) .map {
      _ shouldBe "Wrong selection".asLeft
    }
  }

}
