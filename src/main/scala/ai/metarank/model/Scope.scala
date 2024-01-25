package ai.metarank.model

import ai.metarank.model.Event.{ItemEvent, RankingEvent}
import ai.metarank.model.Identifier.{ItemId, RankingId, SessionId, UserId}
import ai.metarank.model.ScopeType.{
  GlobalScopeType,
  ItemFieldScopeType,
  ItemScopeType,
  RankingFieldScopeType,
  RankingScopeType,
  SessionScopeType,
  UserScopeType
}
import io.circe.{Codec, Decoder, Encoder}

import scala.annotation.switch

sealed trait Scope extends {
  def asString: String
  def getType: ScopeType
}

object Scope {
  case class UserScope(user: UserId) extends Scope {
    override val hashCode: Int      = user.value.hashCode
    override val asString: String   = s"user=${user.value}"
    override val getType: ScopeType = UserScopeType
  }

  case class ItemScope(item: ItemId) extends Scope {
    override val hashCode: Int      = item.value.hashCode
    override val asString: String   = s"item=${item.value}"
    override val getType: ScopeType = ItemScopeType
  }

  case class RankingScope(item: RankingId) extends Scope {
    override val hashCode: Int      = item.value.hashCode
    override val asString: String   = s"ranking=${item.value}"
    override val getType: ScopeType = RankingScopeType
  }

  case class ItemFieldScope(fieldName: String, fieldValue: String) extends Scope {
    override val hashCode: Int      = fieldName.hashCode ^ fieldValue.hashCode
    override val asString: String   = s"field=$fieldName:$fieldValue"
    override val getType: ScopeType = ItemFieldScopeType(fieldName)
  }

  case class RankingFieldScope(fieldName: String, fieldValue: String, item: ItemId) extends Scope {
    override val hashCode: Int      = fieldName.hashCode ^ fieldValue.hashCode ^ item.value.hashCode
    override val asString: String   = s"irf=$fieldName:$fieldValue:${item.value}"
    override val getType: ScopeType = RankingFieldScopeType(fieldName)
  }

  case object GlobalScope extends Scope {
    override val hashCode: Int      = 20221223
    override val asString: String   = "global"
    override val getType: ScopeType = GlobalScopeType
  }

  case class SessionScope(session: SessionId) extends Scope {
    override val hashCode: Int      = session.value.hashCode
    override val asString: String   = s"session=${session.value}"
    override val getType: ScopeType = SessionScopeType
  }

  def fromString(str: String): Either[Throwable, Scope] = {
    str match {
      case "global" => Right(GlobalScope)
      case other =>
        val firstEq = other.indexOf('='.toInt)
        if (firstEq > 0) {
          val left  = other.substring(0, firstEq)
          val right = other.substring(firstEq + 1)
          (left: @switch) match {
            case "item"    => Right(ItemScope(ItemId(right)))
            case "session" => Right(SessionScope(SessionId(right)))
            case "ranking" => Right(RankingScope(RankingId(right)))
            case "user"    => Right(UserScope(UserId(right)))
            case "field" =>
              val tokens = right.split(':')
              if (tokens.length == 2) {
                Right(ItemFieldScope(tokens(0), tokens(1)))
              } else {
                Left(new IllegalArgumentException(s"cannot parse field scope value '$right'"))
              }
            case "irf" =>
              val tokens = right.split(':')
              if (tokens.length == 3) {
                Right(RankingFieldScope(tokens(0), tokens(1), ItemId(tokens(2))))
              } else {
                Left(new IllegalArgumentException(s"cannot parse item field scope value '$right'"))
              }
            case _ => Left(new IllegalArgumentException(s"cannot parse scope $other"))
          }
        } else {
          Left(new IllegalArgumentException(s"cannot parse scope $other"))
        }
    }
  }

  implicit val scopeDecoder: Decoder[Scope] = Decoder.decodeString.emapTry(str => Scope.fromString(str).toTry)
  implicit val scopeEncoder: Encoder[Scope] = Encoder.encodeString.contramap(_.asString)
  implicit val scopeCodec: Codec[Scope]     = Codec.from(scopeDecoder, scopeEncoder)

}
