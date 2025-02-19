create table team
(
    id              UUID,
    name            varchar                  not null,
    confluence_link varchar,
    contact_address varchar,
    jira_link       varchar,
    created_at      timestamp with time zone not null,
    modified_at     timestamp with time zone,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX team_name ON team (name);

create table system
(
    id               UUID,
    name             varchar                  not null,
    default_owner_id uuid                     not null references team (id),
    confluence_link  varchar,
    description      varchar(1024),
    created_at       timestamp with time zone not null,
    modified_at      timestamp with time zone,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX system_name ON system (name);
CREATE INDEX system_default_owner_id ON system (default_owner_id);

create table message_type
(
    id                UUID,
    system_id         uuid    not null references system (id),
    message_type_name varchar not null,
    type              varchar not null,
    documentation_url varchar,
    scope             varchar,
    topic             varchar,
    description       varchar(1024),
    descriptor_url    varchar(1024),
    importer          varchar,
    PRIMARY KEY (id)
);

CREATE INDEX message_type_system_id ON message_type (system_id);

create table message_contract
(
    id                   UUID,
    message_sender_id    uuid references message_type (id),
    message_receiver_id  uuid references message_type (id),
    message_publisher_id uuid references message_type (id),
    message_consumer_id  uuid references message_type (id),
    component_name       varchar,
    topic                varchar,
    versions             varchar,
    PRIMARY KEY (id)
);

CREATE INDEX message_contract_sender_id ON message_contract (message_sender_id);
CREATE INDEX message_contract_receiver_id ON message_contract (message_receiver_id);
CREATE INDEX message_contract_publisher_id ON message_contract (message_publisher_id);
CREATE INDEX message_contract_consumer_id ON message_contract (message_consumer_id);

create table message_type_versions
(
    message_type_id       uuid    not null references message_type (id),
    key_schema_name       varchar,
    key_schema_resolved   varchar,
    key_schema_url        varchar,
    value_schema_name     varchar,
    value_schema_resolved varchar,
    value_schema_url      varchar,
    version               varchar not null,
    compatibility_mode    varchar,
    compatible_version    varchar

);

ALTER TABLE message_type_versions
    ADD CONSTRAINT pk_message_type_versions PRIMARY KEY (message_type_id, version);

create table system_aliases
(
    system_id uuid    not null references system (id),
    aliases   varchar not null
);

ALTER TABLE system_aliases
    ADD CONSTRAINT pk_system_aliases PRIMARY KEY (system_id, aliases);

create table system_component
(
    id          UUID,
    name        varchar                  not null,
    system_id   uuid                     not null references system (id),
    team_id     uuid                     not null references team (id),
    type        varchar                  not null,
    description varchar,
    importer    varchar,
    created_at  timestamp with time zone not null,
    last_seen   timestamp with time zone,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX system_component_name ON system_component (name);
CREATE INDEX system_component_system_id ON system_component (system_id);
CREATE INDEX system_component_team_id ON system_component (team_id);

create table rest_api
(
    id          UUID,
    system_id   uuid                     not null references system (id),
    provider_id uuid                     not null references system_component (id),
    method      varchar                  not null,
    path        varchar                  not null,
    created_at  timestamp with time zone not null,
    modified_at timestamp with time zone,
    PRIMARY KEY (id)
);

CREATE INDEX rest_api_system_id ON rest_api (system_id);
CREATE INDEX rest_api_provider_id ON rest_api (provider_id);

create table rest_api_importers
(
    rest_api_id uuid    not null references rest_api (id),
    importers   varchar not null
);

ALTER TABLE rest_api_importers
    ADD CONSTRAINT pk_rest_api_importers PRIMARY KEY (rest_api_id, importers);

create table relation
(
    id            UUID,
    type          varchar not null,
    system_id     uuid    not null references system (id),
    rest_api_id   uuid references rest_api (id),
    provider_name varchar,
    consumer_name varchar,
    command_name  varchar,
    event_name    varchar,
    pact_url      varchar,
    last_seen     timestamp with time zone,
    status        varchar NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (id)
);

CREATE INDEX relation_system_id ON relation (system_id);
CREATE INDEX relation_rest_api_id ON relation (rest_api_id);

create table relation_importers
(
    relation_id uuid    not null references relation (id),
    importers   varchar not null
);

ALTER TABLE relation_importers
    ADD CONSTRAINT pk_relation_importers PRIMARY KEY (relation_id, importers);

create table open_api_spec
(
    id          UUID,
    system_id   uuid                     not null references system (id),
    provider_id uuid                     not null references system_component (id),
    content     bytea,
    created_at  timestamp with time zone not null,
    modified_at timestamp with time zone,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX open_api_spec_system_provider ON open_api_spec (system_id, provider_id);
CREATE INDEX open_api_spec_system_id ON open_api_spec (system_id);
CREATE INDEX open_api_spec_provider_id ON open_api_spec (provider_id);

CREATE TABLE shedlock
(
    name       VARCHAR(64)  NOT NULL,
    lock_until TIMESTAMP    NOT NULL,
    locked_at  TIMESTAMP    NOT NULL,
    locked_by  VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);


