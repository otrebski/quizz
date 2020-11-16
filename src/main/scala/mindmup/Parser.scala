package mindmup

import com.typesafe.scalalogging.LazyLogging
import io.circe.generic.semiauto._
import io.circe.parser._
import io.circe.{ Decoder, _ }

object Parser extends LazyLogging {

  private implicit val pDecoder: Decoder[ParentConnector]    = deriveDecoder[ParentConnector]
  private implicit val noteDecoder: Decoder[Note]            = deriveDecoder[Note]
  private implicit val attrDecoder: Decoder[Attr]            = deriveDecoder[Attr]
  private implicit val ideaDecoder: Decoder[V3IdString.Idea] = deriveDecoder[V3IdString.Idea]
  private implicit val mindMupDecoder: Decoder[V3IdString.Mindmap] =
    deriveDecoder[V3IdString.Mindmap]

  private implicit val ideaDecoderInt: Decoder[V3IdInt.Idea]       = deriveDecoder[V3IdInt.Idea]
  private implicit val mindMupDecoderInt: Decoder[V3IdInt.Mindmap] = deriveDecoder[V3IdInt.Mindmap]

  def parseInput(name: String, json: String): Either[String, V3IdString.Mindmap] = {
    val parsedJson                           = parse(json)
    val r: Either[Error, V3IdString.Mindmap] = parsedJson.flatMap(mindMupDecoder.decodeJson)
    r match {
      case Left(e) =>
        logger.debug(
          s"Parsing of $name was not successful due to ${e.getMessage}, will try different parser"
        )
        parsedJson.flatMap(mindMupDecoderInt.decodeJson).map(_.toV3IdString) match {
          case Left(_) =>
            logger.info(
              s"""Parsing of $name again was not successful due to ${e.getMessage}, will try to replace all INT "id" with String type"""
            )
            val fixedJson = json.replaceAll("\"id\":\\s*(\\d+)", "\"id\": \"$1\"")
            val r         = parse(fixedJson).flatMap(mindMupDecoder.decodeJson)
            r match {
              case Left(_)  => logger.warn(s"Could not parse $name")
              case Right(_) => logger.info(s"$name parsed successfully after replacing INT id as STRING id")
            }
            r.left.map(_.getMessage)
          case Right(value) =>
            logger.info(s"$name parsed successfully")
            Right(value)
        }
      case Right(s) =>
        logger.info(s"$name parsed successfully")
        Right(s)
    }

  }

}
