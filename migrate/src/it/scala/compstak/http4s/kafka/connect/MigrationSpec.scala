package compstak.http4s.kafka.connect

import cats.implicits._
import cats.effect.{ContextShift, IO, Resource, Timer}
import io.circe.literal._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.http4s.client.asynchttpclient.AsyncHttpClient
import org.http4s.Uri

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

class MigrationSpec extends AnyFunSuite with Matchers {

  implicit val ctx: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)

  test("After migration connector and config should exist") {

    AsyncHttpClient
      .resource[IO]()
      .flatMap(
        KafkaConnectMigration[IO](
          _,
          Uri.uri("http://localhost:18083"),
          Map(
            "test1" -> json"""
                        {
                          "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
                          "database.hostname": "database",
                          "database.port": "5432",
                          "database.user": "postgres",
                          "database.password": "",
                          "database.dbname" : "postgres",
                          "database.server.name": "test"
                        }
                        """
          ),
          "kafka-migration-test1"
        )
      )
      .flatMap(migration =>
        Resource.liftF(
          migration.migrate *>
            IO.sleep(10.seconds) *>
            migration.client.connectorNames.map(_ shouldBe List("test1"))
        )
      )
      .>>(
        AsyncHttpClient
          .resource[IO]()
          .flatMap(
            KafkaConnectMigration[IO](
              _,
              Uri.uri("http://localhost:18083"),
              Map(
                "test2" -> json"""
                            {
                              "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
                              "database.hostname": "database",
                              "database.port": "5432",
                              "database.user": "postgres",
                              "database.password": "",
                              "database.dbname" : "postgres",
                              "database.server.name": "test"
                            }
                            """
              ),
              "kafka-migration-test2"
            )
          )
      )
      .flatMap(migration =>
        Resource.liftF(
          migration.migrate *>
            IO.sleep(10.seconds) *>
            migration.client.connectorNames.map(_.sorted shouldBe List("test1", "test2"))
        )
      )
      .use(IO.pure)
      .unsafeRunSync()
  }
}
