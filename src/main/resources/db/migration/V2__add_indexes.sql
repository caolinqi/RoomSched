-- ============================================================
-- V2: 添加数据库索引
-- 使用存储过程判断索引是否存在，避免重复创建报错
-- ============================================================

-- booking_record：冲突检测、状态大屏、自动释放定时任务
CREATE INDEX IF NOT EXISTS idx_booking_room_time_status
    ON booking_record (room_id, start_time, end_time, status);

-- booking_record：用户预约列表分页查询
CREATE INDEX IF NOT EXISTS idx_booking_user_time
    ON booking_record (user_id, start_time);

-- booking_record：待审批列表、自动释放任务（按 status + start_time 查询）
CREATE INDEX IF NOT EXISTS idx_booking_status_time
    ON booking_record (status, start_time);

-- booking_record：仪表盘按 create_time 聚合统计
CREATE INDEX IF NOT EXISTS idx_booking_create_time
    ON booking_record (create_time);

-- sys_user：登录时按 username 查询（已有 UNIQUE KEY，此处无需重复建索引）
-- CREATE INDEX IF NOT EXISTS idx_user_username ON sys_user (username);

-- meeting_room：按状态查询可用会议室
CREATE INDEX IF NOT EXISTS idx_room_status
    ON meeting_room (status);
