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

  paginationTest(1, 1)(
    List(
      PageNumber(1).asCurrent
    )
  )

  paginationTest(1, 2)(
    List(
      PageNumber(1).asCurrent,
      PageNumber(2),
    )
  )

  paginationTest(1, 3)(
    List(
      PageNumber(1).asCurrent,
      PageNumber(2),
      PageNumber(3),
    )
  )

  paginationTest(1, 4)(
    List(
      PageNumber(1).asCurrent,
      PageNumber(2),
      Ellipsis,
      PageNumber(4),
    )
  )

  paginationTest(1, 10)(
    List(
      PageNumber(1).asCurrent,
      PageNumber(2),
      Ellipsis,
      PageNumber(10),
    )
  )

  paginationTest(2, 10)(
    List(
      PageNumber(1),
      PageNumber(2).asCurrent,
      PageNumber(3),
      Ellipsis,
      PageNumber(10),
    )
  )

  paginationTest(3, 10)(
    List(
      PageNumber(1),
      PageNumber(2),
      PageNumber(3).asCurrent,
      PageNumber(4),
      Ellipsis,
      PageNumber(10),
    )
  )

  paginationTest(4, 10)(
    List(
      PageNumber(1),
      Ellipsis,
      PageNumber(3),
      PageNumber(4).asCurrent,
      PageNumber(5),
      Ellipsis,
      PageNumber(10),
    )
  )

  paginationTest(8, 10)(
    List(
      PageNumber(1),
      Ellipsis,
      PageNumber(7),
      PageNumber(8).asCurrent,
      PageNumber(9),
      PageNumber(10),
    )
  )

  paginationTest(9, 10)(
    List(
      PageNumber(1),
      Ellipsis,
      PageNumber(8),
      PageNumber(9).asCurrent,
      PageNumber(10),
    )
  )

  paginationTest(10, 10)(
    List(
      PageNumber(1),
      Ellipsis,
      PageNumber(9),
      PageNumber(10).asCurrent,
    )
  )
}
