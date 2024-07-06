package scalabeauty.frontend.test

import scalabeauty.frontend.Pagination
import scalabeauty.frontend.Pagination.Block
import weaver.*

object PaginationTests extends SimpleIOSuite {
  import Pagination.Block.*

  private def paginationTest(current: Long, total: Long)(expected: List[Block]): Unit =
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
      PageNumber(0).asCurrent
    )
  )

  paginationTest(0, 2)(
    List(
      PageNumber(0).asCurrent,
      PageNumber(1),
    )
  )

  paginationTest(0, 3)(
    List(
      PageNumber(0).asCurrent,
      PageNumber(1),
      PageNumber(2),
    )
  )

  paginationTest(0, 4)(
    List(
      PageNumber(0).asCurrent,
      PageNumber(1),
      Ellipsis,
      PageNumber(3),
    )
  )

  paginationTest(0, 10)(
    List(
      PageNumber(0).asCurrent,
      PageNumber(1),
      Ellipsis,
      PageNumber(9),
    )
  )

  paginationTest(1, 10)(
    List(
      PageNumber(0),
      PageNumber(1).asCurrent,
      PageNumber(2),
      Ellipsis,
      PageNumber(9),
    )
  )

  paginationTest(2, 10)(
    List(
      PageNumber(0),
      PageNumber(1),
      PageNumber(2).asCurrent,
      PageNumber(3),
      Ellipsis,
      PageNumber(9),
    )
  )

  paginationTest(3, 10)(
    List(
      PageNumber(0),
      Ellipsis,
      PageNumber(2),
      PageNumber(3).asCurrent,
      PageNumber(4),
      Ellipsis,
      PageNumber(9),
    )
  )

  paginationTest(7, 10)(
    List(
      PageNumber(0),
      Ellipsis,
      PageNumber(6),
      PageNumber(7).asCurrent,
      PageNumber(8),
      PageNumber(9),
    )
  )

  paginationTest(8, 10)(
    List(
      PageNumber(0),
      Ellipsis,
      PageNumber(7),
      PageNumber(8).asCurrent,
      PageNumber(9),
    )
  )

  paginationTest(9, 10)(
    List(
      PageNumber(0),
      Ellipsis,
      PageNumber(8),
      PageNumber(9).asCurrent,
    )
  )
}
