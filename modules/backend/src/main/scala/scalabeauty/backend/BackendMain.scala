package scalabeauty.backend

import cats.effect.IO
import cats.effect.IOApp
import cats.syntax.all.*
import org.http4s.ember.server.EmberServerBuilder
import scalabeauty.api.*
import smithy4s.http4s.SimpleRestJsonBuilder

object BackendMain extends IOApp.Simple {

  def run: IO[Unit] = {
    val service = new ScalaBeautyApi[IO] {
      def getSnippets(before: Option[Slug], page: Option[Page]): IO[GetSnippetsOutput] =
        IO.println("Received request to get snippets") *>
          IO.pure(
            GetSnippetsOutput(
              List(
                Snippet(
                  id = Slug("my-snippet"),
                  author = Author.github(GithubAuthor(username = "kubukoz")),
                  description = "My snippet",
                  code = """def hello = println("foobar!")""",
                ),
                Snippet(
                  id = Slug("my-snippet-2"),
                  author = Author.github(GithubAuthor(username = "kubukoz")),
                  description = "My snippet 2",
                  code = """def hello = println("foobar but different!")""",
                ),
              )
            )
          )
    }

    for {
      route <- SimpleRestJsonBuilder.routes(service).resource

      httpApp = (
        route <+>
          smithy4s.http4s.swagger.docs[IO](ScalaBeautyApi)
      ).orNotFound

      server <- EmberServerBuilder
        .default[IO]
        .withHttpApp(httpApp)
        .build
      _ <- IO.println(show"Server running at ${server.baseUri}").toResource
      _ <- IO.println(show"SwaggerUI running at ${server.baseUri / "docs"}").toResource
    } yield ()
  }.useForever
}
