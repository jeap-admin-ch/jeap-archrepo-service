ALTER TABLE
    system_graph
    ADD COLUMN last_published_fingerprint TEXT;

ALTER TABLE component_graph
    ADD COLUMN last_published_fingerprint TEXT;