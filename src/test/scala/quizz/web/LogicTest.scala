package quizz.web
import org.scalatest._
import quizz.model.Question
import quizz.web.WebApp.Api
class LogicTest extends FlatSpec with Matchers {

  val request: Api.QuizzQuery = Api.QuizzQuery("q1","electricity")


  "Logic" should "calculate history" in {
    val actual: Either[String, Api.QuizzState] = Logic.calculateStateOnPath(request)

    actual match {
      case Right(r) =>
        r.currentStep.id shouldBe "electricity"
        r.path shouldBe "electricity"
        r.history.head.id shouldBe "root"
        r.history(1).id shouldBe "electricity"
      case Left(error) => fail(error)
    }



  }
}
