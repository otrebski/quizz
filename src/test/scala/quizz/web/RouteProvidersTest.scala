package quizz.web

import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.io.Source
import cats.syntax.either._

class RouteProvidersTest extends AsyncFlatSpec with Matchers {

  "validateProvider" should "valid correct mindmup" in {
    val future = RouteProviders.validateProvider.apply(Source.fromResource("mindmup/mindmup_with_notes.mup.json").mkString)
    future map { result => result shouldBe Api.ValidationResult(valid = true, List.empty[String]).asRight }
  }

  "validateProvider" should "find error in incorrect mindmup" in {
    val future = RouteProviders.validateProvider.apply(Source.fromResource("mindmup/invalid_mindmup.json").mkString)
    future map { result => result shouldBe Api.ValidationResult(valid = false, List("Attempt to decode value on failed cursor: DownField(id),DownField(1),DownField(ideas)")).asRight }
  }



}
