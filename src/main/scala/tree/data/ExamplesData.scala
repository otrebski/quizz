/*
 * Copyright 2019 k.otrebski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tree.data

import cats.syntax.show._
import com.typesafe.scalalogging.LazyLogging
import mindmup.Parser
import tree.model.{FailureStep, Question, DecisionTree, SuccessStep}
import tree.web.Api.{Answer, HistoryStep, DecisionTreeState, Step}

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

  private val exampleSource = Source
    .fromInputStream(this.getClass.getClassLoader.getResourceAsStream("tree.mup.json"))
    .mkString
  private val errorOrDecisionTree: Either[String, DecisionTree] =
    Parser.parseInput("example", exampleSource).flatMap(_.toDecisionTree)

  logger.info(s"Loaded trees: ${errorOrDecisionTree.map(_.show)}")
  val treeMindmup: DecisionTree = errorOrDecisionTree.toOption.get

  val trees: Map[String, DecisionTree] = List(
    DecisionTree("q1", "Example tree", tree),
    DecisionTree("q2", "Another tree", tree),
    treeMindmup
  ).map(q => q.id -> q).toMap

}