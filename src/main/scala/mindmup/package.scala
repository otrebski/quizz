import com.typesafe.scalalogging.LazyLogging
import quizz.model.{Question, QuizStep, Quizz, SuccessStep}

package object mindmup extends LazyLogging {

  case class Attr(parentConnector: Option[ParentConnector] = None)
  case class ParentConnector(label: Option[String] = None)

  object V3IdString {
    case class Mindmap(
        id: String,
        formatVersion: Int,
        ideas: Map[String, Idea],
        title: String
    )
    case class Idea(
        title: String,
        id: String,
        attr: Option[Attr] = None,
        ideas: Option[Map[String, Idea]] = None
    )
  }

  object V3IdInt {
    case class Mindmap(
        id: String,
        formatVersion: Int,
        ideas: Map[String, Idea],
        title: String
    )
    case class Idea(
        title: String,
        id: Int,
        attr: Option[Attr] = None,
        ideas: Option[Map[String, Idea]] = None
    )

    implicit class MindmupIntOps(m: Mindmap) {

      def ideaToV3String(idea: Idea): V3IdString.Idea =
        V3IdString.Idea(
          title = idea.title,
          id = idea.id.toString,
          attr = idea.attr,
          ideas = idea.ideas.map(i => i.mapValues(ideaToV3String))
        )

      def toV3IdString: V3IdString.Mindmap =
        V3IdString.Mindmap(
          id = m.id,
          formatVersion = m.formatVersion,
          ideas = m.ideas.mapValues(ideaToV3String),
          title = m.title
        )

    }

  }

  implicit class MindmupStringOps(m: V3IdString.Mindmap) {

    //TODO change to Either[String,Quizz]
    def toQuizz: Quizz = {
      def toStep(idea: V3IdString.Idea): QuizStep = {
        val title = idea.title
        val id    = idea.id
        val ideas = idea.ideas.getOrElse(Map.empty[String, V3IdString.Idea])

        if (ideas.isEmpty) {
          SuccessStep(id.toString, title)
        } else {
          val stringToStep: Map[String, QuizStep] = ideas.map {
            case (k, v) =>
              val label = for {
                attr            <- v.attr
                parentConnector <- attr.parentConnector
                label           <- parentConnector.label
              } yield label
              label.getOrElse("?") -> toStep(v)
          }
          Question(id.toString, title, stringToStep)

        }
      }

      val answers: Map[String, QuizStep] = m.ideas.map {
        case (k, v) => m.title -> toStep(v)
      }
      Quizz(m.id, m.title, Question(m.id, m.title, answers))
    }
  }

}
