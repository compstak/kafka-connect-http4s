package compstak.http4s.kafka.connect

import io.circe.Decoder

final case class KafkaConnectTask(connector: String, task: Int)

object KafkaConnectTask {
  implicit val decoder: Decoder[KafkaConnectTask] =
    Decoder.forProduct2("connector", "task")(KafkaConnectTask.apply)
}
