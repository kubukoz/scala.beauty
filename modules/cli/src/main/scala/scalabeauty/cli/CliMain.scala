package scalabeauty.cli

import cats.effect.kernel.Resource
import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.effect.MonadCancelThrow
import com.monovore.decline.Command
import com.monovore.decline.Opts
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.implicits.*
import org.http4s.Uri
import scalabeauty.api.ScalaBeautyApi
import scalabeauty.api.ScalaBeautyApiGen
import smithy4s.decline.util.PrinterApi
import smithy4s.decline.Entrypoint
import smithy4s.decline.Smithy4sCli
import smithy4s.http4s.SimpleRestJsonBuilder
import smithy4s.kinds.FunctorAlgebra
import smithy4s.kinds.Kind1
import smithy4s.kinds.PolyFunction5
import smithy4s.Service

object CliMain extends IOApp {
  private def mkClient(baseUri: Uri): Resource[IO, ScalaBeautyApi[IO]] = EmberClientBuilder
    .default[IO]
    .build
    .flatMap { client =>
      SimpleRestJsonBuilder(ScalaBeautyApi)
        .client(client)
        .uri(baseUri)
        .resource
    }

  private def unliftService[Alg[_[_, _, _, _, _]], F[_]: MonadCancelThrow](
      algRes: Resource[F, FunctorAlgebra[Alg, F]]
  )(implicit
      service: Service[Alg]
  ): FunctorAlgebra[Alg, F] = service.fromPolyFunction(
    new PolyFunction5[service.Operation, Kind1[F]#toKind5] {

      def apply[I, E, O, SI, SO](op: service.Operation[I, E, O, SI, SO]): F[O] =
        algRes.use(service.toPolyFunction(_)(op))

    }
  )

  def impl(baseUri: Uri) = unliftService(mkClient(baseUri))

  val mainOpts = Opts
    .option[String]("env", "Environment ")
    .map { case "prod" =>
      uri"https://www.scala.beauty"
    }
    .withDefault(uri"http://localhost:8080")
    .map(baseUri => Entrypoint(impl(baseUri), PrinterApi.std[IO]))

  def run(args: List[String]): IO[ExitCode] =
    Command("scalabeauty", "Scala Beauty CLI")(
      Smithy4sCli(mainOpts).opts
    )
      .parse(args)
      .fold(help => cats.effect.std.Console[IO].errorln(help).as(ExitCode.Error), _.as(ExitCode.Success))
}
