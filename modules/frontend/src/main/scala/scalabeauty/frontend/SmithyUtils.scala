package scalabeauty.frontend

import cats.effect.kernel.Async
import smithy4s.kinds.Kind1
import smithy4s.kinds.PolyFunction5
import smithy4s.Service

import scala.scalajs.js.Promise

object SmithyUtils {
  // todo note to smithy4s-fetch about proper suspension
  def suspendPromise[Alg[_[_, _, _, _, _]], F[_]: Async](
      impl: Alg[Kind1[Promise]#toKind5]
  )(using svc: Service[Alg]): svc.Impl[F] =
    svc.fromPolyFunction(
      new PolyFunction5[svc.Operation, Kind1[F]#toKind5] {
        private val underlying = svc.toPolyFunction(impl)
        def apply[I, E, O, SI, SO](fa: svc.Operation[I, E, O, SI, SO]): F[O] =
          Async[F].fromPromise(Async[F].delay(underlying(fa)))
      }
    )
}
