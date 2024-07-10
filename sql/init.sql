create table if not exists snippets (
              id text primary key,
              description text not null,
              code text not null,
              author jsonb not null,
              created_at timestamptz not null
            );
create index if not exists snipets_created_at on snippets(created_at);
