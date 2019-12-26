package compstak.http4s.kafka.connect

import org.http4s.client.Client
import org.http4s.Request
import org.http4s.Method
import org.http4s.Uri
import org.http4s.circe.CirceEntityCodec._
import cats.effect.Sync
import io.circe.Encoder
import io.circe.Decoder
import cats.effect.Resource

final class KafkaConnectClient[F[_]: Sync](client: Client[F], root: Uri) {

  def connectorNames: F[List[String]] =
    client.expect(
      Request[F](
        uri = root / "connectors"
      )
    )

  def createConnector(ccr: CreateConnectorRequest): F[Connector] =
    client.expect(
      Request[F](
        method = Method.POST,
        uri = root / "connectors"
      ).withEntity(ccr)
    )

  def connector(name: String): F[Connector] =
    client.expect(
      Request[F](
        uri = root / "connectors" / name
      )
    )

  def connectorConfig(name: String): F[Map[String, String]] =
    client.expect(
      Request[F](
        uri = root / "connectors" / name / "config"
      )
    )

  def upsertConnector(name: String, config: Map[String, String]): F[Connector] =
    client.expect(
      Request[F](
        method = Method.PUT,
        uri = root / "connectors" / name / "config"
      ).withEntity(config)
    )

  def connectorStatus(name: String): F[KafkaConnectStatus] =
    client.expect(
      Request[F](
        uri = root / "connectors" / name / "status"
      )
    )

  def restartConnector(name: String): F[Unit] =
    client.expect(
      Request[F](
        method = Method.POST,
        uri = root / "connectors" / name / "restart"
      )
    )

  def pauseConnector(name: String): F[Unit] =
    client.expect(
      Request[F](
        method = Method.PUT,
        uri = root / "connectors" / name / "pause"
      )
    )

  def resumeConnector(name: String): F[Unit] =
    client.expect(
      Request[F](
        method = Method.PUT,
        uri = root / "connectors" / name / "resume"
      )
    )

  def deleteConnector(name: String): F[Unit] =
    client.expect(
      Request[F](
        method = Method.DELETE,
        uri = root / "connectors" / name / "pause"
      )
    )

  def taskStatus(connectorName: String, taskId: Int): F[TaskStatus] =
    client.expect(
      Request[F](
        uri = root / "connectors" / connectorName / "tasks" / taskId.toString / "status"
      )
    )

  def restartTask(connectorName: String, taskId: Int): F[Unit] =
    client.expect(
      Request[F](
        method = Method.POST,
        uri = root / "connectors" / connectorName / "tasks" / taskId.toString / "restart"
      )
    )

}

object KafkaConnectClient {

  def apply[F[_]: Sync](client: Client[F], uri: Uri): Resource[F, KafkaConnectClient[F]] =
    Resource.pure(new KafkaConnectClient(client, uri))
}