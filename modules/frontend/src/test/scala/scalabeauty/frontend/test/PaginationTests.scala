package scalabeauty.frontend.test

import scalabeauty.frontend.Pagination
import scalabeauty.frontend.Pagination.Block
import weaver.*

object PaginationTests extends SimpleIOSuite {
  import Pagination.Block.*

  private def paginationTest(current: Long, total: Long)(expected: List[Block])(using SourceLocation): Unit =
    pureTest(s"$current. page of $total") {
      assert.same(
        expected,
        Pagination.getPaginationBlocks(
          current = current,
          total = total,
        ),
      )
    }

  paginationTest(0, 1)(
    List(
      Previous(None),
      PageNumber(0).asCurrent,
      Next(None),
    )
  )

  paginationTest(0, 2)(
    List(
      Previous(None),
      PageNumber(0).asCurrent,
      PageNumber(1),
      Next(Some(1)),
    )
  )

  paginationTest(0, 3)(
    List(
      Previous(None),
      PageNumber(0).asCurrent,
      PageNumber(1),
      PageNumber(2),
      Next(Some(1)),
    )
  )

  paginationTest(0, 4)(
    List(
      Previous(None),
      PageNumber(0).asCurrent,
      PageNumber(1),
      Ellipsis,
      PageNumber(3),
      Next(Some(1)),
    )
  )

  paginationTest(0, 10)(
    List(
      Previous(None),
      PageNumber(0).asCurrent,
      PageNumber(1),
      Ellipsis,
      PageNumber(9),
      Next(Some(1)),
    )
  )

  paginationTest(1, 10)(
    List(
      Previous(Some(0)),
      PageNumber(0),
      PageNumber(1).asCurrent,
      PageNumber(2),
      Ellipsis,
      PageNumber(9),
      Next(Some(2)),
    )
  )

  paginationTest(2, 10)(
    List(
      Previous(Some(1)),
      PageNumber(0),
      PageNumber(1),
      PageNumber(2).asCurrent,
      PageNumber(3),
      Ellipsis,
      PageNumber(9),
      Next(Some(3)),
    )
  )

  paginationTest(3, 10)(
    List(
      Previous(Some(2)),
      PageNumber(0),
      Ellipsis,
      PageNumber(2),
      PageNumber(3).asCurrent,
      PageNumber(4),
      Ellipsis,
      PageNumber(9),
      Next(Some(4)),
    )
  )

  paginationTest(7, 10)(
    List(
      Previous(Some(6)),
      PageNumber(0),
      Ellipsis,
      PageNumber(6),
      PageNumber(7).asCurrent,
      PageNumber(8),
      PageNumber(9),
      Next(Some(8)),
    )
  )

  paginationTest(8, 10)(
    List(
      Previous(Some(7)),
      PageNumber(0),
      Ellipsis,
      PageNumber(7),
      PageNumber(8).asCurrent,
      PageNumber(9),
      Next(Some(9)),
    )
  )

  paginationTest(9, 10)(
    List(
      Previous(Some(8)),
      PageNumber(0),
      Ellipsis,
      PageNumber(8),
      PageNumber(9).asCurrent,
      Next(None),
    )
  )
}
