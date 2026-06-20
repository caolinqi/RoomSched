-- ============================================================
-- V1: 初始化表结构
-- 使用 IF NOT EXISTS，在已有数据库中执行不会报错
-- ============================================================

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    username    VARCHAR(50)  NOT NULL COMMENT '登录账号',
    password    VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密密码',
    real_name   VARCHAR(50)           COMMENT '真实姓名',
    phone       VARCHAR(20)           COMMENT '手机号',
    email       VARCHAR(100)          COMMENT '邮箱',
    role        VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT '角色：USER/ADMIN',
    status      INT          NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1正常',
    create_time DATETIME              COMMENT '创建时间',
    update_time DATETIME              COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '用户表';

-- 会议室表
CREATE TABLE IF NOT EXISTS meeting_room
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    room_name   VARCHAR(100) NOT NULL COMMENT '会议室名称',
    capacity    INT                   COMMENT '容纳人数',
    location    VARCHAR(200)          COMMENT '位置描述',
    facilities  VARCHAR(500)          COMMENT '设备配置，JSON 数组格式',
    status      INT          NOT NULL DEFAULT 1 COMMENT '状态：0维修中 1可用 2停用',
    create_time DATETIME              COMMENT '创建时间',
    update_time DATETIME              COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '会议室表';

-- 预约记录表
CREATE TABLE IF NOT EXISTS booking_record
(
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    room_id        BIGINT       NOT NULL COMMENT '会议室ID',
    user_id        BIGINT       NOT NULL COMMENT '预约人ID',
    meeting_title  VARCHAR(100) NOT NULL COMMENT '会议主题',
    start_time     DATETIME     NOT NULL COMMENT '开始时间',
    end_time       DATETIME     NOT NULL COMMENT '结束时间',
    attendee_count INT                   COMMENT '参会人数',
    remark         VARCHAR(500)          COMMENT '备注',
    status         VARCHAR(30)  NOT NULL DEFAULT 'PENDING'
        COMMENT '状态：PENDING/APPROVED/REJECTED/CANCELLED/CHECKED_IN/COMPLETED/AUTO_RELEASED',
    approver_id    BIGINT                COMMENT '审批人ID',
    approve_time   DATETIME              COMMENT '审批时间',
    approve_remark VARCHAR(500)          COMMENT '审批意见',
    check_in_time  DATETIME              COMMENT '签到时间',
    create_time    DATETIME              COMMENT '创建时间',
    update_time    DATETIME              COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '预约记录表';
