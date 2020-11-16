package mindmup

import cats.syntax.either._
import cats.syntax.option._
import io.circe
import mindmup.V3IdString.{ Idea, Mindmap }
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

    val value: Either[String, Mindmap] = Parser.parseInput("test", file)
    value should be(Symbol("right"))
    value shouldBe validMindmap.asRight
  }

  "Parser" should "parse valid file with id's as int and string" in {
    val file: String = Source.fromResource("mindmup/valid_mindmup_id_Int_and_String.json").mkString

    val value: Either[String, Mindmap] = Parser.parseInput("test", file)
    value should be(Symbol("right"))
    value shouldBe validMindmap.asRight
  }

  "Parser" should "parse file with notes" in {
    val file: String = Source.fromResource("mindmup/mindmup_with_notes.mup.json").mkString

    val value: Either[String, Mindmap] = Parser.parseInput("test", file)
    value should be(Symbol("right"))
    val note: Option[Note] = for {
      map  <- value.toOption
      idea <- map.ideas.get("1")
      attr <- idea.attr
      note <- attr.note
    } yield note
    note shouldBe Note("This is note for starting point").some
  }

  "Parser" should "parse file with multiline notes" in {
    val file: String = Source.fromResource("mindmup/mindmup_with_notes.mup.json").mkString

    val value: Either[String, Mindmap] = Parser.parseInput("test", file)
    val note: Option[Note] = for {
      map      <- value.toOption
      mainIdea <- map.ideas.get("1")
      idea     <- mainIdea.ideas.get.get("1")
      attr     <- idea.attr
      note     <- attr.note
    } yield note

    note shouldBe Note("""Note
                         |with
                         |a few
                         |lines.""".stripMargin).some
  }

}
