package compstak.http4s.kafka.connect

import java.nio.file.{Files, FileSystems, Path, Paths}

import cats.effect._
import cats.implicits._

import compstak.http4s.kafka.connect.KafkaConnectMigration.{Delete, MigrationAction, Upsert}
import fs2.Stream
import fs2.io.readInputStream
import fs2.text.utf8Decode
import io.circe.Json
import io.circe.parser.parse
import org.http4s.Uri
import org.http4s.client.Client

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext

final class KafkaConnectMigration[F[_]: ContextShift](
  client: KafkaConnectClient[F],
  path: Path
)(implicit val F: Sync[F]) {

  def migrate: F[Unit] =
    buildDeprecatedConfigs
      .map(_ ++ buildActiveConfigs)
      .flatMap { actions =>
        actions
          .evalMap {
            case Delete(name)         => client.deleteConnector(name).void
            case Upsert(name, config) => client.upsertConnector(name, config).void
          }
          .compile
          .drain
      }

  private[this] def listActiveConfigs =
    Stream
      .fromIterator[F](Files.walk(path).iterator().asScala)
      .filter(_.endsWith(".json"))

  private[this] def buildActiveConfigs =
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

  private[this] def buildDeprecatedConfigs =
    client.connectorNames
      .map { connectors =>
        listActiveConfigs.map(_.getFileName.toString.replace(".json", "")).filter(!connectors.contains_(_))
      }
      .map(_.map(Delete(_)))

  private[this] def loadConfig(p: Path): F[(String, Json)] = {
    val content = readInputStream(
      Sync[F].delay(this.getClass.getResourceAsStream(p.toAbsolutePath.toString)),
      4096,
      Blocker.liftExecutionContext(ExecutionContext.global)
    ).through(utf8Decode).compile.lastOrError

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
      _ <- Resource.liftF( // todo remove this when classpath loading fixed in the library
        Sync[F].delay(
          FileSystems.newFileSystem(
            this.getClass.getResource("/kafka/connect").toURI,
            Map.empty[String, String].asJava
          )
        )
      )
      path = Paths.get(this.getClass.getResource("/kafka/connect").toURI)
    } yield new KafkaConnectMigration(connect, path)

  sealed trait MigrationAction

  case class Upsert(name: String, config: Map[String, String]) extends MigrationAction
  case class Delete(name: String) extends MigrationAction
}
