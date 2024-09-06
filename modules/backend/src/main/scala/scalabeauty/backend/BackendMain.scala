package scalabeauty.backend

import cats.data.NonEmptyList
import cats.effect.IO
import cats.effect.IOApp
import cats.syntax.all.*
import com.comcast.ip4s.*
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.HSTS
import org.http4s.HttpRoutes
import org.http4s.StaticFile
import scalabeauty.api.*
import smithy4s.http4s.SimpleRestJsonBuilder
import smithy4s.Timestamp

import scala.concurrent.duration.Duration
import scala.util.Random

object BackendMain extends IOApp.Simple {

  def run: IO[Unit] = {

    for {
      config <- AppConfig.read.load[IO].toResource
      _      <- IO.println(s"Loaded config: $config").toResource

      connectEc <- ExecutionContexts.fixedThreadPool[IO](size = 10)
      transactor <- HikariTransactor.newHikariTransactor[IO](
        classOf[org.postgresql.Driver].getName(),
        s"jdbc:postgresql://${config.db.host}:${config.db.port}/${config.db.database}",
        config.db.user,
        config.db.password.value,
        connectEc,
      )

      repo = SnippetRepository.doobieInstance(transactor)

      _ <- initStates(repo).toResource

      service = ScalaBeautyApiImpl.instance(repo, pageSize = 5)

      route <- SimpleRestJsonBuilder.routes(service).resource

      httpApp = NonEmptyList
        .of(
          route,
          smithy4s.http4s.swagger.docs[IO](ScalaBeautyApi),
          staticRoutes,
        )
        .reduceK
        .orNotFound

      server <- EmberServerBuilder
        .default[IO]
        .withHttpApp(HSTS(httpApp))
        .withHost(host"0.0.0.0")
        .withPort(config.http.port)
        .withErrorHandler { case e =>
          cats.effect.std.Console[IO].printStackTrace(e) *>
            IO.raiseError(e)
        }
        .withShutdownTimeout(Duration.Zero)
        .build
      _ <- IO.println(show"Server running at ${server.baseUri}").toResource
      _ <- IO.println(show"SwaggerUI running at ${server.baseUri / "docs"}").toResource
    } yield ()
  }.useForever

  import org.http4s.dsl.io.*

  private def staticRoutes = HttpRoutes.of[IO] {
    case req @ GET -> path if path.startsWith(Root / "assets") =>
      StaticFile
        .fromResource("frontend/" + path.renderString, Some(req))
        .getOrElseF(InternalServerError())

    case req =>
      // catch-all for the SPA
      StaticFile
        .fromResource("frontend/index.html", Some(req))
        .getOrElseF(InternalServerError())

  }

  private def mkSlug(len: Int) = Slug(Random.alphanumeric.take(len).mkString.toLowerCase())

  private def initStates(repo: SnippetRepository): IO[Unit] = {
    val values = List.fill(10) {
      Snippet(
        id = mkSlug(10),
        author = Author.github(GithubAuthor(username = "kubukoz")),
        description = "My snippet",
        code = """def hello = println("foobar!")""",
        createdAt = Timestamp.nowUTC(),
      )
    }

    repo.createTable() *>
      repo.insert(values)
  }.void
}
