package tree.feedback

import cats.effect.{ Clock, IO, Sync }
import cats.syntax.option._
import com.typesafe.scalalogging.LazyLogging
import doobie.quill.DoobieContext
import doobie.util.transactor.Transactor.Aux
import io.getquill.Literal
import tree.db.Feedback
import tree.web.Api.{ DecisionTreeState, FeedbackSend }

import scala.language.higherKinds

trait FeedbackSender[F[_]] {

  def send(feedback: FeedbackSend, treeState: DecisionTreeState): F[Unit]
}

class LogFeedbackSender[F[_]: Sync] extends FeedbackSender[F] with LazyLogging {
  override def send(feedback: FeedbackSend, treeState: DecisionTreeState): F[Unit] =
    Sync[F].delay(logger.info(s"Have feedback: $feedback for $treeState"))
}

object SlackFeedbackSender {

  case class SlackMessage(blocks: List[Block])

  case class Block(text: Text, `type`: String = "section", block_id: Option[String] = None)

  case class Text(text: String, `type`: String = "mrkdwn")

  private def feedbackIcon(feedback: FeedbackSend): String =
    feedback.rate match {
      case a if a > 0  => ":+1:"
      case a if a < 0  => ":-1:"
      case a if a == 0 => ":point_right:"
    }

  def convertFeedback(feedback: FeedbackSend, treeState: DecisionTreeState): SlackMessage = {
    val history: List[Block] = treeState.history.foldRight(List.empty[Block]) {
      (historyStep, list) =>
        val answers = historyStep.answers
          .map(answer =>
            s" - ${answer.selected
              .map(if (_) ":ballot_box_with_check:" else ":black_square_button:")
              .getOrElse(":black_square_button:")} ${answer.text}"
          )
          .mkString("\n")
        Block(
          Text(
            s"${historyStep.question}\n$answers"
          ),
          block_id = historyStep.id.some
        ) :: list
    }
    val answers = treeState.currentStep.answers
      .map(answer =>
        s" - ${answer.selected.map(if (_) ":heavy_check_mark:" else "").getOrElse("")} ${answer.text}"
      )
      .mkString("\n")
    val lastQuestion = Block(
      Text(
        s"${treeState.currentStep.question}\n$answers"
      ),
      block_id = treeState.currentStep.id.some
    )
    val header = Block(
      Text(
        s"Feedback ${feedbackIcon(feedback)} for tree ${feedback.treeId}",
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

  override def send(feedback: FeedbackSend, treeState: DecisionTreeState): F[Unit] =
    Sync[F].delay {
      import sttp.client.circe._

      val url: String = s"https://hooks.slack.com/services/$token"
      import sttp.client._
      val uri = uri"$url"

      val message = SlackFeedbackSender.convertFeedback(feedback, treeState)
      import io.circe.generic.auto._

      val myRequest = basicRequest
        .post(uri)
        .header("Content-type", "application/json")
        .body(message)

      implicit val backend: SttpBackend[Identity, Nothing, NothingT] = HttpURLConnectionBackend()
      myRequest.send()
    }
}

class FeedbackDBSender(xa: Aux[IO, Unit])(implicit clock: Clock[IO]) extends FeedbackSender[IO] {

  import cats.effect._
  import doobie._

  val trivial = LogHandler(e => Console.println("*** " + e))
  val dc      = new DoobieContext.Postgres(Literal) // Literal naming scheme

  import dc._

  override def send(feedback: FeedbackSend, treeState: DecisionTreeState): IO[Unit] = {
    import java.util.Date

    import doobie.implicits._
    for {
      now <- clock.realTime(scala.concurrent.duration.MILLISECONDS)
      fb = Feedback(
        id = 0,
        timestamp = new Date(now),
        treeId = feedback.treeId,
        path = feedback.path,
        comment = feedback.comment,
        rate = feedback.rate
      )
      _ <- addFeedbackToDb(fb).transact(xa)
    } yield ()
  }

  protected def addFeedbackToDb(feedback: Feedback): doobie.ConnectionIO[Index] = {
    val q1 = quote {
      query[Feedback].insert(lift(feedback)).returningGenerated(_.id)
    }
    run(q1)
  }
}
