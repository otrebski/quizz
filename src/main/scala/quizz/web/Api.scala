package quizz.web

object Api {

  case class UserSession(session:String)

  case class QuizzQuery(id: String, path: String)

  case class QuizzId(id: String)

  case class QuizzState(path: String, currentStep: Step, history: List[HistoryStep] = List.empty)

  case class Step(
      id: String,
      question: String,
      answers: List[Answer] = List.empty,
      success: Option[Boolean] = None
  )

  case class HistoryStep(
      id: String,
      question: String,
      answers: List[Answer] = List.empty,
      path: List[String] = List.empty,
      success: Option[Boolean] = None
  )

  case class Answer(id: String, text: String, selected: Option[Boolean] = None)

  case class QuizzInfo(id: String, title: String)

  case class QuizzErrorInfoInfo(id: String, error: String)

  case class Quizzes(
      quizzes: List[QuizzInfo] = List.empty,
      errorQuizzes: List[QuizzErrorInfoInfo] = List.empty
  )

  case class FeedbackSend(quizzId: String, path: String, rate: Int, comment: String)

  case class FeedbackResponse(status: String)

  case class AddQuizz(id: String, mindmupSource: String)

  case class DeleteQuizz(id: String)

  case class AddQuizzResponse(status: String)

  case class ValidationResult(valid: Boolean, errors: List[String])

}
