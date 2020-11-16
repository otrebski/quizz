import cats.{ Eval, Foldable }
import cats.implicits.catsSyntaxEitherId
import com.typesafe.scalalogging.LazyLogging
import quizz.model.{ Question, QuizStep, Quizz, SuccessStep }

import scala.collection.{ MapView, immutable }

package object mindmup extends LazyLogging {

  case class Note(text: String)
  case class Attr(parentConnector: Option[ParentConnector] = None, note: Option[Note] = None)
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
        ideas: Option[Map[String, V3IdString.Idea]] = None
    )

    implicit class MindmupIntOps(m: Mindmap) {

      def ideaToV3String(idea: Idea): V3IdString.Idea =
        V3IdString.Idea(
          title = idea.title,
          id = idea.id.toString,
          attr = idea.attr,
          ideas = idea.ideas
        )

      def toV3IdString: V3IdString.Mindmap =
        V3IdString.Mindmap(
          id = m.id,
          formatVersion = m.formatVersion,
          ideas = m.ideas.view.mapValues(ideaToV3String).toMap,
          title = m.title
        )
    }

  }

  implicit class MindmupStringOps(mindmap: V3IdString.Mindmap) {

    def toQuizz: Either[String, Quizz] =
      for {
        validateRootNodes  <- validateSingleRoot(mindmap)
        validateDuplicates <- validateDuplicateAnswer(validateRootNodes)
        quizz              <- convertMindmup(validateDuplicates)
        validated          <- validateEmptyAnswer(quizz)
      } yield validated

    private def convertMindmup(m: V3IdString.Mindmap): Either[String, Quizz] = {
      def toStep(idea: V3IdString.Idea): QuizStep = {
        val note = for {
          attr <- idea.attr
          note <- attr.note
        } yield note.text

        val title = idea.title + note.map(t => s"\n$t").getOrElse("")
        val id    = idea.id
        val ideas = idea.ideas.getOrElse(Map.empty[String, V3IdString.Idea])

        if (ideas.isEmpty)
          SuccessStep(id, title)
        else {
          val stringToStep: Map[String, QuizStep] = ideas.values.map { v =>
            val label = for {
              attr            <- v.attr
              parentConnector <- attr.parentConnector
              label           <- parentConnector.label
            } yield label
            label.getOrElse("") -> toStep(v)
          }.toMap
          Question(id, title, stringToStep)
        }
      }

      m.ideas.headOption.map(_._2) match {
        case None => "No nodes".asLeft
        case Some(value) =>
          val maybeIdeas: Option[Map[String, V3IdString.Idea]] = value.ideas
          maybeIdeas match {
            case None => "There is no nodes".asLeft[Quizz]
            case Some(ideas) =>
              val answers: Map[String, QuizStep] = ideas.map {
                case (k, v) =>
                  val label: Option[String] = for {
                   attr <- v.attr
                   parent <-attr.parentConnector
                   label <- parent.label
                  } yield label
                  label.getOrElse("") -> toStep(v)
              }
              val firstQuestion: Question = Question(m.id, m.title, answers)
              Quizz(m.id, m.title, firstQuestion).asRight[String]
          }
      }

    }

    private def validateEmptyAnswer(quizz: Quizz): Either[String, Quizz] = {
      def detectEmpty(quizStep: QuizStep): List[String] =
        quizStep match {
          case Question(id, text, answers) =>
            val empty = answers.keys.count(_.trim.isEmpty)
            val detectedEmpty =
              if (empty == 0)
                List()
              else
                List(s"""Node "$text" has answer without text""")
            detectedEmpty ::: answers.values.flatMap(a => detectEmpty(a)).toList
          case _ => List.empty[String]
        }
      detectEmpty(quizz.firstStep) match {
        case Nil  => quizz.asRight
        case list => list.mkString(". ").asLeft
      }

    }

    private def validateSingleRoot(m: V3IdString.Mindmap): Either[String, V3IdString.Mindmap] =
      if (m.ideas.size > 1)
        "More than one root node".asLeft
      else if (m.ideas.isEmpty)
        "Decision tree is empty".asLeft
      else
        m.asRight

    private def validateDuplicateAnswer(
        m: V3IdString.Mindmap
    ): Either[String, V3IdString.Mindmap] = {
      def countDuplicates(title: String, ideas: Iterable[V3IdString.Idea]): List[String] = {
        val labels = ideas.flatMap { idea =>
          for {
            attr            <- idea.attr
            parentConnector <- attr.parentConnector
            label           <- parentConnector.label
          } yield label
        }
        val errors = labels
          .groupBy(identity)
          .filter(_._2.size > 1)
          .keySet
          .map(answer => s"""In question "$title" answer $answer is duplicated""")

        val a = ideas.flatMap(idea =>
          countDuplicates(idea.title, idea.ideas.map(_.values).getOrElse(List.empty))
        )
        errors.toList ::: a.toList
      }
      val values: Iterable[V3IdString.Idea] = m.ideas.values
      val title                             = m.title
      val duplicates                        = countDuplicates(title, values)
      if (duplicates.isEmpty)
        m.asRight
      else
        duplicates.mkString(". ").asLeft
    }

  }
}
