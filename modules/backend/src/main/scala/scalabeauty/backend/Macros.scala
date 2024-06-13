package scalabeauty.backend

import cats.syntax.all.*

import java.nio.file.Path
import java.nio.file.Paths
import scala.quoted.*

object Macros {
  inline transparent def dbg(inline args: Any*): Any = ${ dbgImpl('args) }

  private def dbgImpl(argExpr: Expr[Seq[Any]])(using Quotes): Expr[Any] = {
    import quotes.reflect.*

    argExpr.asTerm match {
      case Inlined(_, _, Typed(Repeated(items, _), _)) =>
        val file = Expr(argExpr.asTerm.pos.sourceFile.path)
        val line = Expr(argExpr.asTerm.pos.startLine)

        val params = items.map(_.asExpr).map { arg =>
          val nameExpr = Expr(arg.show)

          '{ Param(value = ${ arg }, name = $nameExpr) }
        }

        val types = typeReprsToTuple(items.map(_.tpe))

        types.asType match {
          case '[r] =>
            '{ debugRuntime(${ Expr.ofList(params) }, Paths.get($file), $line).asInstanceOf[r] }.asExprOf[r]
        }

    }
  }

  private def typeReprsToTuple(using Quotes)(types: List[quotes.reflect.TypeRepr]): quotes.reflect.TypeRepr = {
    import quotes.reflect.*
    types.foldLeft(TypeRepr.of[EmptyTuple]) { (rest, item) =>
      TypeRepr.of[*:].appliedTo(List(item, rest))
    }
  }

  case class Param(value: Any, name: String)

  def debugRuntime(params: List[Param], file: Path, line: Int): Tuple = {
    val paramsText = params
      .map { p =>
        s"${Console.MAGENTA}${p.name}${Console.RESET}: ${Console.GREEN}${p.value}${Console.RESET}"
      }
      .mkString(", ")

    println(s"dbg@[${Console.CYAN}${file.getFileName().toString()}:$line${Console.RESET}]: $paramsText")
    Tuple.fromArray(params.map(_.value).toArray)
  }
}
