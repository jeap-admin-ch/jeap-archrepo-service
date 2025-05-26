CREATE TABLE reaction_statistics
(
    id           UUID,
    component_id  UUID                     not null references system_component (id),
    trigger_type VARCHAR(64),
    trigger_fqn  VARCHAR(1024),
    action_type  VARCHAR(64),
    action_fqn   VARCHAR(1024),
    count        INT                      NOT NULL,
    median       DOUBLE PRECISION         NOT NULL,
    percentage   DOUBLE PRECISION         NOT NULL,
    created_at   timestamp with time zone not null,
    modified_at  timestamp with time zone,
    PRIMARY KEY (id)
);

CREATE TABLE reaction_property
(
    id                  UUID,
    reaction_trigger_fk UUID
        CONSTRAINT reaction_property_trigger_fkey REFERENCES reaction_statistics (id) ON DELETE CASCADE,
    reaction_action_fk  UUID
        CONSTRAINT reaction_property_action_fkey REFERENCES reaction_statistics (id) ON DELETE CASCADE,
    property_key        VARCHAR(1024) NOT NULL,
    property_value      VARCHAR(1024) NOT NULL,
    PRIMARY KEY (id)
);