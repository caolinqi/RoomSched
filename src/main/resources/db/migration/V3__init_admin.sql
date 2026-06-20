-- ============================================================
-- V3: 初始化 admin 账号
-- INSERT IGNORE：若 admin 已存在则跳过，不会重置密码
-- 密码：123456（BCrypt 哈希，由 PasswordGenerator 工具生成）
-- ============================================================

INSERT IGNORE INTO sys_user (username, password, real_name, role, status, create_time, update_time)
VALUES ('admin',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
        '系统管理员',
        'ADMIN',
        1,
        NOW(),
        NOW());
