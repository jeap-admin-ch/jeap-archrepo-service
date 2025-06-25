create table system_component_database_schema
(
    id                  uuid,
    system_id           uuid                        not null references system(id),
    system_component_id uuid                        not null references system_component(id),
    schema              bytea,
    schema_version      varchar                     not null,
    created_at          timestamp with time zone    not null,
    modified_at         timestamp with time zone,
    PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uq_system_component_database_schema_system_component_id ON system_component_database_schema(system_component_id);
CREATE INDEX idx_system_component_database_schema_system_id ON system_component_database_schema(system_id);
