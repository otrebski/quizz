package mindmup

import cats.syntax.option.none
import mindmup.V3IdString.{Idea, Mindmap}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import cats.syntax.option._
import quizz.model.{Question, SuccessStep}

class packageTest extends AnyFlatSpec with Matchers {

  val mindmup: Mindmap = Mindmap(
    id = "root",
    title = "root node",
    formatVersion = 3,
    ideas = Map(
      "1" -> Idea(
        title = "root node",
        id = "1",
        attr = Some(Attr(none)),
        ideas = Map(
          "1" -> Idea(
            title = "child 1",
            id = "3",
            attr = Attr(
              ParentConnector("answer 1".some).some, Note("note").some
            ).some,
          ),
          "21" -> Idea(
            title = "child 2",
            id = "6",
            attr = Attr(ParentConnector("answer 2".some).some).some
          )
        ).some
      )
    )
  )

  "package" should "convert mindmup to quizz" in {
    val quizz = mindmup.toQuizz

    val question: Question = quizz.firstStep.asInstanceOf[Question].answers("root node").asInstanceOf[Question]
    question.answers.foreach {
      case (k,v) => println(s"$k => $v")
    }
    question.answers("answer 1").asInstanceOf[SuccessStep].text shouldBe "child 1\nnote"
    question.answers("answer 2").asInstanceOf[SuccessStep].text shouldBe "child 2"
  }
}
