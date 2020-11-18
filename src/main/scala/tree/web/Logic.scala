package tree.web

import cats.syntax.option._
import tree.engine.DecisionTreeEngine
import tree.model
import tree.model.{FailureStep, Question, DecisionTree, SuccessStep}
import tree.web.Api.HistoryStep

object Logic {

  def calculateState(
                      request: Api.DecisionTreeQuery,
                      trees: Map[String, DecisionTree] //TODO replace map with single Quizz
  ): Either[String, Api.DecisionTreeState] = {

    val maybeTree = trees.get(request.id)

    maybeTree match {
      case None                                => Left("Tree not found")
      case Some(tree) if request.path.isEmpty => calculateStateOnPathStart(tree.firstStep)
      case Some(tree)                         => calculateStateOnPath(request, tree)
    }
  }

  def calculateStateOnPath(
                            request: Api.DecisionTreeQuery,
                            tree: DecisionTree
  ): Either[String, Api.DecisionTreeState] = {
    val path     = request.path
    val pathList = path.split(";").toList.reverse
    def newState(quizz: DecisionTree): Either[String, Api.DecisionTreeState] =
      pathList match {
        case head :: Nil if head == "" =>
          val answers = quizz.firstStep
            .asInstanceOf[Question]
            .answers
            .map(kv => Api.Answer(kv._2.id, kv._1))
            .toList
          Right(
            Api.DecisionTreeState(
              path = "",
              currentStep = Api
                .Step(id = quizz.firstStep.id, question = quizz.firstStep.text, answers = answers)
            )
          )

        case head :: tail =>
          val r: Either[String, DecisionTreeEngine.SelectionResult] =
            DecisionTreeEngine.process(head, quizz.firstStep, tail)

          r.map(_.current)
            .map {
              case q: Question =>
                val currentStep = Api.Step(
                  id = q.id,
                  question = q.text,
                  answers = q.answers.map(kv => Api.Answer(kv._2.id, kv._1)).toList
                )
                Api.DecisionTreeState(
                  path = path,
                  currentStep = currentStep
                )
              case f: FailureStep =>
                Api
                  .DecisionTreeState(
                    path = path,
                    currentStep = Api.Step(id = f.id, question = f.text, success = Some(false))
                  )
              case f: SuccessStep =>
                Api
                  .DecisionTreeState(
                    path = path,
                    currentStep = Api.Step(id = f.id, question = f.text, success = Some(true))
                  )
            }

      }

    def valueSteps(quizz: DecisionTree): Either[String, List[model.DecisionTreeStep]] =
      DecisionTreeEngine.history(quizz.firstStep, pathList)

    def history(quizz: DecisionTree): Either[String, List[Api.HistoryStep]] =
      valueSteps(quizz)
        .map(h =>
          h.foldLeft(List.empty[Api.HistoryStep]) { (list, step) =>
              val path = list.map(_.id).reverse
              val hStep = step match {
                case Question(id, text, answers) =>
                  val historyAnswers = answers.map { a =>
                    Api.Answer(a._2.id, a._1, pathList.contains(a._2.id).some)
                  }.toList
                  HistoryStep(id, text, historyAnswers, path)
                case f: FailureStep =>
                  Api.HistoryStep(f.id, f.text, success = Some(false), path = path)
                case s: SuccessStep =>
                  Api.HistoryStep(s.id, s.text, success = Some(true), path = path)
              }
              hStep :: list
            }
            .reverse
        )

    val result: Either[String, Api.DecisionTreeState] = for {
      state <- newState(tree)
      h     <- history(tree)
    } yield state.copy(history = h)
    result

  }

  def calculateStateOnPathStart(quiz: model.DecisionTreeStep): Either[String, Api.DecisionTreeState] =
    quiz match {
      case q: Question =>
        val answers: List[Api.Answer] = q.answers.map(kv => Api.Answer(kv._2.id, kv._1)).toList
        Right(
          Api.DecisionTreeState(
            path = quiz.id,
            currentStep = Api.Step(id = quiz.id, question = quiz.text, answers = answers)
          )
        )
      case _ => Left("Quiz have to starts question")
    }
}
