--liquibase formatted sql

--changeset Liquibase Rafael:1

create table "user"
(
    id       uuid primary key default gen_random_uuid(),
    name     varchar(255) not null,
    email    varchar(255) not null,
    password varchar(255) not null
)