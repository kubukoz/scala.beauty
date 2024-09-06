package scalabeauty.backend

import cats.effect.kernel.Resource
import cats.effect.IO
import com.dimafeng.testcontainers.PostgreSQLContainer
import doobie.implicits.*
import doobie.util.log.LogHandler
import doobie.util.transactor.Transactor
import org.testcontainers.utility.DockerImageName
import weaver.*

import scala.concurrent.duration.*

object SnippetRepositoryDoobieIntegrationTests extends SimpleIOSuite with SnippetRepositoryTests {
  private val containerDef = PostgreSQLContainer.Def(
    dockerImageName = DockerImageName.parse("postgres:16-alpine"),
    databaseName = "postgres",
    username = "postgres",
    password = "postgres",
  )

  private def containerToTransactor(container: PostgreSQLContainer) =
    Transactor.fromDriverManager[IO](
      driver = "org.postgresql.Driver",
      url = s"jdbc:postgresql://${container.containerIpAddress}:${container
          .mappedPort(org.testcontainers.containers.PostgreSQLContainer.POSTGRESQL_PORT)}/${container.databaseName}",
      user = container.username,
      password = container.password,
      logHandler = Some(LogHandler.jdkLogHandler),
    )

  // a bit hack-ish, apparently just starting the container doesn't wait for it to start responding meaningfully.
  // not a great workaround, but it works for now ;)
  private def newContainerPool = Resource
    .fromAutoCloseable(IO.blocking(containerDef.start()))
    .map(containerToTransactor)
    .evalTap { xa =>
      fs2.Stream
        .retry(
          sql"select 1".query[Int].unique.transact(xa),
          100.millis,
          _ * 2,
          maxAttempts = 7,
        )
        .compile
        .drain
    }

  val makeRepo: Resource[IO, SnippetRepository] =
    newContainerPool.map(SnippetRepository.doobieInstance(_)).evalTap(_.createTable())
}
