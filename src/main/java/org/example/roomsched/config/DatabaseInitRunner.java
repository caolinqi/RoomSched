package org.example.roomsched.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseInitRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        log.info("Checking and updating database schema for Phase 1...");
        try {
            // Check if email_notify exists in sys_user
            jdbcTemplate.execute("SELECT email_notify FROM sys_user LIMIT 1");
        } catch (Exception e) {
            log.info("Adding new columns to sys_user...");
            jdbcTemplate.execute("ALTER TABLE sys_user " +
                    "ADD COLUMN avatar_url VARCHAR(255) DEFAULT NULL COMMENT '头像路径', " +
                    "ADD COLUMN email_notify TINYINT(1) DEFAULT 1 COMMENT '邮件通知开关', " +
                    "ADD COLUMN timezone VARCHAR(50) DEFAULT 'Asia/Shanghai' COMMENT '用户偏好时区'");
        }

        try {
            // Create sys_user_device table if it doesn't exist
            log.info("Creating sys_user_device table if not exists...");
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS sys_user_device (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "user_id BIGINT NOT NULL, " +
                    "device_info VARCHAR(255) NOT NULL, " +
                    "ip_address VARCHAR(50) NOT NULL, " +
                    "session_id VARCHAR(100) DEFAULT NULL, " +
                    "last_active_time DATETIME NOT NULL, " +
                    "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            log.info("Database schema update completed successfully.");
        } catch (Exception e) {
            log.error("Failed to update database schema", e);
        }
    }
}
