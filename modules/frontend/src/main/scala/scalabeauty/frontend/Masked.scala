package scalabeauty.frontend

import cats.effect.IO
import tyrian.Sub

import scala.concurrent.duration.*

object Masked {
  enum Model {
    case Pending
    case Fetched(maskSize: Int)
  }

  def subscriptions[Msg](model: Model, id: String)(onShorten: Msg, onUpdate: => Msg): Sub[IO, Msg] = {
    val updatePlaceholder = Sub.every[IO](1.second / 60, id + "-update").map(_ => onUpdate)

    model.match {
      case Model.Pending => updatePlaceholder

      case Model.Fetched(maskSize) if maskSize > 0 =>
        updatePlaceholder |+| Sub.every[IO](1.second / 20, id + "-shorten").map(_ => onShorten)

      case Model.Fetched(_) =>
        Sub.None
    }
  }

}
