package quizz.web

import cats.syntax.option._
import quizz.engine.QuizzEngine
import quizz.model
import quizz.model.{ FailureStep, Question, Quizz, SuccessStep }
import quizz.web.Api.HistoryStep

object Logic {

  def calculateStateOnPath(
      request: Api.QuizzQuery,
      quizzes: Map[String, Quizz] //TODO replace map with single Quizz
  ): Either[String, Api.QuizzState] = {
    val path = request.path

    val pathList   = path.split(";").toList.reverse
    val maybeQuizz: Either[String, Quizz] = quizzes.get(request.id).map(Right(_)).getOrElse(Left("Quizz not found"))
    if (path.isEmpty) {
      maybeQuizz.flatMap(q => calculateStateOnPathStart(q.firstStep))
    } else {
      def newState(quizz: Quizz): Either[String, Api.QuizzState] =
        pathList match {
          case head :: Nil if head == "" =>
            val answers = quizz.firstStep
              .asInstanceOf[Question]
              .answers
              .map(kv => Api.Answer(kv._2.id, kv._1))
              .toList
            Right(
              Api.QuizzState(
                path = "",
                currentStep = Api
                  .Step(id = quizz.firstStep.id, question = quizz.firstStep.text, answers = answers)
              )
            )

          case head :: tail =>
            val r: Either[String, QuizzEngine.SelectionResult] =
              QuizzEngine.process(head, quizz.firstStep, tail)

            r.map(_.current)
              .map {
                case q: Question =>
                  val currentStep = Api.Step(
                    id = q.id,
                    question = q.text,
                    answers = q.answers.map(kv => Api.Answer(kv._2.id, kv._1)).toList
                  )
                  Api.QuizzState(
                    path = path,
                    currentStep = currentStep
                  )
                case f: FailureStep =>
                  Api
                    .QuizzState(
                      path = path,
                      currentStep = Api.Step(id = f.id, question = f.text, success = Some(false))
                    )
                case f: SuccessStep =>
                  Api
                    .QuizzState(
                      path = path,
                      currentStep = Api.Step(id = f.id, question = f.text, success = Some(true))
                    )
              }

        }

      def valueSteps(quizz: Quizz): Either[String, List[model.QuizStep]] =
        QuizzEngine.history(quizz.firstStep, pathList)

      def history(quizz: Quizz): Either[String, List[Api.HistoryStep]] =
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

      val result: Either[String, Api.QuizzState] = for {
        quizz <- maybeQuizz
        state <- newState(quizz)
        h     <- history(quizz)
      } yield state.copy(history = h)
      result
    }
  }

  def calculateStateOnPathStart(quiz: model.QuizStep): Either[String, Api.QuizzState] =
    quiz match {
      case q: Question =>
        val answers: List[Api.Answer] = q.answers.map(kv => Api.Answer(kv._2.id, kv._1)).toList
        Right(
          Api.QuizzState(
            path = quiz.id,
            currentStep = Api.Step(id = quiz.id, question = quiz.text, answers = answers)
          )
        )
      case _ => Left("Quiz have to starts question")
    }
}
