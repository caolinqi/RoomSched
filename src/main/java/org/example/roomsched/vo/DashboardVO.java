package org.example.roomsched.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 仪表盘数据视图对象
 */
@Data
public class DashboardVO {

    /** 今日预约数 */
    private Long todayBookingCount;

    /** 待审批数 */
    private Long pendingApprovalCount;

    /** 会议室总数 */
    private Long totalRoomCount;

    /** 今日签到率（百分比） */
    private Double todayCheckInRate;

    /** 本周利用率（百分比） */
    private Double weeklyUtilizationRate;

    /** 本月预约排行 TOP5（用户维度） */
    private List<Map<String, Object>> monthlyTopUsers;

    /** 会议室使用排行 TOP5 */
    private List<Map<String, Object>> topUsedRooms;

    /** 今日预约列表 */
    private List<Map<String, Object>> todayBookings;

    /** 最近待审批预约列表 TOP5 */
    private List<BookingVO> pendingBookings;
}
