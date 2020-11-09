package quizz.tracking

import java.time.Instant

import cats.effect.Sync
import com.typesafe.scalalogging.LazyLogging

trait Tracking[F[_]] {
  def step(
      quizzId: String,
      path: String,
      date: Instant,
      session: String,
      user: Option[String]
  ): F[Unit]
}

object TrackingConsole {
  def apply[F[_]: Sync](): F[TrackingConsole[F]] = Sync[F].delay(new TrackingConsole[F]())
}

class TrackingConsole[F[_]: Sync] extends Tracking[F] with LazyLogging {
  override def step(
      quizzId: String,
      path: String,
      date: Instant,
      session: String,
      user: Option[String]
  ): F[Unit] =
    Sync[F].delay {
      logger.info(s"Tracking: Quizz: $quizzId path: $path User $session/${user.getOrElse("N/A")}")
    }
}
