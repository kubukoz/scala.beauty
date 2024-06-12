package scalabeauty.backend

import cats.effect.kernel.Resource
import cats.effect.IO
import cats.effect.IOApp
import cats.syntax.all.*
import natchez.Trace
import org.http4s.ember.server.EmberServerBuilder
import scalabeauty.api.*
import skunk.Session
import smithy4s.http4s.SimpleRestJsonBuilder
import smithy4s.Timestamp

import scala.util.Random

object BackendMain extends IOApp.Simple {

  def run: IO[Unit] = {

    given Trace[IO] = Trace.Implicits.noop

    for {
      sessionPool <- Session.pooled[IO](
        host = "localhost",
        port = 5432,
        user = "postgres",
        database = "postgres",
        password = Some("postgres"),
        max = 10,
      )
      repo = SnippetRepository.instance(sessionPool)

      _ <- initStates(repo).toResource

      service = ScalaBeautyApiImpl.instance(repo)

      route <- SimpleRestJsonBuilder.routes(service).resource

      httpApp = (
        route <+>
          smithy4s.http4s.swagger.docs[IO](ScalaBeautyApi)
      ).orNotFound

      server <- EmberServerBuilder
        .default[IO]
        .withHttpApp(httpApp)
        .withErrorHandler { case e =>
          cats.effect.std.Console[IO].printStackTrace(e) *>
            IO.raiseError(e)
        }
        .build
      _ <- IO.println(show"Server running at ${server.baseUri}").toResource
      _ <- IO.println(show"SwaggerUI running at ${server.baseUri / "docs"}").toResource
    } yield ()
  }.useForever

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
