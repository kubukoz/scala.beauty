package scalabeauty.backend

import cats.effect.kernel.Resource
import cats.effect.IO
import cats.syntax.all.*
import com.dimafeng.testcontainers.PostgreSQLContainer
import natchez.Trace.Implicits.noop
import org.testcontainers.utility.DockerImageName
import skunk.syntax.all.*
import skunk.Session
import weaver.*

import scala.concurrent.duration.*

object SnippetRepositoryIntegrationTests extends SimpleIOSuite with SnippetRepositoryTests {
  private val containerDef = PostgreSQLContainer.Def(
    dockerImageName = DockerImageName.parse("postgres:16-alpine"),
    databaseName = "postgres",
    username = "postgres",
    password = "postgres",
  )

  private def containerToPool(container: PostgreSQLContainer) =
    Session
      .pooled(
        host = container.containerIpAddress,
        port = container.mappedPort(org.testcontainers.containers.PostgreSQLContainer.POSTGRESQL_PORT),
        user = container.username,
        database = container.databaseName,
        password = container.password.some,
        max = 1,
      )

  // a bit hack-ish, apparently just starting the container doesn't wait for it to start responding meaningfully.
  // not a great workaround, but it works for now ;)
  private def newContainerPool = Resource
    .fromAutoCloseable(IO.blocking(containerDef.start()))
    .flatMap(containerToPool)
    .evalTap { pool =>
      fs2.Stream
        .retry(
          pool.use(_.execute(sql"select 1".query(skunk.codec.all.int4))),
          100.millis,
          _ * 2,
          maxAttempts = 7,
        )
        .compile
        .drain
    }

  val makeRepo: Resource[IO, SnippetRepository] =
    newContainerPool.map(SnippetRepository.instance).evalTap(_.createTable())
}
