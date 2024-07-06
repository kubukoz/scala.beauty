package scalabeauty.backend

import cats.effect.IO
import cats.syntax.all.*
import scalabeauty.api.*

object ScalaBeautyApiImpl {
  def instance(repo: SnippetRepository, pageSize: Int): ScalaBeautyApi[IO] =
    new ScalaBeautyApi[IO] {

      def getSnippets(page: Option[Page]): IO[GetSnippetsOutput] =
        (
          repo.getAll(offset = page.getOrElse(Page(0)).value, limit = pageSize),
          repo.countAll().map(calcPageCount(pageSize, _)),
        ).parMapN { (snippets, pageCount) =>
          GetSnippetsOutput(
            snippets,
            Pagination(
              currentPage = page.getOrElse(Page(0)),
              totalPages = Page(pageCount),
            ),
          )
        }

      private def calcPageCount(pageSize: Long, itemCount: Long): Long =
        (itemCount.toDouble / pageSize).ceil.toLong

      def getSnippet(id: Slug): IO[GetSnippetOutput] =
        repo
          .get(id)
          .flatMap(_.liftTo[IO](SnippetNotFound()))
          .map(GetSnippetOutput(_))
    }
}
