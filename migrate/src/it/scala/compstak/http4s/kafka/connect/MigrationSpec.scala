package compstak.http4s.kafka.connect

import cats.effect._
import cats.effect.unsafe.implicits.global
import cats.implicits._
import io.circe.literal._
import org.http4s.implicits._
import org.http4s.asynchttpclient.client.AsyncHttpClient
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration._

class MigrationSpec extends AnyFunSuite with Matchers {

  test("After migration connector and config should exist") {

    AsyncHttpClient
      .resource[IO]()
      .flatMap(
        KafkaConnectMigration[IO](
          _,
          uri"http://localhost:18083",
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
        Resource.eval(
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
              uri"http://localhost:18083",
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
        Resource.eval(
          migration.migrate *>
            IO.sleep(10.seconds) *>
            migration.client.connectorNames
        )
      )
      .use(names => IO(names.sorted shouldBe List("test1", "test2")))
      .unsafeRunSync()
  }
}
