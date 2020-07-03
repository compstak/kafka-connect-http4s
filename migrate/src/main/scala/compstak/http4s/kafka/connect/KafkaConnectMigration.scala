package compstak.http4s.kafka.connect

import java.nio.file.{FileSystems, Files, Path, Paths}
import cats.effect._
import cats.implicits._
import compstak.http4s.kafka.connect.KafkaConnectMigration.{Delete, MigrationAction, Upsert}
import fs2.Stream
import fs2.io.{file, readInputStream}
import fs2.text.utf8Decode
import io.circe.Json
import io.circe.parser.parse
import org.http4s.Uri
import org.http4s.client.Client
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext

final class KafkaConnectMigration[F[_]: ContextShift](
  val client: KafkaConnectClient[F],
  configs: Map[String, Json],
  service: String
)(implicit val F: Sync[F]) {

  val SERVICE_NAME = "service.name"

  def migrate: F[Unit] =
    Stream
      .evalSeq(buildDeprecatedConfigs)
      .append(buildActiveConfigs)
      .evalMap {
        case Delete(name)         => client.deleteConnector(name).void
        case Upsert(name, config) => client.upsertConnector(name, config).void
      }
      .compile
      .drain

  private[this] def buildActiveConfigs: Stream[F, MigrationAction] =
    Stream
      .fromIterator(configs.iterator)
      .evalMap {
        case (name, conf) =>
          conf.asObject match {
            case Some(obj) =>
              val isValidMap = obj.values.forall(_.isString)
              if (!isValidMap) F.raiseError[MigrationAction](new Throwable("Configuration values must be strings"))
              else {
                val finalMap = obj.toMap
                  .fmap(_.asString)
                  .collect {
                    case (s, Some(c)) => s -> c
                  }
                  .+(SERVICE_NAME -> service)
                F.pure[MigrationAction](Upsert(name, finalMap))
              }
            case None => F.raiseError[MigrationAction](new Throwable("Configuration must be a map"))
          }
      }

  private[this] def buildDeprecatedConfigs: F[List[MigrationAction]] =
    for {
      connectors <- client.connectorNames
      serviceConnectors <- connectors.filterA(name =>
        client.connectorConfig(name).map(_.get(SERVICE_NAME).forall(_ === service))
      )
    } yield serviceConnectors.filterNot(configs.map(_._1).toList.contains).map(Delete(_))
}

object KafkaConnectMigration {

  def apply[F[_]: Sync: ContextShift](
    client: Client[F],
    uri: Uri,
    configs: Map[String, Json],
    service: String
  ): Resource[F, KafkaConnectMigration[F]] =
    for {
      connect <- KafkaConnectClient[F](client, uri)
    } yield new KafkaConnectMigration(connect, configs, service)

  sealed trait MigrationAction

  case class Upsert(name: String, config: Map[String, String]) extends MigrationAction
  case class Delete(name: String) extends MigrationAction
}
