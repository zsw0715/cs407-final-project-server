-- V6__extend_user_gender_enum.sql
-- add NON_BINARY option to user.gender
ALTER TABLE user
MODIFY COLUMN gender ENUM('MALE', 'FEMALE', 'NON_BINARY') COMMENT '性别';
