-- Spec 0002: the canonical problem (ADR-0028) and the run (ADR-0023).

CREATE TABLE problem (
    id           uuid        PRIMARY KEY,
    source       text        NOT NULL,
    external_key text        NOT NULL,
    title        text        NOT NULL,
    body         text        NOT NULL,
    requester    text,
    channel      text,
    created_at   timestamptz NOT NULL,
    received_at  timestamptz NOT NULL,
    labels       text[]      NOT NULL DEFAULT '{}',
    content_hash text        NOT NULL,
    attributes   jsonb       NOT NULL DEFAULT '{}',
    CONSTRAINT problem_source_external_key_uk UNIQUE (source, external_key)
);

CREATE TABLE run (
    id              uuid        PRIMARY KEY,
    problem_id      uuid        NOT NULL REFERENCES problem (id),
    status          text        NOT NULL,
    failure_reason  text,
    failure_message text,
    proposal        jsonb,
    verdict         jsonb,
    created_at      timestamptz NOT NULL,
    updated_at      timestamptz NOT NULL,
    version         integer     NOT NULL
);

CREATE INDEX run_status_created_at_idx ON run (status, created_at);
