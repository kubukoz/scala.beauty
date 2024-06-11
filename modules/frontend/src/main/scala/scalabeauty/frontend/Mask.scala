package scalabeauty.frontend
import cats.effect.IO
import scalabeauty.api.Slug
import tyrian.Sub

import scala.concurrent.duration.*

object Mask {
  enum Model {
    case Pending
    case Fetched(maskSize: Int)
  }

  def subscriptions[Msg](model: Model)(onShorten: Msg, onUpdate: => Msg): Sub[IO, Msg] = {
    val updatePlaceholder = Sub.every[IO](1.second / 60).map(_ => onUpdate)

    model.match {
      case Model.Pending => updatePlaceholder

      case Model.Fetched(maskSize) if maskSize > 0 =>
        updatePlaceholder |+| Sub.every[IO](1.second / 20).map(_ => onShorten)

      case Model.Fetched(_) =>
        Sub.None
    }
  }

}
