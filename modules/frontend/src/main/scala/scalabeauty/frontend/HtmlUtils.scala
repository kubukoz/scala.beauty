package scalabeauty.frontend

import tyrian.*
import tyrian.Html.*

object HtmlUtils {
  def heading[Msg](items: Elem[Msg]*): Html[Msg] =
    h1(
      className := "title"
    )(items.toList)

  def subtitle[Msg](items: Elem[Msg]*): Html[Msg] =
    p(
      className := "subtitle"
    )(items.toList)

  def linkAttrs(url: String): List[Attr[Msg]] = List(
    href := url,
    onClick(Msg.NewTab(url)),
  )
}
