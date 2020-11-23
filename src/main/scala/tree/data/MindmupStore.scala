package tree.data

import better.files._
import cats.effect.concurrent.Ref
import cats.effect.{Async, ContextShift, Sync}
import cats.{Applicative, FlatMap, Functor}
import doobie.util.transactor.Transactor.{Aux, connect}

import scala.language.higherKinds

object MindmupStore {

  case class Mindmup(name: String, version: Int, content: String)

}

trait MindmupStore[F[_]] {

  def store(name: String, content: String): F[Unit]

  def listNames(): F[Set[String]]

  def versions(name: String): F[List[Int]]

  def latestVersion(name: String): F[Int]

  def load(name: String, version: Int): F[MindmupStore.Mindmup]

  def delete(name: String, version: Int): F[Unit]
}

object FileMindmupStore {
  def apply[F[_]](dir: File)(implicit ev: Sync[F]): F[FileMindmupStore[F]] =
    Sync[F].delay {
      dir.createDirectoryIfNotExists(createParents = true)

      new FileMindmupStore(dir)
    }
}

class FileMindmupStore[F[_]: Sync: Applicative](dir: File) extends MindmupStore[F] {

  override def store(name: String, content: String): F[Unit] = {
    val version: F[Int] = latestVersion(name)
    Sync[F].flatMap(version)(v =>
      Sync[F].delay((dir / name / (v + 1).toString).overwrite(content))
    )
  }

  override def listNames(): F[Set[String]] =
    Sync[F].delay {
      dir.list
        .filter(_.isDirectory)
        .map(_.name)
        .toSet
    }

  def versions(name: String): F[List[Int]] =
    Sync[F].delay {
      (dir / name).list
        .filter(_.isRegularFile)
        .filter(_.name.matches("\\d+"))
        .map(_.name.toInt)
        .toList
    }

  override def load(name: String, version: Int): F[MindmupStore.Mindmup] =
    Sync[F].delay {
      MindmupStore.Mindmup(name, version, (dir / name / version.toString).contentAsString)
    }

  override def delete(name: String, version: Int): F[Unit] =
    Sync[F].delay {
      val toDelete = dir / name / version.toString
      toDelete.delete(swallowIOExceptions = false)
    }

  override def latestVersion(name: String): F[Int] =
    Applicative[F].map(versions(name))(list => list.maxOption.getOrElse(0))
}

object MemoryMindmupStore {
  def apply[F[_]]()(implicit ev: Sync[F]): F[MemoryMindmupStore[F]] = {
    val ref = Ref.of[F, List[MindmupStore.Mindmup]](List.empty[MindmupStore.Mindmup])
    Applicative[F].map(ref)(r => new MemoryMindmupStore[F](r))
  }

}

class MemoryMindmupStore[F[_]: Sync](ref: Ref[F, List[MindmupStore.Mindmup]]) extends MindmupStore[F] {
  override def store(name: String, content: String): F[Unit] = {
    ref.update{ list =>
      val version = list.map(_.version).maxOption.getOrElse(0) + 1
      MindmupStore.Mindmup(name, version, content) :: list
    }
  }

  override def listNames(): F[Set[String]] =
    FlatMap[F].map(ref.get)(m => m.map(_.name).toSet)

  override def load(name: String, version: Int): F[MindmupStore.Mindmup] =
    FlatMap[F].map(ref.get)(_.filter(m => m.name == name && m.version == version).head)

  override def delete(name: String, version: Int): F[Unit] =
    FlatMap[F].map(ref.update(_.filterNot(m => m.name == name && m.version == version)))(_ => Applicative[F].pure(()))

  override def versions(name: String): F[List[Int]] = {
    FlatMap[F].map(ref.get)(list => list.filter(m => m.name == name).map(_.version))
  }

  override def latestVersion(name: String): F[Int] = Functor[F].map(versions(name))(_.maxOption.getOrElse(0))
}

object DbMindMupStore {
  def apply[F[_]](
      xa: Aux[F, Unit]
  )(implicit ec2: Async[F], ev3: ContextShift[F]): F[DbMindMupStore[F]] =
    Async[F].delay(new DbMindMupStore(xa))
}

class DbMindMupStore[F[_]: Async: ContextShift](xa: Aux[F, Unit]) extends MindmupStore[F] {

  case class Mindmup(version:Int, name: String, json: String)

  import doobie.quill.DoobieContext
  import io.getquill.Literal

  val dc = new DoobieContext.Postgres(Literal) // Literal naming scheme

  import dc._
  import doobie.implicits._

  override def store(name: String, json: String): F[Unit] = {
    val q = quote {
      query[Mindmup]
        .insert(lift(Mindmup(0,name, json)))
        .returningGenerated(_.version)
    }
    Applicative[F].map(run(q).transact(xa))(_ => ())
  }

  override def listNames(): F[Set[String]] = {
    val q = quote {
      query[Mindmup].map(_.name).distinct
    }
    Applicative[F].map(run(q).transact(xa))(_.toSet)
  }

  override def load(name: String, version: Int): F[MindmupStore.Mindmup] = {
    val q = quote {
      query[Mindmup].filter(m => m.name == lift(name) && m.version == lift(version))map(x => Mindmup(x.version, x.name, x.json))
    }
    Applicative[F]
      .map(run(q).transact(xa))(c => MindmupStore.Mindmup(name, version, c.head.json))

  }
  override def delete(name: String, version: Int): F[Unit] = {
    val q = quote {
      query[Mindmup].filter(m => m.name == lift(name) && m.version == lift(version)).delete
    }
    Applicative[F].map(run(q).transact(xa))(_ => ())
  }

  override def versions(name: String): F[List[Int]] = {
    val q: dc.Quoted[dc.Query[Index]] = quote {
      query[Mindmup].filter(_.name == lift(name)).sortBy(_.version)(Ord.desc).map(_.version)
    }
    Functor[F].map(run(q).transact(xa))(l => l)
  }

  override def latestVersion(name: String): F[Index] = Functor[F].map(versions(name))(_.maxOption.getOrElse(0))
}
