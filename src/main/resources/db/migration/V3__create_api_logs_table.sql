CREATE TABLE api_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NULL,
    http_method VARCHAR(10) NOT NULL,
    request_path VARCHAR(2048) NOT NULL,
    response_status SMALLINT NOT NULL,
    duration_ms BIGINT NOT NULL,
    client_ip VARCHAR(45) NULL,
    user_agent VARCHAR(512) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_api_logs PRIMARY KEY (id),
    CONSTRAINT fk_api_logs_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
);

CREATE INDEX idx_api_logs_user_created_at ON api_logs (user_id, created_at);
CREATE INDEX idx_api_logs_status_created_at ON api_logs (response_status, created_at);
