package ai.metarank.config

import ai.metarank.config.Config.InteractionConfig
import ai.metarank.model.{FeatureSchema, FieldSchema}
import ai.metarank.util.Logging
import better.files.File
import cats.effect.IO
import io.circe.Decoder
import io.circe.generic.semiauto._
import io.circe.yaml.parser.{parse => parseYaml}

case class Config(
    features: List[FeatureSchema],
    interactions: List[InteractionConfig]
)

object Config extends Logging {
  case class InteractionConfig(name: String, weight: Double)
  implicit val intDecoder: Decoder[InteractionConfig] = deriveDecoder
  implicit val configDecoder: Decoder[Config]         = deriveDecoder

  def load(path: String): IO[Config] = for {
    contents <- IO(File(path).contentAsString)
    yaml     <- IO.fromEither(parseYaml(contents))
    decoded  <- IO.fromEither(yaml.as[Config])
    _        <- IO(logger.info(s"loaded config file from $path"))
    _        <- IO(logger.info(s"features: ${decoded.features.map(_.name)}"))
  } yield {
    decoded
  }
}