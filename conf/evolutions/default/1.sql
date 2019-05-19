-- !Ups

CREATE TYPE USER_PRIVILEGE AS ENUM ('MASTER', 'ADMIN', 'USER');
CREATE TYPE ACCOUNT_STATUS AS ENUM ('CREATED', 'CONFIRMED');
CREATE TYPE INVITATION_STATUS AS ENUM ('ACCEPTED', 'REJECTED', 'WAITING');

CREATE TABLE users (
    id UUID PRIMARY KEY ,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at_utc TIMESTAMP NOT NULL ,
    time_zone TEXT NOT NULL ,
    email TEXT UNIQUE NOT NULL  ,
    username TEXT NOT NULL ,
    password_hash TEXT NOT NULL ,
    password_salt TEXT NOT NULL ,
    privilege USER_PRIVILEGE NOT NULL ,
    status ACCOUNT_STATUS NOT NULL
);

CREATE TABLE friends (
    user_id UUID REFERENCES users,
    friend_id UUID REFERENCES users,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at_utc TIMESTAMP NOT NULL,
    time_zone TEXT NOT NULL,
    PRIMARY KEY (user_id, friend_id)
);

CREATE TABLE invitations (
    from_user UUID REFERENCES users,
    to_user UUID REFERENCES users,
    created_at_utc TIMESTAMP NOT NULL,
    time_zone TEXT NOT NULL,
    message TEXT DEFAULT '',
    status INVITATION_STATUS DEFAULT 'WAITING',
    PRIMARY KEY (from_user, to_user)
);

CREATE TABLE devices (
    id UUID PRIMARY KEY ,
    deleted BOOLEAN DEFAULT FALSE,
    created_at_utc TIMESTAMP NOT NULL,
    time_zone TEXT NOT NULL,
    name TEXT NOT NULL,
    owner UUID REFERENCES users,
    secret UUID NOT NULL
);

CREATE TABLE device_guests (
   device_id UUID REFERENCES devices,
   user_id UUID REFERENCES users,
   PRIMARY KEY (device_id, user_id)
);

-- !Downs
DROP TABLE device_guests;
DROP TABLE devices;
DROP TABLE friends;
DROP TABLE invitations;
DROP TABLE users;

DROP TYPE USER_PRIVILEGE;
DROP TYPE ACCOUNT_STATUS;
DROP TYPE INVITATION_STATUS;