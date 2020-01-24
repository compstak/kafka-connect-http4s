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
  path: Path,
  blocker: Blocker
)(implicit val F: Sync[F]) {

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

  private[this] def listActiveConfigs: Stream[F, Path] =
    file
      .walk[F](blocker, path)
      .filter(_.toFile().getName().endsWith(".json"))

  private[this] def buildActiveConfigs: Stream[F, MigrationAction] =
    listActiveConfigs
      .evalMap(loadConfig)
      .evalMap {
        case (name, conf) =>
          conf.asObject match {
            case Some(obj) =>
              val isValidMap = obj.values.forall(_.isString)
              if (!isValidMap) F.raiseError[MigrationAction](new Throwable("Configuration values must be strings"))
              else {
                val finalMap = obj.toMap.mapValues(_.asString).collect {
                  case (s, Some(c)) => s -> c
                }
                F.pure[MigrationAction](Upsert(name, finalMap))
              }
            case None => F.raiseError[MigrationAction](new Throwable("Configuration must be a map"))
          }
      }

  private[this] def buildDeprecatedConfigs: F[List[MigrationAction]] =
    for {
      connectors <- client.connectorNames
      configs <- listActiveConfigs.compile.toList
    } yield connectors.filterNot(configs.map(_.getFileName.toString.replace(".json", "")).contains).map(Delete(_))

  private[this] def loadConfig(p: Path): F[(String, Json)] = {
    val content =
      file.readAll(p, blocker, 4096).through(utf8Decode).compile.lastOrError

    content.flatMap { js =>
      parse(js).map((p.getFileName.toString.replace(".json", ""), _)).liftTo[F]
    }
  }
}

object KafkaConnectMigration {

  def apply[F[_]: Sync: ContextShift](
    client: Client[F],
    uri: Uri,
    path: String = "/kafka/connect"
  ): Resource[F, KafkaConnectMigration[F]] =
    for {
      connect <- KafkaConnectClient[F](client, uri)
      p <- Resource.liftF(Sync[F].delay(Paths.get(getClass.getResource(path).toURI)))
    } yield new KafkaConnectMigration(connect, p, Blocker.liftExecutionContext(ExecutionContext.global))

  sealed trait MigrationAction

  case class Upsert(name: String, config: Map[String, String]) extends MigrationAction
  case class Delete(name: String) extends MigrationAction
}
