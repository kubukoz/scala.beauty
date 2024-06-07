package scalabeauty.backend

import cats.effect.IO
import scalabeauty.api.*

import scala.util.Random

object ScalaBeautyApiImpl {
  val instance: ScalaBeautyApi[IO] = new ScalaBeautyApi[IO] {
    private def mkSlug(len: Int) = Random.alphanumeric.take(len).mkString.toLowerCase()

    def getSnippets(before: Option[Slug], page: Option[Page]): IO[GetSnippetsOutput] =
      if (before.nonEmpty || page.nonEmpty) IO.stub
      else
        IO.println("Received request to get snippets") *>
          IO(
            GetSnippetsOutput(
              List.fill(10)(
                Snippet(
                  id = Slug(mkSlug(10)),
                  author = Author.github(GithubAuthor(username = "kubukoz")),
                  description = "My snippet",
                  code = """def hello = println("foobar!")""",
                )
              )
            )
          )

    def getSnippet(id: Slug): IO[GetSnippetOutput] = IO {
      GetSnippetOutput {
        Snippet(
          id = id,
          author = Author.github(GithubAuthor(username = "kubukoz")),
          description = "This amazing snippet prints foobar to the console!",
          code = """def hello = println("foobar!")""",
        )
      }
    }
  }
}
