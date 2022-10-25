-- liquibase formatted sql

--changeset iaktov: 1
CREATE TABLE notification_task (
    id Serial NOT NULL,
    PRIMARY KEY (id),
    chat_Id bigint NOT NULL ,
    notification_Text text NOT NULL ,
    date_Time timestamp NOT NULL
);