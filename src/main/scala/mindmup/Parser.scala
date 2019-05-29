package mindmup

import io.circe.generic.semiauto._
import io.circe.parser._
import io.circe.{ Decoder, _ }

object Parser {

  private implicit val pDecoder: Decoder[ParentConnector] = deriveDecoder[ParentConnector]
  private implicit val attrDecoder: Decoder[Attr]         = deriveDecoder[Attr]
  private implicit val ideaDecoder: Decoder[Idea]         = deriveDecoder[Idea]
  private implicit val mindMupDecoder: Decoder[Mindmap]   = deriveDecoder[Mindmap]

  def parseInput(json: String): Either[Error, Mindmap] =
    parse(json).flatMap(mindMupDecoder.decodeJson)

}
