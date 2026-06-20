package com.example.backendapi.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

/** Writes request audit records without exposing request bodies or credentials. */
@Service
public class ApiLogService {

    private static final Logger log = LoggerFactory.getLogger(ApiLogService.class);

    private final JdbcTemplate jdbcTemplate;

    public ApiLogService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Audit persistence is best-effort and must never change the API response. */
    public void record(
            Long userId, String userEmail, String userName, String method, String path,
            String queryString, int status, long durationMs, String clientIp, String userAgent,
            boolean jwtPresent, boolean jwtValid, Instant jwtExpiresAt, Long jwtSecondsRemaining,
            String dbTable, Long dbRecordId, String dbOperation
    ) {
        log.info(
                "RESPONSE SENT method={} path={} status={} durationMs={} clientIp={} userId={} userEmail={} userName={} jwtPresent={} jwtValid={} jwtExpiresAt={} jwtSecondsRemaining={} dbTable={} dbRecordId={} dbOperation={}",
                method, requestPath(path, queryString), status, durationMs, clientIp,
                value(userId), value(userEmail), value(userName), jwtPresent, jwtValid,
                value(jwtExpiresAt), value(jwtSecondsRemaining), value(dbTable),
                value(dbRecordId), value(dbOperation)
        );

        try {
            jdbcTemplate.update("""
                    INSERT INTO api_logs
                        (user_id, http_method, request_path, response_status,
                         duration_ms, client_ip, user_agent, user_email, user_name,
                         query_string, jwt_present, jwt_valid, jwt_expires_at,
                         jwt_seconds_remaining, db_table, db_record_id, db_operation)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, userId, method, limit(path, 2048), status, durationMs,
                    limit(clientIp, 45), limit(userAgent, 512), limit(userEmail, 254),
                    limit(userName, 120), limit(queryString, 2048), jwtPresent, jwtValid,
                    jwtExpiresAt, jwtSecondsRemaining, limit(dbTable, 64), dbRecordId,
                    limit(dbOperation, 16));
        } catch (DataAccessException exception) {
            log.error("Could not persist API audit log for {} {}", method, path, exception);
        }
    }

    private String requestPath(String path, String queryString) {
        if (queryString == null || queryString.isBlank()) {
            return path;
        }
        return path + "?" + queryString;
    }

    private Object value(Object value) {
        return value == null ? "-" : value;
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
