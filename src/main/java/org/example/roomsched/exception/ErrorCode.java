package org.example.roomsched.exception;

public enum ErrorCode {
    // 权限相关
    UNAUTHORIZED("error.unauthorized", "未登录或登录已过期"),
    FORBIDDEN("error.forbidden", "无权执行此操作"),
    
    // 会议室相关
    ROOM_NOT_FOUND("error.room.not_found", "会议室不存在"),
    ROOM_ID_NULL("error.room.id_null", "会议室ID不能为空"),
    
    // 预定相关
    BOOKING_NOT_FOUND("error.booking.not_found", "预约记录不存在"),
    BOOKING_TITLE_EMPTY("error.booking.title_empty", "会议主题不能为空"),
    BOOKING_TITLE_TOO_LONG("error.booking.title_too_long", "会议主题不能超过100字符"),
    BOOKING_ATTENDEES_INVALID("error.booking.attendees_invalid", "参会人数必须大于0"),
    BOOKING_ATTENDEES_EXCEED("error.booking.attendees_exceed", "参会人数超过会议室容量"),
    BOOKING_TIME_NULL("error.booking.time_null", "开始时间和结束时间不能为空"),
    BOOKING_TIME_INVALID("error.booking.time_invalid", "开始时间必须在结束时间之前"),
    BOOKING_TIME_PAST("error.booking.time_past", "不能预约过去的时间"),
    BOOKING_DURATION_EXCEED("error.booking.duration_exceed", "单次预约不能超过8小时"),
    BOOKING_ADVANCE_TIME("error.booking.advance_time", "必须提前30分钟预约"),
    BOOKING_CONFLICT("error.booking.conflict", "该时间段已被预约，请选择其他时间"),
    
    // 状态流转相关
    STATUS_CANCEL_INVALID("error.status.cancel_invalid", "只能取消待审批或已通过的预约"),
    STATUS_CANCEL_TIMEOUT("error.status.cancel_timeout", "会议开始前30分钟内不可取消，请联系管理员"),
    STATUS_APPROVE_INVALID("error.status.approve_invalid", "只能审批待审批的预约"),
    STATUS_CHECKIN_INVALID("error.status.checkin_invalid", "只能签到已通过的预约"),
    STATUS_CHECKIN_TIME_INVALID("error.status.checkin_time_invalid", "不在签到时间范围内（会议开始前15分钟至开始后15分钟）"),
    
    // 系统相关
    SYSTEM_ERROR("error.system", "系统内部错误");

    private final String messageKey;
    private final String defaultMessage;

    ErrorCode(String messageKey, String defaultMessage) {
        this.messageKey = messageKey;
        this.defaultMessage = defaultMessage;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
