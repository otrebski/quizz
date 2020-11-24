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

package tree
import cats.Show

package object model {

  case class DecisionTree(id: String, name: String, firstStep: DecisionTreeStep)

  sealed trait DecisionTreeStep extends Product with Serializable {
    def id: String
    def text: String
  }

  case class SuccessStep(id: String, text: String) extends DecisionTreeStep
  case class FailureStep(id: String, text: String) extends DecisionTreeStep
  case class Question(id: String, text: String, answers: Map[String, DecisionTreeStep])
      extends DecisionTreeStep

  implicit val decisionTreeShowShow: Show[DecisionTree] = Show.show { q =>
    def stepToString(step: DecisionTreeStep, depth: Int = 0): String = {
      val answers = step match {
        case Question(_, _, a) => a.values.map(i => stepToString(i, depth + 1)).mkString("\n")
        case _                 => ""
      }
      val intend = " " * depth
      s"""$intend${step.id} [${step.text}]
         |$answers""".stripMargin.replaceAll("\\s+$", "")

    }

    s"Decision tree: ${q.id} [${q.name}]\n${stepToString(q.firstStep)}"
  }

}
