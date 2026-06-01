-- 0.0.1

CREATE TABLE `user` (
    id   INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

-- 0.0.2

ALTER TABLE job MODIFY COLUMN notification_state ENUM('pending', 'sent_retirement_reminder') NULL DEFAULT NULL;