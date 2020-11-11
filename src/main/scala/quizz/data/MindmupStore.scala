package quizz.data

import cats.effect.{ Async, ContextShift, IO, Sync }

import scala.language.higherKinds
import better.files._
import cats.{ Applicative, FlatMap }
import cats.effect.concurrent.Ref
import doobie.Transactor
import doobie.util.transactor.Transactor.Aux
import quizz.db.DatabaseConfig

trait MindmupStore[F[_]] {

  def store(name: String, content: String): F[Unit]

  def listNames(): F[Set[String]]

  def load(name: String): F[String]

  def delete(name: String): F[Unit]
}

object FileMindmupStore {
  def apply[F[_]](dir: File)(implicit ev: Sync[F]): F[FileMindmupStore[F]] =
    Sync[F].delay {
      dir.createDirectoryIfNotExists(createParents = true)

      new FileMindmupStore(dir)
    }
}

class FileMindmupStore[F[_]: Sync](dir: File) extends MindmupStore[F] {

  override def store(name: String, content: String): F[Unit] =
    Sync[F].delay {
      (dir / name).overwrite(content)
    }

  override def listNames(): F[Set[String]] =
    Sync[F].delay {
      dir.list
        .filter(_.isRegularFile)
        .map(_.name)
        .toSet
    }

  override def load(name: String): F[String] =
    Sync[F].delay {
      (dir / name).contentAsString
    }

  override def delete(name: String): F[Unit] =
    Sync[F].delay {
      val toDelete = dir / name
      toDelete.delete(swallowIOExceptions = false)
    }
}

object MemoryMindmupStore {
  def apply[F[_]]()(implicit ev: Sync[F]): F[MemoryMindmupStore[F]] = {
    val ref: F[Ref[F, Map[String, String]]] =
      Ref.of[F, Map[String, String]](Map.empty[String, String])
    Applicative[F].map(ref)(r => new MemoryMindmupStore[F](r))
  }

}

class MemoryMindmupStore[F[_]: Sync](ref: Ref[F, Map[String, String]]) extends MindmupStore[F] {
  override def store(name: String, content: String): F[Unit] =
    ref.update(x => x.updated(name, content))

  override def listNames(): F[Set[String]] =
    FlatMap[F].flatMap(ref.get)(m => Applicative[F].pure(m.toList.map(_._1).toSet))

  override def load(name: String): F[String] =
    FlatMap[F].flatMap(ref.get)(m => Applicative[F].pure(m(name)))

  override def delete(name: String): F[Unit] =
    FlatMap[F].flatMap(ref.update(v => v.removed(name)))(_ => Applicative[F].pure(()))
}

object DbMindMupStore {
  def apply[F[_]](
      xa: Aux[F, Unit]
  )(implicit ec2: Async[F], ev3: ContextShift[F]): F[DbMindMupStore[F]] =
    Async[F].delay(new DbMindMupStore(xa))
}

class DbMindMupStore[F[_]: Async: ContextShift](xa: Aux[F, Unit]) extends MindmupStore[F] {

  case class Mindmup(id: String, json: String)

  import doobie.quill.DoobieContext
  import io.getquill.Literal

  val dc = new DoobieContext.Postgres(Literal) // Literal naming scheme

  import dc._
  import doobie.implicits._

  override def store(name: String, json: String): F[Unit] = {
    val q: dc.Quoted[dc.Insert[Mindmup]] = quote {
      query[Mindmup]
        .insert(lift(Mindmup(name, json)))
        .onConflictUpdate(_.id)((t, e) => t.json -> e.json)
    }
    Applicative[F].map(run(q).transact(xa))(_ => ())
  }

  override def listNames(): F[Set[String]] = {
    val q = quote {
      query[Mindmup].map(_.id)
    }
    Applicative[F].map(run(q).transact(xa))(_.toSet)
  }

  override def load(name: String): F[String] = {
    val q = quote {
      query[Mindmup].map(x => x.json)
    }
    Applicative[F].map(run(q).transact(xa))(_.head)
  }

  override def delete(name: String): F[Unit] = {
    val q = quote {
      query[Mindmup].filter(_.id == lift(name)).delete
    }
    Applicative[F].map(run(q).transact(xa))(_ => ())
  }
}
