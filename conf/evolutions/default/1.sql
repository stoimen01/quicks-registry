-- my_first_table schema

-- !Ups

CREATE TYPE PRIVILEGE AS ENUM ('MASTER', 'ADMIN', 'USER');
CREATE TYPE STATUS AS ENUM ('CONFIRMED', 'NON_CONFIRMED');

CREATE TABLE users (
    id UUID PRIMARY KEY ,
    deleted BOOLEAN NOT NULL,
    created_at_utc TIMESTAMP NOT NULL ,
    time_zone TEXT NOT NULL ,
    email TEXT UNIQUE NOT NULL  ,
    username TEXT NOT NULL ,
    password_hash TEXT NOT NULL ,
    password_salt TEXT NOT NULL ,
    current_PRIVILEGE PRIVILEGE NOT NULL ,
    current_status STATUS NOT NULL
);

CREATE TABLE user_entries (
    id UUID,
    user_id UUID REFERENCES users,
    PRIMARY KEY (id, user_id),
    deleted BOOLEAN NOT NULL ,
    created_at_utc TIMESTAMP NOT NULL ,
    time_zone TEXT NOT NULL ,
    ip_v4 INET,
    ip_v6 INET,
    mac_address MACADDR
);

CREATE TABLE devices (
    id UUID PRIMARY KEY ,
    deleted BOOLEAN,
    created_at_utc TIMESTAMP NOT NULL ,
    time_zone TEXT NOT NULL ,
    created_by UUID REFERENCES users,
    password_hash TEXT NOT NULL ,
    password_salt TEXT NOT NULL
);





-- !Downs

DROP TABLE users;
DROP TABLE user_entries;
DROP TABLE devices;