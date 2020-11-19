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

package tree.engine

import tree.model.{FailureStep, Question, DecisionTreeStep, SuccessStep}

object DecisionTreeEngine {

  case class SelectionResult(current: DecisionTreeStep, selections: List[String])

  def history(tree: DecisionTreeStep, selections: List[String]): Either[String, List[DecisionTreeStep]] = {
    def next(current: DecisionTreeStep, path: List[String]): Either[String, List[DecisionTreeStep]] =
      path match {
        case head :: tail =>
          current match {
            case q: Question =>
              q.answers.values
                .find(_.id == head)
                .map(step => next(step, tail).map(r => step :: r))
                .getOrElse(Left("Error"))
            case f: FailureStep => Right(List(f))
            case f: SuccessStep => Right(List(f))
          }
        case Nil => Right(Nil)
      }

    if (selections.reverse.headOption.contains(tree.id))
      next(tree, selections.dropRight(1).tail.reverse).map(tree :: _)
    else
      Left("Wrong path, not starting with first node selection")
  }

  def process(
               answerId: String,
               tree: DecisionTreeStep,
               selections: List[String]
  ): Either[String, SelectionResult] = {
    def select(path: List[String], tree: DecisionTreeStep): Either[String, DecisionTreeStep] =
      path.headOption match {
        case Some(c) =>
          tree match {
            case q: Question =>
              q.answers.values
                .find(_.id == c)
                .map(step => select(path.tail, step))
                .getOrElse(Left("Wrong path"))
            case _ => Left("Wrong path")
          }

        case None =>
          tree match {
            case q: Question =>
              val values = q.answers.values
              values
                .find(_.id == answerId)
                .map(s => Right(s))
                .getOrElse(Left("Wrong selection"))
            case s: SuccessStep => Right(s)
            case f: FailureStep => Right(f)
          }
      }
    if (selections.reverse.headOption.contains(tree.id)) {
      val value: Either[String, DecisionTreeStep] = select(selections.reverse.drop(1), tree)
      value.map(step => SelectionResult(step, answerId :: selections))
    } else
      Left("Wrong path, not starting with first node selection")
  }

}
