package compstak.http4s.kafka.connect

import io.circe.Encoder

final case class CreateConnectorRequest(name: String, config: Map[String, String])

object CreateConnectorRequest {
  implicit val encoder: Encoder[CreateConnectorRequest] =
    Encoder.forProduct2("name", "config")(ccr => (ccr.name, ccr.config))
}
