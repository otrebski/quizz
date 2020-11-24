package mindmup

import cats.implicits.catsSyntaxEitherId
import cats.syntax.option.{ none, _ }
import mindmup.V3IdString.{ Idea, Mindmap }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import tree.model
import tree.model.{ Question, SuccessStep }

import scala.io.Source

class packageTest extends AnyFlatSpec with Matchers {

  private val tree: Either[String, model.DecisionTree] = Parser
    .parseInput("test", Source.fromResource("mindmup/simple_tree.json").mkString)
    .flatMap(_.toDecisionTree)

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
  "package" should "convert mindmup to tree" ignore {

    val tree = mindmup.toDecisionTree.getOrElse(throw new Exception("Mindmup not parsed"))
    val question: Question =
      tree.firstStep.asInstanceOf[Question].answers("root node").asInstanceOf[Question]
    question.answers.foreach {
      case (k, v) => println(s"$k => $v")
    }
    question.answers("answer 1").asInstanceOf[SuccessStep].text shouldBe "child 1\nnote"
    question.answers("answer 2").asInstanceOf[SuccessStep].text shouldBe "child 2"
  }

  "converted mindmup" should "have correct title" in {
    tree.map(_.name) shouldBe "Root node".asRight
  }

  "converted mindmup" should "have correct root node" in {
    tree.map(_.firstStep.text) shouldBe "Root node".asRight
  }

  "converted mindmup" should "have correct first steps" in {
    tree.map(_.firstStep.asInstanceOf[Question].answers("Left").text) shouldBe "Left node".asRight
    tree.map(
      _.firstStep.asInstanceOf[Question].answers("Right").text
    ) shouldBe "Right node".asRight
    tree.map(
      _.firstStep
        .asInstanceOf[Question]
        .answers("Right")
        .asInstanceOf[Question]
        .answers("Right")
        .asInstanceOf[SuccessStep]
        .text
    ) shouldBe "Right Right Node".asRight
  }

  "package" should "detect missing answer" in {
    val error: Either[String, model.DecisionTree] = Parser
      .parseInput(
        "test",
        Source.fromResource("mindmup/invalid_mindmup_missing_answer.json").mkString
      )
      .flatMap(_.toDecisionTree)

    error shouldBe Left(
      """Node "Child 1" has answer without text. Node "Child 2" has answer without text"""
    )
  }

  "package" should "detect 2 root nodes" in {
    val error: Either[String, model.DecisionTree] = Parser
      .parseInput(
        "test",
        Source.fromResource("mindmup/2roots.json").mkString
      )
      .flatMap(_.toDecisionTree)

    error shouldBe Left("""More than one root node""")
  }

  "package" should "detect 2 the same answers" in {
    val error: Either[String, model.DecisionTree] = Parser
      .parseInput(
        "test",
        Source.fromResource("mindmup/duplicated_answers.json").mkString
      )
      .flatMap(_.toDecisionTree)

    error shouldBe Left(
      """In question "Left node" answer Left is duplicated. In question "Right node" answer Right is duplicated"""
    )
  }

}
