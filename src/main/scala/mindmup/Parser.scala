package mindmup

import com.typesafe.scalalogging.LazyLogging
import io.circe.generic.semiauto._
import io.circe.parser._
import io.circe.{ Decoder, _ }

object Parser extends LazyLogging {

  private implicit val pDecoder: Decoder[ParentConnector]    = deriveDecoder[ParentConnector]
  private implicit val attrDecoder: Decoder[Attr]            = deriveDecoder[Attr]
  private implicit val ideaDecoder: Decoder[V3IdString.Idea] = deriveDecoder[V3IdString.Idea]
  private implicit val mindMupDecoder: Decoder[V3IdString.Mindmap] =
    deriveDecoder[V3IdString.Mindmap]

  private implicit val ideaDecoderInt: Decoder[V3IdInt.Idea]       = deriveDecoder[V3IdInt.Idea]
  private implicit val mindMupDecoderInt: Decoder[V3IdInt.Mindmap] = deriveDecoder[V3IdInt.Mindmap]

  def parseInput(json: String): Either[Error, V3IdString.Mindmap] = {
    val parsedJson                           = parse(json)
    val r: Either[Error, V3IdString.Mindmap] = parsedJson.flatMap(mindMupDecoder.decodeJson)
    r match {
      case Left(e) =>
        logger.debug(s"Error $e, will do fallback")
        parsedJson.flatMap(mindMupDecoderInt.decodeJson).map(_.toV3IdString)
      case Right(s) => Right(s)
    }

  }

}
