package quizz.web

import cats.syntax.option._
import quizz.engine.QuizzEngine
import quizz.model
import quizz.model.{FailureStep, Question, Quizz, SuccessStep}
import quizz.web.WebApp.Api

object Logic {

  def calculateStateOnPath(request: Api.QuizzQuery, quizz:Quizz): Either[String, Api.QuizzState] = {
    val path               = request.path
    val pathList           = path.split(";").toList.reverse
//    val quizz: model.Quizz = quizz
    val newState: Either[String, Api.QuizzState] = pathList match {
      case head :: Nil if head == "" =>
        val answers = quizz.firstStep
          .asInstanceOf[Question]
          .answers
          .map(kv => Api.Answer(kv._2.id, kv._1))
          .toList
        Right(
          Api.QuizzState(
            path = "",
            currentStep =
              Api.Step(id = quizz.firstStep.id, question = quizz.firstStep.text, answers = answers)
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
                .QuizzState(path = path,
                            currentStep =
                              Api.Step(id = f.id, question = f.text, success = Some(false)))
            case f: SuccessStep =>
              Api
                .QuizzState(
                  path = path,
                  currentStep = Api.Step(id = f.id, question = f.text, success = Some(true))
                )
          }

    }

    val valueSteps: Either[String, List[model.QuizStep]] =
      QuizzEngine.history(quizz.firstStep, pathList)
    val history: Either[String, List[Api.Step]] = valueSteps
      .map(
        h =>
          h.map {
            case q: Question =>
              val answers = q.answers.map { a =>
                Api.Answer(a._2.id, a._1, pathList.contains(a._2.id).some)
              }.toList
              Api.Step(q.id, q.text, answers)
            case f: FailureStep => Api.Step(f.id, f.text, success = Some(false))
            case s: SuccessStep => Api.Step(s.id, s.text, success = Some(true))
        }
      )

    val result: Either[String, Api.QuizzState] = for {
      state <- newState
      h     <- history
    } yield state.copy(history = h)
    result
  }

  def calculateStateOnPathStart(quiz: model.QuizStep): Either[String, Api.QuizzState] = {
    val r = quiz match {
      case q: Question =>
        val answers = q.answers.map(kv => Api.Answer(kv._2.id, kv._1)).toList
        Right(
          Api.QuizzState(
            path = "",
            currentStep = Api.Step(id = quiz.id, question = quiz.text, answers = answers)
          )
        )
      case _ => Left("Quiz have to starts question")
    }
    r
  }
}
