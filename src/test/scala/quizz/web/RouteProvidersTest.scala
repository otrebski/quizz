package quizz.web

import cats.effect.IO
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.io.Source
import cats.syntax.either._
import quizz.data.MemoryMindmupStore
import quizz.web.Api.{QuizzErrorInfoInfo, QuizzInfo, Quizzes}

import scala.concurrent.Future

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


}
