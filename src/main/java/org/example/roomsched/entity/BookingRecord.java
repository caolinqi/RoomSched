package org.example.roomsched.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 预约记录实体类
 * 对应数据库表：booking_record
 */
@Data
@TableName("booking_record")
public class BookingRecord {

    /** 预约ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会议室ID */
    private Long roomId;

    /** 预约人ID */
    private Long userId;

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
     * 预约状态（枚举，数据库存储为 name() 字符串）
     * 见 {@link BookingStatus} 了解所有状态和流转规则
     */
    private BookingStatus status;

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

    /** 更新时间 */
    private LocalDateTime updateTime;
}
