package scalabeauty.backend

import cats.effect.kernel.Resource
import cats.effect.IO
import cats.syntax.all.*
import com.dimafeng.testcontainers.PostgreSQLContainer
import natchez.Trace.Implicits.noop
import org.testcontainers.utility.DockerImageName
import scalabeauty.api.Author
import scalabeauty.api.GithubAuthor
import scalabeauty.api.Slug
import scalabeauty.api.Snippet
import skunk.syntax.all.*
import skunk.Session
import smithy4s.Timestamp
import weaver.*

import scala.concurrent.duration.*

object SnippetRepositoryIntegrationTests extends SimpleIOSuite {
  private def sampleSnippet(number: Int) =
    Snippet(Slug("abcde" + number), "desc", "code", Author.github(GithubAuthor("kubukoz")), Timestamp.epoch)

  val containerDef = PostgreSQLContainer.Def(
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

  private val newRepository = newContainerPool.map(SnippetRepository.instance).evalTap(_.createTable())

  test("create table") {
    newRepository.use_.as(success)
  }

  test("insert and query") {
    newRepository.use { repo =>
      val input = sampleSnippet(0)

      repo.insert(List(input)) *>
        repo
          .get(input.id)
          .map(assert.same(input.some, _))
    }
  }

  test("insert and list") {
    newRepository.use { repo =>
      val inputs = List(sampleSnippet(0), sampleSnippet(1))

      repo.insert(inputs) *>
        repo
          .getAll(offset = 0L, limit = 10)
          .map(result => assert(result.sameElements(inputs)))
    }
  }

  test("insert and count") {
    newRepository.use { repo =>
      val inputs = List(sampleSnippet(0), sampleSnippet(1))

      repo.insert(inputs) *>
        repo
          .countAll()
          .map(assert.same(inputs.size, _))
    }
  }

}
