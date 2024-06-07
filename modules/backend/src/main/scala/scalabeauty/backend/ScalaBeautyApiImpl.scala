package scalabeauty.backend

import cats.effect.IO
import scalabeauty.api.*

object ScalaBeautyApiImpl {
  def instance(repo: SnippetRepository): ScalaBeautyApi[IO] =
    new ScalaBeautyApi[IO] {

      def getSnippets(before: Option[Slug], page: Option[Page]): IO[GetSnippetsOutput] =
        if (before.nonEmpty || page.nonEmpty) IO.stub
        else
          IO.println("Received request to get snippets") *>
            repo
              .getAll()
              .map(GetSnippetsOutput(_))

      def getSnippet(id: Slug): IO[GetSnippetOutput] =
        IO.println("Received request to get snippet " + id.value) *>
          IO {
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
