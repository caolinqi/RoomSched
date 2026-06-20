package org.example.roomsched.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DatabaseInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        log.info("检查并初始化数据库表结构...");
        
        String createAuditLogTableSql = "CREATE TABLE IF NOT EXISTS sys_audit_log (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "user_id BIGINT COMMENT '操作人ID', " +
                "username VARCHAR(50) COMMENT '操作人姓名', " +
                "action VARCHAR(100) COMMENT '动作类型', " +
                "target_id BIGINT COMMENT '操作目标ID', " +
                "details TEXT COMMENT '日志详情', " +
                "ip_address VARCHAR(50) COMMENT 'IP地址', " +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间'" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统审计日志表';";
                
        try {
            jdbcTemplate.execute(createAuditLogTableSql);
            log.info("sys_audit_log 表检查/创建成功！");
        } catch (Exception e) {
            log.error("初始化数据库表失败", e);
        }
    }
}
