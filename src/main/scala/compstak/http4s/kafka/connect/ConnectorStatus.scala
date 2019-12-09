package compstak.http4s.kafka.connect

import io.circe.Decoder

final case class ConnectorStatus(state: Status, workerId: String, trace: Option[String])

object ConnectorStatus {
  implicit val decoder: Decoder[ConnectorStatus] =
    Decoder.forProduct3("state", "worker_id", "trace")(ConnectorStatus.apply)
}
