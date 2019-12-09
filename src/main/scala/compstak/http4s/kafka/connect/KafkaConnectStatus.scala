package compstak.http4s.kafka.connect

import io.circe.Decoder

final case class KafkaConnectStatus(name: String, connector: ConnectorStatus, tasks: List[TaskStatus])

object KafkaConnectStatus {
  implicit val decoder: Decoder[KafkaConnectStatus] =
    Decoder.forProduct3("name", "connector", "tasks")(KafkaConnectStatus.apply)
}
