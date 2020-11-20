package tree.data

import com.typesafe.scalalogging.LazyLogging
import mindmup.Parser
import tree.model.{ DecisionTree, FailureStep, Question, SuccessStep }
import tree.web.Api.{ Answer, DecisionTreeState, HistoryStep, Step }

import scala.io.Source

object ExamplesData extends LazyLogging {

  val tree: Question = Question(
    "root",
    "What kind of problem do you have",
    Map(
      "Electricity" -> Question(
        "electricity",
        "What kind of issue?",
        Map(
          "No power at home" ->
          Question(
            "whereIsOutage",
            "Does your neighborhood has power",
            Map(
              "yes" -> SuccessStep("localOutage", "Check your fuses or pay you bills"),
              "no"  -> SuccessStep("totalOutage", "Probably outage, just wait")
            )
          ),
          "No power at some lights" ->
          Question(
            "checkLocal",
            "Are fuses ok in your apartment",
            Map(
              "no" -> Question(
                "localFusesDown",
                "Turn them on. Is it solved?",
                Map(
                  "yes" -> SuccessStep("localFusesFixed", "Congrats"),
                  "no"  -> FailureStep("localFusesBroken", "Shit!")
                )
              ),
              "yes" -> Question(
                "buildingFuses",
                "Are fuses outside your apartment ok?",
                Map(
                  "yes" -> FailureStep("noPower", "Pay your bills!"),
                  "no" -> Question(
                    "buildingFusesBroken",
                    "Fix fuses outside",
                    Map(
                      "Problem fixed"     -> SuccessStep("buildingFusesFixed", "Congrats"),
                      "Still not working" -> FailureStep("buildingFusesNotFixed", "Pay your bills")
                    )
                  )
                )
              )
            )
          )
        )
      ),
      "Water or plumbing" -> Question(
        "waterOrPlumbing",
        "What kind of problem",
        Map(
          "Water is leaking" -> FailureStep("waterLeaking", "Call for plumber"),
          "No hot water"     -> FailureStep("noHotWater", "Call your local maintainer"),
          "Water is dirty"   -> FailureStep("dirtyWater", "Call your local maintainer")
        )
      ),
      "Phone or internet" -> Question(
        "phoneOrInternet",
        "Phone or Internet",
        Map(
          "Cell phone has no signal" -> Question(
            "phoneNoSignal",
            "Restart phone",
            Map(
              "Working"     -> SuccessStep("phoneWorkingAfterRestart", "Congrats!"),
              "Not working" -> FailureStep("phoneNotWorkingAfterRestart", "Use other phone")
            )
          ),
          "Internet is slow" -> Question(
            "slowInternet",
            "Are you downloading movies or watching videos online",
            Map(
              "yes" -> Question(
                "internetHeavilyUsed",
                "Turn off all redundant transfers. Is it working better now?",
                Map(
                  "yes" -> SuccessStep("internetSolved", "Congrats"),
                  "no"  -> SuccessStep("internetNotSolved2", "Call your internet provider")
                )
              ),
              "no" -> FailureStep("internetNotSolved1", "Call your internet provider")
            )
          )
        )
      )
    )
  )

  object Fake {

    val exampleStateInProgress: DecisionTreeState = DecisionTreeState(
      path = "root",
      currentStep = Step(
        "a",
        "I co dalej?",
        List(Answer("", "jedziemy"), Answer("", "Stoimi"), Answer("", "Lezymy"))
      ),
      history = List(
        HistoryStep(
          "b",
          "I co dalej 1 ?",
          List(
            Answer("", "jedziemy", selected = Some(true)),
            Answer("", "Stoimi"),
            Answer("", "Lezymy")
          )
        ),
        HistoryStep(
          "c",
          "I co dalej 2 ?",
          List(
            Answer("", "jedziemy"),
            Answer("", "Stoimi", selected = Some(true)),
            Answer("", "Lezymy")
          )
        ),
        HistoryStep(
          "d",
          "I co dalej 3 ?",
          List(
            Answer("", "jedziemy"),
            Answer("", "Stoimi"),
            Answer("", "Lezymy", selected = Some(true))
          )
        )
      )
    )
    val exampleStateFinalSuccess: DecisionTreeState = DecisionTreeState(
      path = "asdfsdf",
      currentStep = Step("a", "I co dalej?", List.empty, success = Some(true)),
      history = List()
    )
    val exampleStateFinalFailure: DecisionTreeState = DecisionTreeState(
      path = "asdfsdf",
      currentStep = Step("a", "I co dalej?", List.empty, success = Some(false)),
      history = List()
    )
  }

  val trees: Map[String, DecisionTree] = List(
    DecisionTree("q1", "Example tree", tree),
    DecisionTree("q2", "Another tree", tree)
  ).map(q => q.id -> q).toMap

}
