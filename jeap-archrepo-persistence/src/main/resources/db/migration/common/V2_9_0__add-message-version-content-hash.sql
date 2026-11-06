-- Content hash of a stored message type version, over everything the docs API serves for it, so that the message
-- type index can carry each version's entity tag and a conditional request for a version can be answered without
-- reading its schemas.
--
-- Nullable and without a backfill on purpose: the hash is computed in Java over several columns with an encoding
-- the database does not know, and every model update removes and re-imports all message types, which writes the
-- hash. Until then a version without a hash keeps the entity tag over its serialized body, and the index carries no
-- tag for it.
alter table message_type_versions
    add column content_hash varchar(64);
