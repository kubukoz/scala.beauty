package scalabeauty.frontend

import scala.scalajs.js
import scala.scalajs.js.annotation.*

@js.native
@JSImport("shiki", JSImport.Namespace)
object Shiki extends js.Object {
  def codeToHtml(code: String, options: js.Dynamic): js.Promise[String] = js.native
}
