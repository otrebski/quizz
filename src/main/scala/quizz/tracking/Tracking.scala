package quizz.tracking

import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date

import better.files.Dsl.SymbolicOperations
import better.files.File
import cats.Applicative
import cats.effect.concurrent.Ref
import cats.effect.{ Async, ContextShift, Sync }
import cats.implicits.{ catsSyntaxOptionId, none }
import com.typesafe.scalalogging.LazyLogging
import doobie.util.transactor.Transactor.Aux

import scala.language.higherKinds

case class TrackingStep(
    id: Long,
    quizzId: String,
    path: String,
    date: Date,
    session: String,
    username: Option[String]
)

case class TrackingSession(session: String, quizzId: String, date: Date, duration: Long)

trait Tracking[F[_]] {
  def step(
      quizzId: String,
      path: String,
      date: Instant,
      session: String,
      user: Option[String]
  ): F[Unit]

  def session(session: String, quizzId: String): F[List[TrackingStep]]
  def listSessions(): F[List[TrackingSession]]
}

object MemoryTracking {
  def apply[F[_]: Sync](): F[MemoryTracking[F]] =
    Applicative[F]
      .map(Ref.of[F, List[TrackingStep]](List.empty[TrackingStep]))(new MemoryTracking[F](_))
}

class MemoryTracking[F[_]: Sync](ref: Ref[F, List[TrackingStep]])
    extends Tracking[F]
    with LazyLogging {
  override def step(
      quizzId: String,
      path: String,
      date: Instant,
      session: String,
      user: Option[String]
  ): F[Unit] =
    ref.update(list => TrackingStep(0, quizzId, path, Date.from(date), session, user) :: list)

  override def session(session: String, quizzId: String): F[List[TrackingStep]] =
    Applicative[F].map(ref.get) { list =>
      list
        .filter(step => step.session == session && step.quizzId == quizzId)
        .sortBy(_.date)
    }

  override def listSessions(): F[List[TrackingSession]] =
    Applicative[F].map(ref.get) { list =>
      list
        .groupBy(ts => (ts.session, ts.quizzId))
        .map {
          case ((session, quizzId), steps) =>
            val dates = steps.map(_.date)
            TrackingSession(session, quizzId, dates.min, dates.max.getTime - dates.min.getTime)
        }
        .toList
    }

}

object FileTracking {
  private val fileName = "tracking.csv"
  def apply[F[_]](dir: File)(implicit ev: Sync[F]): F[FileTracking[F]] =
    Sync[F].delay {
      (dir / fileName).createFileIfNotExists(createParents = true)
      new FileTracking(dir)
    }
}

class FileTracking[F[_]: Sync](dir: File) extends Tracking[F] {
  private val file = dir / FileTracking.fileName

  override def step(
      quizzId: String,
      path: String,
      date: Instant,
      session: String,
      user: Option[String]
  ): F[Unit] =
    Sync[F].delay {
      val dateString = new SimpleDateFormat("yyyyMMdd HHmmss").format(Date.from(date))
      file << s"$dateString, ${date.toEpochMilli}, $quizzId, $path, $session, ${user.getOrElse("")}"
    }

  private val parseEntry: String => TrackingStep = line => {
    val split = line.split(",").toList.map(_.trim)
    TrackingStep(
      id = 0,
      quizzId = split(2),
      path = split(3),
      date = new Date(split(1).toLong),
      session = split(4),
      username = if (split(5).isEmpty) none[String] else split(5).some
    )
  }

  override def session(session: String, quizzId: String): F[List[TrackingStep]] =
    Sync[F].delay {
      file.lines
        .map(parseEntry)
        .filter(step => step.session == session && step.quizzId == quizzId)
        .toList
        .sortBy(_.date)
    }

  override def listSessions(): F[List[TrackingSession]] =
    Sync[F].delay {
      val allSteps: List[TrackingStep] = file.lines
        .map(parseEntry)
        .toList

      allSteps
        .groupBy(ts => (ts.session, ts.quizzId))
        .map {
          case ((session, quizzId), steps) =>
            val dates = steps.map(_.date)
            TrackingSession(session, quizzId, dates.min, dates.max.getTime - dates.min.getTime)
        }
        .toList
    }
}

object DbTracking {
  def apply[F[_]](
      xa: Aux[F, Unit]
  )(implicit ec2: Async[F], ev3: ContextShift[F]): F[DbTracking[F]] =
    Sync[F].delay {
      new DbTracking(xa)
    }
}

class DbTracking[F[_]: Async: ContextShift](xa: Aux[F, Unit]) extends Tracking[F] {

  import doobie.implicits._
  import doobie.quill.DoobieContext
  import io.getquill.Literal
  val dc = new DoobieContext.Postgres(Literal) // Literal naming scheme
  import dc._

  override def step(
      quizzId: String,
      path: String,
      date: Instant,
      session: String,
      user: Option[String]
  ): F[Unit] = {
    val q = quote {
      query[TrackingStep]
        .insert(lift(TrackingStep(0, quizzId, path, Date.from(date), session, user)))
        .returningGenerated(_.id)
    }
    val value = run(q).transact(xa)
    Applicative[F].map(value)(_ => ())
  }

  override def session(session: String, quizzId: String): F[List[TrackingStep]] = {
    val q = quote {
      query[TrackingStep]
        .filter(ts => ts.quizzId == lift(quizzId) && ts.session == lift(session))
        .sortBy(_.date)(Ord.asc)
    }
    run(q).transact(xa)
  }
  def listSessions(): F[List[TrackingSession]] = {
    val q: dc.Quoted[dc.Query[(String, String, Option[Date], Option[Date])]] = quote {
      query[TrackingStep]
        .groupBy(s => (s.session, s.quizzId))
        .map {
          case (k, v) =>
            (
              k._1,
              k._2,
              v.map(_.date).max,
              v.map(_.date).min
            )
        }
    }
    val q1 = run(q)
      .map(_.toList)
      .transact(xa)
    import cats.implicits._
    Applicative[F].map(q1) { l =>
      l.map {
        case (session, quizzId, max, min) =>
          TrackingSession(
            session = session,
            quizzId = quizzId,
            date = min.getOrElse(new Date(0)),
            duration = (max, min).tupled.map(t => t._1.getTime - t._2.getTime).getOrElse(0)
          )
      }
    }
  }
}
