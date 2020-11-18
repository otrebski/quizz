package quizz.web

import cats.syntax.option._
import quizz.engine.DecisionTreeEngine
import quizz.model
import quizz.model.{FailureStep, Question, DecisionTree, SuccessStep}
import quizz.web.Api.HistoryStep

object Logic {

  def calculateState(
                      request: Api.DecisionTreeQuery,
                      quizzes: Map[String, DecisionTree] //TODO replace map with single Quizz
  ): Either[String, Api.DecisionTreeState] = {

    val maybeQuizz = quizzes.get(request.id)

    maybeQuizz match {
      case None                                => Left("Quizz not found")
      case Some(quizz) if request.path.isEmpty => calculateStateOnPathStart(quizz.firstStep)
      case Some(quizz)                         => calculateStateOnPath(request, quizz)
    }
  }

  def calculateStateOnPath(
                            request: Api.DecisionTreeQuery,
                            quizz: DecisionTree
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
      state <- newState(quizz)
      h     <- history(quizz)
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
