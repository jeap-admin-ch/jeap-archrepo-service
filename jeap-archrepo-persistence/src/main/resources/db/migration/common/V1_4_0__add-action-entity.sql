CREATE TABLE action
(
    id                   UUID,
    reaction_statistics_id UUID not null references reaction_statistics (id) ON DELETE CASCADE,
    action_type          VARCHAR(64),
    action_fqn           VARCHAR(1024),
    created_at           timestamp with time zone,
    modified_at          timestamp with time zone,
    PRIMARY KEY (id)
);
CREATE INDEX action_reaction_statistics_id ON action (reaction_statistics_id);
