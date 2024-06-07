package scalabeauty.backend

import cats.effect.kernel.Resource
import cats.effect.IO
import org.typelevel.twiddles.Iso
import scalabeauty.api.*
import skunk.Codec
import skunk.Session
import smithy4s.Bijection

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

      val slug: Codec[Slug] = text.to[Slug]

      val snippet: Codec[Snippet] = {
        slug *:
          text *:
          text *:
          jsonb[Author]
      }.to[Snippet]
    }

    new SnippetRepository {
      private val allFields = sql"""id, description, code, author"""

      def createTable(): IO[Unit] =
        getSession.use {
          _.execute(
            sql"""create table if not exists snippets (
            id text primary key,
            description text not null,
            code text not null,
            author jsonb not null
          )""".command
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
            .use(_.execute(sql"""select $allFields from snippets""".query(codecs.snippet)))

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
