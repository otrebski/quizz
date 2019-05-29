package quizz.data

import java.io.{File, FileFilter}

import scala.io.Source

import cats.syntax.either._
import io.circe
import mindmup.{Parser, V3IdString}
import quizz.model.Quizz

object Loader {

  def fromFolder(folder: File): Either[String, List[Quizz]] = {
    val files = folder.listFiles(new FileFilter {
      override def accept(pathname: File): Boolean =
        pathname.isFile && pathname.getName.endsWith("mindmup.json")
    })

    val a: Seq[Either[String, Quizz]] = files.map(fromFile).toList
    a.foldLeft(List.empty[Quizz].asRight[String]) {
      case (Right(list), Right(quizz)) => Right(quizz :: list)
      case (Left(error), _)            => Left(error)
      case (_, Left(error))            => Left(error)
    }
  }

  def fromFile(file: File): Either[String, Quizz] = {
    val source = Source.fromFile(file)
    try {
      val content                                        = source.mkString
      val value: Either[circe.Error, V3IdString.Mindmap] = Parser.parseInput(content)
      value
        .map(_.toQuizz)
        .left
        .map(e => e.getMessage)
    } catch {
      case e: Exception => Left(e.getMessage)
    } finally {
      source.close()
    }

  }
}
