package scalabeauty.backend

import cats.effect.kernel.Resource
import cats.effect.IO
import org.typelevel.twiddles.Iso
import scalabeauty.api.*
import skunk.Codec
import skunk.Session
import smithy4s.Bijection
import smithy4s.Timestamp

import java.time.Instant
import java.time.ZoneOffset

trait SnippetRepository {
  def createTable(): IO[Unit]
  def insert(snippets: List[Snippet]): IO[Unit]
  def getAll(before: Option[Slug], page: Option[Page]): IO[List[Snippet]]
  def get(id: Slug): IO[Option[Snippet]]
}

object SnippetRepository {
  def instance(getSession: Resource[IO, Session[IO]]): SnippetRepository = {

    import skunk.implicits.*

    object codecs {
      import skunk.codec.all.*
      import skunk.smithy4s.codec.all.*

      given [From, To](using b: Bijection[From, To]): Iso[From, To] = Iso.instance(b.to)(b.from)

      val instant: Codec[Instant] =
        timestamptz.imap(_.toInstant())(_.atOffset(ZoneOffset.UTC))

      val timestamp: Codec[Timestamp] =
        instant.imap(Timestamp.fromInstant)(_.toInstant)

      val slug: Codec[Slug] = text.to[Slug]

      val snippet: Codec[Snippet] = {
        slug *:
          text *:
          text *:
          jsonb[Author] *:
          timestamp
      }.to[Snippet]
    }

    new SnippetRepository {
      private val allFields = sql"""id, description, code, author, created_at"""

      def createTable(): IO[Unit] =
        getSession.use {
          _.execute(
            sql"""create table if not exists snippets (
            id text primary key,
            description text not null,
            code text not null,
            author jsonb not null,
            created_at timestamptz not null
          )""".command
              // todo: create an index for ordered querying by date?
          )
        }.void

      def insert(snippets: List[Snippet]): IO[Unit] =
        getSession.use {
          _.prepare(
            sql"insert into snippets ($allFields) values ${codecs.snippet.values.list(snippets)}".command
          )
            .flatMap(_.execute(snippets))
        }.void

      def getAll(before: Option[Slug], page: Option[Page]): IO[List[Snippet]] =
        if (before.nonEmpty || page.nonEmpty) IO.stub
        else
          getSession
            .use(_.execute(sql"""select $allFields from snippets order by created_at asc""".query(codecs.snippet)))

      def get(id: Slug): IO[Option[Snippet]] =
        getSession.use { ses =>
          ses
            .prepare(
              sql"""select $allFields from snippets where id = ${codecs.slug}"""
                .query(codecs.snippet)
            )
            .flatMap(_.option(id))
        }
    }
  }
}
