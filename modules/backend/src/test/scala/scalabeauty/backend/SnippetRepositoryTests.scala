package scalabeauty.backend

import cats.effect.kernel.Resource
import cats.effect.IO
import cats.syntax.all.*
import scalabeauty.api.Author
import scalabeauty.api.GithubAuthor
import scalabeauty.api.Slug
import scalabeauty.api.Snippet
import smithy4s.Timestamp
import weaver.*

trait SnippetRepositoryTests extends weaver.SimpleIOSuite {
  // make a raw, untouched repository
  def makeRepo: Resource[IO, SnippetRepository]

  private def sampleSnippet(number: Int) =
    Snippet(Slug("abcde" + number), "desc", "code", Author.github(GithubAuthor("kubukoz")), Timestamp.epoch)

  // make a repository that's usable for queries etc. (initialized)
  private def withRepository = makeRepo.evalTap(_.createTable())

  test("create table") {
    makeRepo.use(_.createTable()).as(success)
  }

  test("create table twice") {
    withRepository
      .use(_.createTable().replicateA(2))
      .as(success)
  }

  test("query without write") {
    withRepository.use { repo =>
      repo.get(Slug("nonexistent")).map(assert.same(none[Snippet], _))
    }
  }

  test("insert + query") {
    withRepository.use { repo =>
      val input = sampleSnippet(0)

      repo.insert(List(input)) *>
        repo
          .get(input.id)
          .map(assert.same(input.some, _))
    }
  }

  test("double insert overwrites") {
    withRepository.use { repo =>
      val input = sampleSnippet(0)

      // this would definitely benefit from some property-based testing
      val updatedInput = input.copy(
        description = "updated description",
        code = "updated code",
        author = Author.github(GithubAuthor("updated author")),
        createdAt = Timestamp.fromEpochSecond(10),
      )

      repo.insert(List(input)) *>
        repo.insert(List(updatedInput)) *>
        repo
          .get(input.id)
          .map(assert.same(updatedInput.some, _))
    }
  }

  test("insert + list (ordered by creation date, from newest to oldest)") {
    withRepository.use { repo =>
      val inputs = List(
        sampleSnippet(1).copy(createdAt = Timestamp.fromEpochSecond(1)),
        sampleSnippet(2).copy(createdAt = Timestamp.fromEpochSecond(2)),
        sampleSnippet(0).copy(createdAt = Timestamp.fromEpochSecond(0)),
      )

      repo.insert(inputs) *>
        repo
          .getAll(offset = 0L, limit = 10)
          .map(result =>
            assert.same(
              inputs.sortBy(_.createdAt.toInstant).reverse,
              result,
            )
          )
    }
  }

  test("insert + count") {
    withRepository.use { repo =>
      val inputs = List(sampleSnippet(0), sampleSnippet(1))

      repo.insert(inputs) *>
        repo
          .countAll()
          .map(assert.same(inputs.size, _))
    }
  }

}
