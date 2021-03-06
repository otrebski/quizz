package tree.tracking

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
    treeId: String,
    version: Int,
    path: String,
    date: Date,
    session: String,
    username: Option[String]
)

case class TrackingSession(
    session: String,
    treeId: String,
    version: Int,
    date: Date,
    duration: Long
)

trait Tracking[F[_]] {
  def step(
      treeId: String,
      version: Int,
      path: String,
      date: Instant,
      session: String,
      user: Option[String]
  ): F[Unit]

  def session(session: String, treeId: String): F[List[TrackingStep]]
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
      treeId: String,
      version: Int,
      path: String,
      date: Instant,
      session: String,
      user: Option[String]
  ): F[Unit] =
    ref.update(list =>
      TrackingStep(0, treeId, version, path, Date.from(date), session, user) :: list
    )

  override def session(session: String, treeId: String): F[List[TrackingStep]] =
    Applicative[F].map(ref.get) { list =>
      list
        .filter(step => step.session == session && step.treeId == treeId)
        .sortBy(_.date)
    }

  override def listSessions(): F[List[TrackingSession]] =
    Applicative[F].map(ref.get) { list =>
      list
        .groupBy(ts => (ts.session, ts.treeId))
        .map {
          case ((session, treeId), steps) =>
            val dates   = steps.map(_.date)
            val version = steps.map(_.version).max
            TrackingSession(
              session,
              treeId,
              version,
              dates.min,
              dates.max.getTime - dates.min.getTime
            )
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
      treeId: String,
      version: Int,
      path: String,
      date: Instant,
      session: String,
      user: Option[String]
  ): F[Unit] =
    Sync[F].delay {
      val dateString = new SimpleDateFormat("yyyyMMdd HHmmss").format(Date.from(date))
      file << s"$dateString, ${date.toEpochMilli}, $treeId, $version, $path, $session, ${user.getOrElse("")}"
    }

  private val parseEntry: String => TrackingStep = line => {
    val split = line.split(",").toList.map(_.trim)
    TrackingStep(
      id = 0,
      treeId = split(2),
      version = split(3).toInt,
      path = split(4),
      date = new Date(split(1).toLong),
      session = split(5),
      username = if (split(6).isEmpty) none[String] else split(6).some
    )
  }

  override def session(session: String, treeId: String): F[List[TrackingStep]] =
    Sync[F].delay {
      file.lines
        .map(parseEntry)
        .filter(step => step.session == session && step.treeId == treeId)
        .toList
        .sortBy(_.date)
    }

  override def listSessions(): F[List[TrackingSession]] =
    Sync[F].delay {
      val allSteps: List[TrackingStep] = file.lines
        .map(parseEntry)
        .toList

      allSteps
        .groupBy(ts => (ts.session, ts.treeId))
        .map {
          case ((session, treeId), steps) =>
            val dates   = steps.map(_.date)
            val version = steps.map(_.version).max
            TrackingSession(
              session,
              treeId,
              version,
              dates.min,
              dates.max.getTime - dates.min.getTime
            )
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
      treeId: String,
      version: Int,
      path: String,
      date: Instant,
      session: String,
      user: Option[String]
  ): F[Unit] = {
    val q = quote {
      query[TrackingStep]
        .insert(lift(TrackingStep(0, treeId, version, path, Date.from(date), session, user)))
        .returningGenerated(_.id)
    }
    val value = run(q).transact(xa)
    Applicative[F].map(value)(_ => ())
  }

  override def session(session: String, treeId: String): F[List[TrackingStep]] = {
    val q = quote {
      query[TrackingStep]
        .filter(ts => ts.treeId == lift(treeId) && ts.session == lift(session))
        .sortBy(_.date)(Ord.asc)
    }
    run(q).transact(xa)
  }
  def listSessions(): F[List[TrackingSession]] = {
    val q = quote {
      query[TrackingStep]
        .groupBy(s => (s.session, s.treeId))
        .map {
          case (k, v) =>
            (
              k._1,
              k._2,
              v.map(_.version).max,
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
        case (session, treeId, version, max, min) =>
          TrackingSession(
            session = session,
            treeId = treeId,
            version = version.getOrElse(1),
            date = min.getOrElse(new Date(0)),
            duration = (max, min).tupled.map(t => t._1.getTime - t._2.getTime).getOrElse(0)
          )
      }
    }
  }
}
