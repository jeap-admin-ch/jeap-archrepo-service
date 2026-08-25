-- Content hash of the stored OpenAPI specs and database schemas, so that the docs API can serve an ETag and answer
-- a conditional GET without reading the blob.
--
-- Nullable and without a backfill on purpose: computing SHA-256 in SQL is not portable across the databases the
-- migrations run on. Rows that predate this column keep a null hash and are backfilled lazily the first time the
-- artifact is read.
alter table open_api_spec
    add column content_hash varchar(64);

alter table system_component_database_schema
    add column content_hash varchar(64);
