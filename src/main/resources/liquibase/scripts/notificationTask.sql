-- liquibase formatted sql

--changeset iaktov: 1
CREATE TABLE notification_task (
    id Serial,
    chatId integer,
    notificationText text,
    dateTime timestamp
);