-- my_first_table schema

-- !Ups

CREATE TYPE PRIVILEGE AS ENUM ('MASTER', 'ADMIN', 'USER');
CREATE TYPE STATUS AS ENUM ('CREATED', 'CONFIRMED');

CREATE TABLE users (
    id UUID PRIMARY KEY ,
    deleted BOOLEAN NOT NULL,
    created_at_utc TIMESTAMP NOT NULL ,
    time_zone TEXT NOT NULL ,
    email TEXT UNIQUE NOT NULL  ,
    username TEXT NOT NULL ,
    password_hash TEXT NOT NULL ,
    password_salt TEXT NOT NULL ,
    privilege PRIVILEGE NOT NULL ,
    status STATUS NOT NULL
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

CREATE TABLE device_owners (
    device_id UUID REFERENCES devices,
    user_id UUID REFERENCES users,
    PRIMARY KEY (device_id)
);

CREATE TABLE device_guests (
   device_id UUID REFERENCES devices,
   user_id UUID REFERENCES users,
   PRIMARY KEY (device_id, user_id)
);

-- !Downs

DROP TABLE users;
DROP TABLE devices;
DROP TABLE device_owners;
DROP TABLE device_guests;