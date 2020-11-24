package tree.web

import java.util.Date

object Api {

  case class UserSession(session: String)

  case class DecisionTreeQuery(id: String, path: String, version: Option[Int] = None)

  case class DecisionTreeId(id: String)

  case class DecisionTreeState(
      path: String,
      currentStep: Step,
      history: List[HistoryStep] = List.empty
  )

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

  case class DecisionTreeInfo(id: String, title: String, version: Int)

  case class DecisionTreeErrorInfo(id: String, error: String)

  case class DecisionTrees(
      trees: List[DecisionTreeInfo] = List.empty,
      treesWithErrors: List[DecisionTreeErrorInfo] = List.empty
  )

  case class FeedbackSend(treeId: String, version: Int, path: String, rate: Int, comment: String)

  case class FeedbackResponse(status: String)

  case class AddDecisionTree(id: String, mindmupSource: String)

  case class DeleteDecisionTree(id: String, version: Int)

  case class AddQDecisionTreeResponse(status: String)

  case class ValidationResult(valid: Boolean, errors: List[String])

  case class TrackingSessions(sessions: List[TrackingSession])

  case class TrackingSession(session: String, date: Date, treeId: String, duration: Long)

  case class TrackingSessionHistory(details: TrackingSession, steps: List[TrackingStep])

  case class TrackingSessionHistoryQuery(session: String, treeId: String)

  case class TrackingStep(
      treeId: String,
      path: String,
      date: Date,
      session: String,
      username: Option[String]
  )

}
