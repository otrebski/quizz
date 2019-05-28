
import quizz.model.{Question, QuizStep, Quizz, SuccessStep}

package object mindmup {

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

  case class Attr(parentConnector: Option[ParentConnector] = None)
  case class ParentConnector(label: Option[String] = None)

  implicit class MindmupOps(m: Mindmap) {

    def toQuizz(): Quizz = {
      def toStep(idea: Idea): QuizStep = {
        val title                    = idea.title
        val id                       = idea.id
        val ideas: Map[String, Idea] = idea.ideas.getOrElse(Map.empty[String, Idea])
        if (ideas.isEmpty) {
          SuccessStep(id.toString, title)
        } else {
          val stringToStep: Map[String, QuizStep] = ideas.map {
            case (k, v) =>
              val label = for {
                attr <- v.attr
                parentConnector <- attr.parentConnector
                label <- parentConnector.label
              } yield label
              label.get -> toStep(v)
          }
          Question(id.toString, title, stringToStep)

        }
      }

      val answers: Map[String, QuizStep] = m.ideas.map {
        case (k, v) => "Start" -> toStep(v)
      }
      Quizz(m.id, m.title, Question(m.id, m.title, answers))
    }
  }

}
