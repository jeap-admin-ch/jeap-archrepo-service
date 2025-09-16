create table system_graph
(
    id           uuid,
    system_name  varchar not null,
    graph_data   bytea,
    fingerprint  varchar not null,
    created_at   timestamp with time zone not null,
    modified_at  timestamp with time zone,
    PRIMARY KEY (id)
);

create table component_graph
(
    id             uuid,
    system_name    varchar not null,
    component_name varchar not null,
    graph_data     bytea,
    fingerprint    varchar not null,
    created_at     timestamp with time zone not null,
    modified_at    timestamp with time zone,
    PRIMARY KEY (id)
);

create table message_graph
(
    id                uuid,
    message_type_name varchar not null,
    variant           varchar not null,
    graph_data        bytea,
    fingerprint       varchar not null,
    created_at        timestamp with time zone not null,
    modified_at       timestamp with time zone,
    PRIMARY KEY (id)
);
