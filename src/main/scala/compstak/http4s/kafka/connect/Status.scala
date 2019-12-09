package compstak.http4s.kafka.connect

import cats.implicits._
import io.circe.Decoder
import io.circe.DecodingFailure

sealed abstract class Status(val asString: String)

object Status {
  final case object Unassigned extends Status("UNASSIGNED")
  final case object Running extends Status("RUNNING")
  final case object Paused extends Status("PAUSED")
  final case object Failed extends Status("FAILED")

  def all: Set[Status] = Set(Unassigned, Running, Paused, Failed)

  def fromString(s: String): Option[Status] = all.find(_.asString === s)

  implicit val decoder: Decoder[Status] =
    Decoder[String].emap(s => fromString(s).toRight(s"Can't decode $s as Status"))
}
