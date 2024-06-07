package scalabeauty.backend

import cats.effect.IO
import cats.syntax.all.*
import scalabeauty.api.*

object ScalaBeautyApiImpl {
  def instance(repo: SnippetRepository): ScalaBeautyApi[IO] =
    new ScalaBeautyApi[IO] {

      def getSnippets(before: Option[Slug], page: Option[Page]): IO[GetSnippetsOutput] =
        repo
          .getAll(before, page)
          .map(GetSnippetsOutput(_))

      def getSnippet(id: Slug): IO[GetSnippetOutput] =
        repo
          .get(id)
          .flatMap(_.liftTo[IO](SnippetNotFound()))
          .map(GetSnippetOutput(_))
    }
}
