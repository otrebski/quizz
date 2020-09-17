package quizz.feedback

import cats.effect.Sync
import cats.syntax.option._
import com.typesafe.scalalogging.LazyLogging
import quizz.web.WebApp.Api.{ Feedback, QuizzState }

import scala.language.higherKinds

trait FeedbackSender[F[_]] {

  def send(feedback: Feedback, quizzState: QuizzState): F[Unit]
}

class LogFeedbackSender[F[_]: Sync] extends FeedbackSender[F] with LazyLogging {
  override def send(feedback: Feedback, quizzState: QuizzState): F[Unit] =
    Sync[F].delay(logger.info(s"Have feedback: $feedback for $quizzState"))
}

object SlackFeedbackSender {
  case class SlackMessage(blocks: List[Block])
  case class Block(text: Text, `type`: String = "section", block_id: Option[String] = None)
  case class Text(text: String, `type`: String = "mrkdwn")

  private def feedbackIcon(feedback: Feedback): String =
    feedback.rate match {
      case a if a > 0  => ":+1:"
      case a if a < 0  => ":-1:"
      case a if a == 0 => ":point_right:"
    }

  def convertFeedback(feedback: Feedback, quizzState: QuizzState): SlackMessage = {
    val history: List[Block] = quizzState.history.foldRight(List.empty[Block]) {
      (historyStep, list) =>
        val answers = historyStep.answers
          .map(answer =>
            s" - ${answer.selected.map(if (_) ":ballot_box_with_check:" else ":black_square_button:").getOrElse(":black_square_button:")} ${answer.text}"
          )
          .mkString("\n")
        Block(
          Text(
            s"${historyStep.question}\n$answers"
          ),
          block_id = historyStep.id.some
        ) :: list
    }
    val answers = quizzState.currentStep.answers
      .map(answer =>
        s" - ${answer.selected.map(if (_) ":heavy_check_mark:" else "").getOrElse("")} ${answer.text}"
      )
      .mkString("\n")
    val lastQuestion = Block(
      Text(
        s"${quizzState.currentStep.question}\n$answers"
      ),
      block_id = quizzState.currentStep.id.some
    )
    val header = Block(
      Text(
        s"Feedback ${feedbackIcon(feedback)} for quiz ${feedback.quizzId}",
        `type` = "plain_text"
      ),
      block_id = "header".some,
      `type` = "header"
    )
    val comment = Block(
      Text(s"Comment: ${feedback.comment}"),
      block_id = "comment".some
    )
    SlackMessage(header :: comment :: history ::: List(lastQuestion))
  }
}

class SlackFeedbackSender[F[_]: Sync](token: String) extends FeedbackSender[F] {

  override def send(feedback: Feedback, quizzState: QuizzState): F[Unit] =
    Sync[F].delay {
      import sttp.client.circe._

      val url: String = s"https://hooks.slack.com/services/$token"
      import sttp.client._
      val uri = uri"$url"
      val rate = feedback.rate match {
        case a if a > 0  => ":+1:"
        case a if a < 0  => ":-1:"
        case a if a == 0 => ":point_right:"
      }

      val message = SlackFeedbackSender.convertFeedback(feedback, quizzState)
      import io.circe.generic.auto._

      val myRequest = basicRequest
        .post(uri)
        .header("Content-type", "application/json")
        .body(message)

      implicit val backend: SttpBackend[Identity, Nothing, NothingT] = HttpURLConnectionBackend()
      val response: Identity[Response[Either[String, String]]]       = myRequest.send()
      response
    }
}
