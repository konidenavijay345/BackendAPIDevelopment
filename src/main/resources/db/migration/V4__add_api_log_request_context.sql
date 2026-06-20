ALTER TABLE api_logs
    ADD COLUMN user_email VARCHAR(254) NULL AFTER user_id,
    ADD COLUMN user_name VARCHAR(120) NULL AFTER user_email,
    ADD COLUMN query_string VARCHAR(2048) NULL AFTER request_path,
    ADD COLUMN jwt_present BOOLEAN NOT NULL DEFAULT FALSE AFTER user_agent,
    ADD COLUMN jwt_valid BOOLEAN NOT NULL DEFAULT FALSE AFTER jwt_present,
    ADD COLUMN jwt_expires_at TIMESTAMP(6) NULL AFTER jwt_valid,
    ADD COLUMN jwt_seconds_remaining BIGINT NULL AFTER jwt_expires_at,
    ADD COLUMN db_table VARCHAR(64) NULL AFTER jwt_seconds_remaining,
    ADD COLUMN db_record_id BIGINT NULL AFTER db_table,
    ADD COLUMN db_operation VARCHAR(16) NULL AFTER db_record_id;

CREATE INDEX idx_api_logs_db_record_created_at ON api_logs (db_table, db_record_id, created_at);
CREATE INDEX idx_api_logs_jwt_expires_at ON api_logs (jwt_expires_at);
