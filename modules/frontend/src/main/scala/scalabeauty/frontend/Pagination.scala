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
  }

  object Block {
    def PageNumber(number: Long): PageNumber = PageNumber(number, current = false)

    extension (pn: PageNumber) def asCurrent: Block = pn.copy(current = true)
  }

  def getPaginationBlocks(p: api.Pagination): List[Block] =
    getPaginationBlocks(
      current = p.currentPage.value + 1,
      total = p.totalPages.value,
    )

  def getPaginationBlocks(current: Long, total: Long): List[Block] = {
    import Block._

    // first and last page always visible
    // current page and 1 page before / after always visible
    // ellipsis in between if there's a gap.
    // if total pages <= 5, all pages visible

    val firstPage = 1L
    val lastPage  = total

    val currentAndSurrounding = List.concat(
      if current > 2 then List(current - 1) else Nil,
      if current != 1L && current != total then List(current) else Nil,
      if current < (total - 1) then List(current + 1) else Nil,
    )

    val numbersToShow = (firstPage :: currentAndSurrounding ::: lastPage :: Nil).distinct

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
        case Ellipsis              => Ellipsis
      }
  }

  def view(blocks: List[Block]): Html[Nothing] =
    nav(
      className := "pagination block is-centered",
      role      := "navigation",
    )(
      ul(className := "pagination-list")(
        blocks.map {
          case Block.Ellipsis => li(span(className := "pagination-ellipsis")("…"))
          case Block.PageNumber(number, current) =>
            li(
              a(
                className := s"pagination-link ${if current then "is-current" else ""}",
                href      := s"?page=$number",
              )(number.toString)
            )
        }
      )
    )
}
