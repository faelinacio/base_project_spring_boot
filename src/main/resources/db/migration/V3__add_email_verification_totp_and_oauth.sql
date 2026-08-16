-- Password-based accounts remain required to verify their email; existing rows predate this
-- feature and are backfilled as verified. OAuth-only accounts (password null) are always created
-- pre-verified (Google already confirms the email), so no separate backfill is needed for them.
alter table "user"
    alter column password drop not null,
    add column email_verified boolean not null default true,
    add column google_id varchar(255),
    add column totp_secret varchar(255),
    add column totp_enabled boolean not null default false;

create unique index uk_user_google_id on "user" (google_id);

create table email_verification_tokens
(
    id         uuid primary key default gen_random_uuid(),
    user_id    uuid         not null references "user" (id) on delete cascade,
    token_hash varchar(255) not null,
    expires_at timestamp    not null,
    used       boolean      not null default false,
    created_at timestamp    not null default now()
);

create unique index uk_evt_token_hash on email_verification_tokens (token_hash);
create index ix_evt_user_id on email_verification_tokens (user_id);
