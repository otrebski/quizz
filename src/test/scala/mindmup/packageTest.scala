package mindmup

import cats.syntax.option.none
import mindmup.V3IdString.{ Idea, Mindmap }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import cats.syntax.option._
import quizz.model
import quizz.model.{ Question, SuccessStep }

import scala.io.Source

class packageTest extends AnyFlatSpec with Matchers {

  private val quizz: model.Quizz = Parser
    .parseInput("test", Source.fromResource("mindmup/simple_tree.json").mkString)
    .flatMap(_.toQuizz)
    .getOrElse(throw new Exception("Mindmup not parsed"))

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
              ParentConnector("answer 1".some).some,
              Note("note").some
            ).some
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
  "package" should "convert mindmup to quizz" ignore {

    val quizz = mindmup.toQuizz.getOrElse(throw new Exception("Mindmup not parsed"))
    val question: Question =
      quizz.firstStep.asInstanceOf[Question].answers("root node").asInstanceOf[Question]
    question.answers.foreach {
      case (k, v) => println(s"$k => $v")
    }
    question.answers("answer 1").asInstanceOf[SuccessStep].text shouldBe "child 1\nnote"
    question.answers("answer 2").asInstanceOf[SuccessStep].text shouldBe "child 2"
  }

  "converted mindmup" should "have correct title" in {
    quizz.name shouldBe "Root node"
  }

  "converted mindmup" should "have correct root node" in {
    quizz.firstStep.text shouldBe "Root node"
  }

  "converted mindmup" should "have correct first steps" in {
    quizz.firstStep.asInstanceOf[Question].answers("Left").text shouldBe "Left node"
    quizz.firstStep.asInstanceOf[Question].answers("Right").text shouldBe "Right node"
    quizz.firstStep
      .asInstanceOf[Question]
      .answers("Right")
      .asInstanceOf[Question]
      .answers("Right")
      .asInstanceOf[SuccessStep]
      .text shouldBe "Right Right Node"
  }

}
