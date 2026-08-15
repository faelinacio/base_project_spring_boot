--liquibase formatted sql

--changeset Liquibase Rafael:2

alter table "user"
    add column role       varchar(50)  not null default 'USER',
    add column enabled    boolean      not null default true,
    add column created_at timestamp    not null default now(),
    add column updated_at timestamp    not null default now();

alter table "user"
    add constraint uk_user_email unique (email);

create table refresh_tokens
(
    id         uuid primary key default gen_random_uuid(),
    user_id    uuid         not null references "user" (id) on delete cascade,
    token_hash varchar(255) not null,
    expires_at timestamp    not null,
    revoked    boolean      not null default false,
    created_at timestamp    not null default now()
);

create unique index uk_refresh_tokens_token_hash on refresh_tokens (token_hash);
create index ix_refresh_tokens_user_id on refresh_tokens (user_id);
