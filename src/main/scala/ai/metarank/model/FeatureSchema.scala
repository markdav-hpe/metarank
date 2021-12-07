package ai.metarank.model

import ai.metarank.feature.BooleanFeature.BooleanFeatureSchema
import ai.metarank.feature.InteractedWithFeature.InteractedWithSchema
import ai.metarank.feature.NumberFeature.NumberFeatureSchema
import ai.metarank.feature.RateFeature.RateFeatureSchema
import ai.metarank.feature.StringFeature.StringFeatureSchema
import ai.metarank.feature.WordCountFeature.WordCountSchema
import io.circe.{Codec, Decoder}

import scala.concurrent.duration.FiniteDuration

trait FeatureSchema {
  def name: String
  def refresh: Option[FiniteDuration]
  def ttl: Option[FiniteDuration]
  def scope: FeatureScope
}

object FeatureSchema {

  implicit val featureSchemaDecoder: Decoder[FeatureSchema] = Decoder.instance(c =>
    for {
      tpe <- c.downField("type").as[String]
      decoded <- tpe match {
        case "number"          => implicitly[Decoder[NumberFeatureSchema]].apply(c)
        case "boolean"         => implicitly[Decoder[BooleanFeatureSchema]].apply(c)
        case "string"          => implicitly[Decoder[StringFeatureSchema]].apply(c)
        case "word_count"      => implicitly[Decoder[WordCountSchema]].apply(c)
        case "rate"            => implicitly[Decoder[RateFeatureSchema]].apply(c)
        case "interacted_with" => implicitly[Decoder[InteractedWithSchema]].apply(c)
      }
    } yield {
      decoded
    }
  )
}