package compstak.http4s.kafka.connect

import io.circe.Decoder

final case class TaskStatus(id: Int, state: Status, workerId: String, trace: Option[String])

object TaskStatus {
  implicit val decoder: Decoder[TaskStatus] = Decoder.forProduct4("id", "state", "worker_id", "trace")(TaskStatus.apply)
}
