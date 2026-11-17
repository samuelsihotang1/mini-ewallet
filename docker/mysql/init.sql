-- Database per service: each service gets its own schema and a user that can
-- reach only that schema, so no service can join or write another's tables.
CREATE DATABASE IF NOT EXISTS wallet_db;
CREATE DATABASE IF NOT EXISTS transfer_db;
CREATE DATABASE IF NOT EXISTS notification_db;

CREATE USER IF NOT EXISTS 'wallet_svc'@'%' IDENTIFIED BY 'wallet_pass';
CREATE USER IF NOT EXISTS 'transfer_svc'@'%' IDENTIFIED BY 'transfer_pass';
CREATE USER IF NOT EXISTS 'notification_svc'@'%' IDENTIFIED BY 'notification_pass';

GRANT ALL PRIVILEGES ON wallet_db.* TO 'wallet_svc'@'%';
GRANT ALL PRIVILEGES ON transfer_db.* TO 'transfer_svc'@'%';
GRANT ALL PRIVILEGES ON notification_db.* TO 'notification_svc'@'%';
