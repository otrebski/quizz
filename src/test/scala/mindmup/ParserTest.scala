package mindmup

import cats.syntax.either._
import cats.syntax.option._
import io.circe
import mindmup.V3IdString.{Idea, Mindmap}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.io.Source

class ParserTest extends AnyFlatSpec with Matchers {

  val validMindmap: Mindmap = Mindmap(
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
            attr = Attr(ParentConnector("Label line".some).some).some,
            ideas = Map(
              "1" -> Idea(
                title = "child 1.2",
                id = "10",
                ideas = Some(Map.empty)
              ),
              "2" -> Idea(
                title = "child 1.1",
                id = "12"
              )
            ).some
          ),
          "11" -> Idea(
            id = "5",
            title = "child 2",
            attr = Attr(ParentConnector("label".some).some).some,
            ideas = Map(
              "1" -> Idea(
                id = "7",
                title = "child 2.1",
                attr = Attr(ParentConnector("label line".some).some).some
              ),
              "2" -> Idea(
                id = "9",
                title = "child 2.2",
                attr = Attr(ParentConnector(none[String]).some).some
              )
            ).some
          ),
          "21" -> Idea(
            title = "child 3",
            id = "6",
            attr = Attr(none[ParentConnector]).some
          )
        ).some
      )
    )
  )

  "Parser" should "parse valid file with id's as string" in {
    val file = Source.fromResource("mindmup/valid_mindmup_id_String.json").mkString

    val value: Either[circe.Error, Mindmap] = Parser.parseInput(file)
    value should be(Symbol("right"))
    value shouldBe validMindmap.asRight
  }

  "Parser" should "parse valid file with id's as int and string" in {
    val file: String = Source.fromResource("mindmup/valid_mindmup_id_Int_and_String.json").mkString

    val value: Either[circe.Error, Mindmap] = Parser.parseInput(file)
    value should be(Symbol("right"))
    value shouldBe validMindmap.asRight
  }

}
