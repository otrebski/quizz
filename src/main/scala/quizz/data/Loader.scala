package quizz.data

import java.io.{File, FileFilter}

import cats.syntax.either._
import com.typesafe.scalalogging.LazyLogging
import mindmup.{Parser, V3IdString}
import quizz.model.Quizz

import scala.io.{BufferedSource, Source}

object Loader extends LazyLogging {

  //TODO IO[]
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

  //TODO IO[]
  def fromFile(file: File): Either[String, Quizz] = {
    val source: BufferedSource = Source.fromFile(file)
    try {
      val content: String                           = source.mkString
      val value: Either[String, V3IdString.Mindmap] = Parser.parseInput(file.getName, content)
      value
        .flatMap(_.toQuizz)
        .map(_.copy(id = file.getName))
    } catch {
      case e: Exception => Left(e.getMessage)
    } finally source.close()

  }
}
