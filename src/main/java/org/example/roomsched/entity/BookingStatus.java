package org.example.roomsched.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 预约状态枚举
 * <p>
 * 数据库存储字段类型为 VARCHAR，存储值为枚举的 {@link #name()}（即 PENDING、APPROVED 等）。
 * MyBatis-Plus 通过 {@code EnumTypeHandler} 用 name() 进行读写，与已有数据库数据完全兼容。
 * </p>
 *
 * <pre>
 * 状态流转：
 *   PENDING ──[审批通过]──▶ APPROVED ──[签到]──▶ CHECKED_IN ──[会议结束]──▶ COMPLETED
 *   PENDING ──[审批拒绝]──▶ REJECTED
 *   PENDING ──[用户取消]──▶ CANCELLED
 *   APPROVED ──[会议开始前30min内用户取消]──▶ CANCELLED
 *   APPROVED ──[超过开始时间15min未签到]──▶ AUTO_RELEASED（由定时任务处理）
 * </pre>
 */
@Getter
@RequiredArgsConstructor
public enum BookingStatus {

    PENDING("待审批"),
    APPROVED("已通过"),
    REJECTED("已拒绝"),
    CANCELLED("已取消"),
    CHECKED_IN("已签到"),
    COMPLETED("已完成"),
    AUTO_RELEASED("已超时释放");

    /** 前端展示文本 */
    private final String displayName;

    /**
     * 是否可以被取消。
     * <ul>
     *   <li>PENDING：随时可取消（尚未审批，无实际资源占用）</li>
     *   <li>APPROVED：在会议开始前 30 分钟之前可取消（由 Service 层额外判断时间窗口）</li>
     * </ul>
     */
    public boolean canCancel() {
        return this == PENDING || this == APPROVED;
    }

    /**
     * 是否可以签到（仅 APPROVED 状态）
     */
    public boolean canCheckIn() {
        return this == APPROVED;
    }

    /**
     * 是否可以被审批通过或拒绝（仅 PENDING 状态）
     */
    public boolean canApprove() {
        return this == PENDING;
    }
}
