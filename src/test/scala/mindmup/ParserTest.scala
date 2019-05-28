package mindmup

import scala.io.Source

import cats.syntax.option._
import io.circe
import org.scalatest.{ FlatSpec, Matchers }

class ParserTest extends FlatSpec with Matchers {

  "Parser" should "parse valid file " in {
    val file = Source
      .fromInputStream(
        this.getClass.getClassLoader.getResourceAsStream("mindmup/valid_mindmup.json")
      )
      .mkString

    val value: Either[circe.Error, Mindmap] = Parser.parseInput(file)
    println(value.map(_.toQuizz()))

    value should be('right)
    value.right.get shouldBe Mindmap(
      id = "root",
      title = "root node",
      formatVersion = 3,
      ideas = Map(
        "1" -> Idea(
          title = "root node",
          id = 1,
          attr = Some(Attr(none)),
          ideas = Map(
            "1" -> Idea(
              title = "child 1",
              id = 3,
              attr = Attr(ParentConnector("Label line".some).some).some,
              ideas = Map(
                "1" -> Idea(
                  title = "child 1.2",
                  id = 10,
                  ideas = Some(Map.empty)
                ),
                "2" -> Idea(
                  title = "child 1.1",
                  id = 12
                )
              ).some
            ),
            "11" -> Idea(
              id = 5,
              title = "child 2",
              attr = Attr(ParentConnector("label".some).some).some,
              ideas = Map(
                "1" -> Idea(
                  id = 7,
                  title = "child 2.1",
                  attr = Attr(ParentConnector("label line".some).some).some,
                ),
                "2" -> Idea(
                  id = 9,
                  title = "child 2.2",
                  attr = Attr(ParentConnector(none[String]).some).some
                )
              ).some
            ),
            "21" -> Idea(
              title = "child 3",
              id = 6,
              attr = Attr(none[ParentConnector]).some
            )
          ).some
        )
      )
    )
  }

}
