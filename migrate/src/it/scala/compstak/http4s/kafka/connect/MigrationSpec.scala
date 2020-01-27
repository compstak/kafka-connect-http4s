package compstak.http4s.kafka.connect

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.http4s.client.asynchttpclient.AsyncHttpClient
import org.http4s.Uri
import cats.implicits._
import cats.effect.{ContextShift, IO, Resource, Timer}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

class MigrationSpec extends AnyFunSuite with Matchers {

  implicit val ctx: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)

  test("After migration connector and config should exist") {

    AsyncHttpClient.resource[IO]()
      .flatMap(KafkaConnectMigration[IO](_, Uri.uri("http://localhost:18083"), "test/kafka/connect"))
      .flatMap(migration => Resource.liftF(
          migration.migrate *> 
            IO.sleep(10.seconds) *> 
            migration.client.connectorNames.map(_ shouldBe List("test"))
      ))
      .use(IO.pure)
      .unsafeRunSync()
  }
}
