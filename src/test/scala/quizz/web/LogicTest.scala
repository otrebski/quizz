package quizz.web

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import quizz.data.ExamplesData

class LogicTest extends AnyFlatSpec with Matchers {

  val request: Api.QuizzQuery = Api.QuizzQuery("q1", "root;electricity;whereIsOutage")

  "Logic" should "calculate history" in {
    val actual: Either[String, Api.QuizzState] =
      Logic.calculateStateOnPath(request, ExamplesData.quizzes)

    actual match {
      case Right(r) =>
        r.currentStep.id shouldBe "whereIsOutage"
        r.path shouldBe "root;electricity;whereIsOutage"
        r.history.map(_.id) shouldBe List("root", "electricity")
      case Left(error) => fail(error)
    }

  }
}
