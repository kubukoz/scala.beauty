package scalabeauty.frontend

import tyrian.*
import tyrian.Html.*

object HtmlUtils {
  def linkAttrs(url: String) = List(
    href := url,
    onClick(Msg.NewTab(url)),
  )
}
