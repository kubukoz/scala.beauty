package skunk.smithy4s.codec

import cats.syntax.all._
import skunk.data.Type
import skunk.Codec
import smithy4s.json.Json
import smithy4s.schema.Schema
import smithy4s.Blob

trait JsonCodecs {

  private def genCodec[A](tpe: Type)(using
      schema: Schema[A]
  ): Codec[A] =
    Codec.simple(
      a => Json.writeBlob(a).toUTF8String,
      s => Json.read[A](Blob(s)).leftMap(_.getMessage()),
      tpe,
    )

  /** Construct a codec for `A`, coded as Json, mapped to the `json` schema type. */
  def json[A: Schema]: Codec[A] = genCodec[A](Type.json)

  /** Construct a codec for `A`, coded as Json, mapped to the `jsonb` schema type. */
  def jsonb[A: Schema]: Codec[A] = genCodec[A](Type.jsonb)
}

object json extends JsonCodecs

object all extends JsonCodecs
