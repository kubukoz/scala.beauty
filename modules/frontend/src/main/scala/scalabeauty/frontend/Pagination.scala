package scalabeauty.frontend

import cats.data.Ior.Both
import cats.syntax.all.*
import scalabeauty.api
import scalabeauty.frontend.Pagination.Block.PageNumber
import tyrian.*
import tyrian.Html.*

object Pagination {

  enum Block {
    case Ellipsis
    case PageNumber(number: Long, current: Boolean)
    case Previous(page: Option[Long])
    case Next(page: Option[Long])
  }

  object Block {
    def PageNumber(number: Long): PageNumber = PageNumber(number, current = false)

    extension (pn: PageNumber) def asCurrent: Block = pn.copy(current = true)
  }

  def getPaginationBlocks(p: api.Pagination): List[Block] =
    getPaginationBlocks(
      current = p.currentPage.value,
      total = p.totalPages.value,
    )

  def getPaginationBlocks(current: Long, total: Long): List[Block] = {
    import Block._

    val firstPage    = 0L
    val previousPage = current - 1
    val nextPage     = current + 1
    val lastPage     = total - 1L

    // first and last page always visible
    // current page and 1 page before / after always visible
    // ellipsis in between if there's a gap.
    // if total pages <= 5, all pages visible
    val numbersToShow = List
      .concat(
        List(firstPage),
        List(previousPage).filter(_ >= 0),
        List(current).filterNot(_ == 0).filterNot(_ == total),
        List(nextPage).filter(_ < total),
        List(lastPage),
      )
      .distinct

    numbersToShow
      .align(numbersToShow.tail)
      .flatMap {
        case cats.data.Ior.Right(b)                         => PageNumber(b) :: Nil
        case Both(previous, next) if (previous + 1) == next => PageNumber(previous) :: Nil
        case Both(previous, next)                           => PageNumber(previous) :: Ellipsis :: Nil
        case cats.data.Ior.Left(a)                          => PageNumber(a) :: Nil
      }
      .map {
        case PageNumber(number, _) => PageNumber(number, current = number == current)
        case other                 => other
      }
      .prepended(Block.Previous(previousPage.some.filter(_ >= 0)))
      .appended(Block.Next(nextPage.some.filter(_ <= (total - 1))))
  }

  def view(blocks: List[Block]): Html[Nothing] =
    nav(
      className := "pagination block is-centered",
      role      := "navigation",
    )(
      ul(className := "pagination-list")(
        blocks.map {
          case Block.Previous(page: Option[Long]) =>
            page match {
              case Some(page) =>
                li(
                  a(
                    className := "pagination-previous is-family-monospace",
                    href      := s"?page=$page",
                  )("--")
                )
              case None =>
                li(
                  span(
                    className := "pagination-previous is-family-monospace is-disabled"
                  )("--")
                )
            }

          case Block.Next(page: Option[Long]) =>
            page match {
              case Some(page) =>
                li(
                  a(
                    className := "pagination-next is-family-monospace",
                    href      := s"?page=$page",
                  )("++")
                )
              case None =>
                li(
                  span(
                    className := "pagination-next is-family-monospace is-disabled"
                  )("++")
                )
            }

          case Block.Ellipsis => li(span(className := "pagination-ellipsis")("…"))
          case Block.PageNumber(number, current) if current =>
            li(
              span(
                className := "pagination-link is-current is-family-monospace"
              )(number.toString)
            )
          case Block.PageNumber(number, _) =>
            li(
              a(
                className := s"pagination-link is-family-monospace",
                href      := s"?page=$number",
              )(number.toString)
            )
        }
      )
    )
}
