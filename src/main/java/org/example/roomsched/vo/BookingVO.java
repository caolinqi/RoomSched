package org.example.roomsched.vo;

import lombok.Data;
import org.example.roomsched.entity.BookingRecord;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 预约记录视图对象
 */
@Data
public class BookingVO {

    /** 预约ID */
    private Long id;

    /** 会议室ID */
    private Long roomId;

    /** 会议室名称（JOIN 或调用方填充） */
    private String roomName;

    /** 预约人ID */
    private Long userId;

    /** 预约人姓名（JOIN 或调用方填充） */
    private String userName;

    /** 会议主题 */
    private String meetingTitle;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 参会人数 */
    private Integer attendeeCount;

    /** 备注 */
    private String remark;

    /**
     * 状态字符串（如 "PENDING"）
     * BookingStatus 枚举的 name()，JSON 输出与之前保持一致，前端代码无需修改
     */
    private String status;

    /** 状态展示文本（中文，如"待审批"，来自 BookingStatus.displayName） */
    private String statusText;

    /** 审批人ID */
    private Long approverId;

    /** 审批时间 */
    private LocalDateTime approveTime;

    /** 审批意见 */
    private String approveRemark;

    /** 签到时间 */
    private LocalDateTime checkInTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /**
     * 从实体转换为 VO（不含 roomName / userName，需调用方填充）
     */
    public static BookingVO fromEntity(BookingRecord record) {
        BookingVO vo = new BookingVO();
        vo.setId(record.getId());
        vo.setRoomId(record.getRoomId());
        vo.setUserId(record.getUserId());
        vo.setMeetingTitle(record.getMeetingTitle());
        vo.setStartTime(record.getStartTime());
        vo.setEndTime(record.getEndTime());
        vo.setAttendeeCount(record.getAttendeeCount());
        vo.setRemark(record.getRemark());
        vo.setApproverId(record.getApproverId());
        vo.setApproveTime(record.getApproveTime());
        vo.setApproveRemark(record.getApproveRemark());
        vo.setCheckInTime(record.getCheckInTime());
        vo.setCreateTime(record.getCreateTime());

        // 枚举 → 字符串，消除 switch-case 魔法字符串
        if (record.getStatus() != null) {
            vo.setStatus(record.getStatus().name());
            vo.setStatusText(record.getStatus().getDisplayName());
        }

        return vo;
    }

    /** 格式化开始时间，供 Thymeleaf 模板使用 */
    public String getStartTimeFormatted() {
        if (startTime == null) return "";
        return startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    /** 格式化结束时间，供 Thymeleaf 模板使用 */
    public String getEndTimeFormatted() {
        if (endTime == null) return "";
        return endTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }
}
