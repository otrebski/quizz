package quizz.web

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import quizz.data.ExamplesData
import quizz.web.WebApp.Api

class LogicTest extends AnyFlatSpec with Matchers {

  val request: Api.QuizzQuery = Api.QuizzQuery("q1", "electricity;whereIsOutage")

  "Logic" should "calculate history" in {
    val actual: Either[String, Api.QuizzState] =
      Logic.calculateStateOnPath(request, ExamplesData.quizzes)

    actual match {
      case Right(r) =>
        r.currentStep.id shouldBe "whereIsOutage"
        r.path shouldBe "electricity;whereIsOutage"
        r.history.map(_.id) shouldBe List("root", "electricity")
      case Left(error) => fail(error)
    }

  }
}
