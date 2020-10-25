package quizz.data

import java.io.{ File, FileFilter }

import scala.io.Source

import cats.syntax.either._
import com.typesafe.scalalogging.LazyLogging
import io.circe
import mindmup.{ Parser, V3IdString }
import quizz.model.Quizz

object Loader extends LazyLogging {

  def fromFolder(folder: File): Either[String, List[Quizz]] = {
    val files = folder.listFiles(new FileFilter {
      override def accept(pathname: File): Boolean =
        pathname.isFile && pathname.getName.endsWith("mup")
    })

    files.map(fromFile).foldLeft(List.empty[Quizz].asRight[String]) {
      case (Right(list), Right(quizz)) => Right(quizz :: list)
      case (Left(error), _)            => Left(error)
      case (_, Left(error))            => Left(error)
    }
  }

  def fromFile(file: File): Either[String, Quizz] = {
    val source = Source.fromFile(file)
    try {
      val content                                        = source.mkString
      val value: Either[circe.Error, V3IdString.Mindmap] = Parser.parseInput(file.getName, content)
      value
        .map(_.toQuizz)
        .map(_.copy(id = file.getName))
        .left
        .map(e => e.getMessage)
    } catch {
      case e: Exception => Left(e.getMessage)
    } finally source.close()

  }
}
