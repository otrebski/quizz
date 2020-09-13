package quizz.feedback

import cats.effect.{IO, Sync}
import com.typesafe.scalalogging.LazyLogging
import quizz.web.WebApp.Api.Feedback

import scala.language.higherKinds

trait FeedbackSender[F[_]] {

  def send(feedback: Feedback): F[Unit]
}

class LogFeedbackSender[F[_]: Sync] extends FeedbackSender[F] with LazyLogging {
  override def send(feedback: Feedback): F[Unit] =
    Sync[F].delay(logger.info(s"Have feedback: $feedback"))
}

class SlackFeedbackSender[F[_]: Sync](token: String) extends FeedbackSender[F] {
  override def send(feedback: Feedback): F[Unit] =
    Sync[F].delay {
      val url: String = s"https://hooks.slack.com/services/$token"
      import sttp.client._
      val uri = uri"$url"
      val rate = feedback.rate match {
        case a if a > 0  => ":+1:"
        case a if a < 0  => ":-1:"
        case a if a == 0 => ":point_right:"
      }

      //change to model
      val msg = s"""
       |{
       |  "blocks": [
       |    {
       |      "type": "section",
       |      "text": {
       |        "type": "mrkdwn",
       |        "text": "Quizz feedback has been send"
       |      }
       |    },
       |    {
       |      "type": "section",
       |      "block_id": "section1",
       |      "text": {
       |        "type": "mrkdwn",
       |        "text": "*Quizz*: ${feedback.quizzId}\\nPath: ${feedback.path}"
       |      }
       |    },
       |    {
       |      "type": "section",
       |      "block_id": "section2",
       |      "fields": [
       |        {
       |          "type": "mrkdwn",
       |          "text": "*Rating:*\\n ${rate}"
       |        }
       |      ]
       |    },
       |    {
       |      "type": "section",
       |      "block_id": "section3",
       |      "fields": [
       |        {
       |          "type": "mrkdwn",
       |          "text": "*Comment:*\\n ${feedback.comment.replace("\n", "\\\\n")}"
       |        }
       |      ]
       |    }
       |  ]
       |}
       |""".stripMargin
      val myRequest = basicRequest
        .post(uri)
        .header("Content-type", "application/json")
        .body(msg)
      implicit val backend: SttpBackend[Identity, Nothing, NothingT] = HttpURLConnectionBackend()
      val response: Identity[Response[Either[String, String]]] = myRequest.send()

    }
}
