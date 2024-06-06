package scalabeauty.backend

import cats.effect.IO
import scalabeauty.api.*

import java.util.UUID
import scala.util.Random

object ScalaBeautyApiImpl {
  val instance: ScalaBeautyApi[IO] = new ScalaBeautyApi[IO] {
    private def mkSlug(len: Int) = Random.alphanumeric.take(len).mkString

    def getSnippets(before: Option[Slug], page: Option[Page]): IO[GetSnippetsOutput] =
      IO.println("Received request to get snippets") *>
        IO.pure(
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
  }
}
