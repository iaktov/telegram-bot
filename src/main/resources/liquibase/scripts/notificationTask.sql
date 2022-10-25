-- liquibase formatted sql

--changeset iaktov: 1
CREATE TABLE notification_task (
    id Serial,
    chat_Id bigint,
    notification_Text text,
    date_Time timestamp
);