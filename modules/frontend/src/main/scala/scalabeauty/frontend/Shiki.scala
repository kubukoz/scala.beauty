package scalabeauty.frontend

import scalabeauty.api
import scalabeauty.api.*
import tyrian.*

import scala.scalajs.js
import scala.scalajs.js.annotation.*

@js.native
@JSImport("shiki", JSImport.Namespace)
object Shiki extends js.Object {
  def codeToHtml(code: String, options: js.Dynamic): js.Promise[String] = js.native
}
