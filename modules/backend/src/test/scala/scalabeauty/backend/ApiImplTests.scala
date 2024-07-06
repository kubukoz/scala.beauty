package scalabeauty.backend

import cats.effect.IO
import scalabeauty.api.GetSnippetOutput
import scalabeauty.api.GetSnippetsOutput
import scalabeauty.api.Page
import scalabeauty.api.Pagination
import scalabeauty.api.ScalaBeautyApi
import scalabeauty.api.SnippetNotFound
import weaver.Expectations
import weaver.SimpleIOSuite
import weaver.TestName

object ApiImplTests extends SimpleIOSuite {
  private val pageSize = 10

  private def sampleSnippet(number: Int) =
    scalabeauty.api.Snippet(
      scalabeauty.api.Slug("abcde" + number),
      "desc",
      "code",
      scalabeauty.api.Author.github(scalabeauty.api.GithubAuthor("kubukoz")),
      smithy4s.Timestamp.epoch,
    )

  apiTest("getSnippet on an existing snippet") {
    val input = sampleSnippet(0)
    repo.insert(List(input)) *>
      api.getSnippet(input.id).map {
        expect.same(GetSnippetOutput(input), _)
      }
  }

  apiTest("getSnippet on a nonexistent snippet") {
    val input = sampleSnippet(0)
    api.getSnippet(input.id).attempt.map {
      matches(_) { case Left(e) =>
        assert.same(SnippetNotFound(), e)
      }
    }
  }

  apiTest("query: empty list") {
    val anyPage = None

    api.getSnippets(page = anyPage).map {
      expect.same(
        GetSnippetsOutput(
          snippets = List.empty,
          pagination = Pagination(Page(0), Page(0)),
        ),
        _,
      )
    }
  }

  apiTest("query: default page") {
    val items = List.tabulate(20)(sampleSnippet)

    repo.insert(items) *>
      api.getSnippets(page = None).map { result =>
        expect(result.snippets.size == pageSize) &&
        expect.same(
          Pagination(currentPage = Page(0), totalPages = Page(2)),
          result.pagination,
        )
      }
  }

  private def apiTest(name: TestName)(f: (SnippetRepository, ScalaBeautyApi[IO]) ?=> IO[Expectations]) =
    test(name) {
      SnippetRepository.inMemory.flatMap { repo =>
        val api = ScalaBeautyApiImpl.instance(repo, pageSize)
        f(using repo, api)
      }
    }

  private def repo(using SnippetRepository) = summon[SnippetRepository]
  private def api(using ScalaBeautyApi[IO]) = ScalaBeautyApi[IO]
}
