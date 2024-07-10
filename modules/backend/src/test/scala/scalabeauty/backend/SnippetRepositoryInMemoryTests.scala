package scalabeauty.backend

import cats.effect.kernel.Resource
import cats.effect.IO

object SnippetRepositoryInMemoryTests extends SnippetRepositoryTests {
  val makeRepo: Resource[IO, SnippetRepository] = SnippetRepository.inMemory.toResource
}
