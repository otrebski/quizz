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

package quizz

package object model {

  case class Quizz(id: String, name: String, firstStep: QuizStep)

  sealed trait QuizStep {
    def id: String
    def text: String
  }
  case class SuccessStep(id: String, text: String)                              extends QuizStep
  case class FailureStep(id: String, text: String)                              extends QuizStep
  case class Question(id: String, text: String, answers: Map[String, QuizStep]) extends QuizStep

}
