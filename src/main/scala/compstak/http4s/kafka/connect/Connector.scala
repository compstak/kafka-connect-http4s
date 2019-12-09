package compstak.http4s.kafka.connect

import io.circe.Decoder

final case class Connector(name: String, config: Map[String, String], tasks: List[KafkaConnectTask])

object Connector {
  implicit val decoder: Decoder[Connector] = Decoder.forProduct3("name", "config", "tasks")(Connector.apply)
}
