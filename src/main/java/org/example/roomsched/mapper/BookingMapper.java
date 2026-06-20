package org.example.roomsched.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.roomsched.entity.BookingRecord;
import org.example.roomsched.vo.BookingVO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 预约记录 Mapper 接口
 */
@Mapper
public interface BookingMapper extends BaseMapper<BookingRecord> {

    /**
     * 带 FOR UPDATE 行锁的冲突计数（仅在 submitBooking 事务内调用）。
     * 并发下只有一个事务可通过此检测并插入，后续事务等锁释放后重新检测到冲突而失败。
     * 注意：调用方需锁定会议室行（roomMapper.selectByIdForUpdate），本方法作为二次确认。
     */
    @Select("SELECT COUNT(*) FROM booking_record " +
            "WHERE room_id = #{roomId} " +
            "AND status IN ('PENDING', 'APPROVED', 'CHECKED_IN') " +
            "AND start_time < #{endTime} " +
            "AND end_time > #{startTime}")
    long countConflict(@Param("roomId") Long roomId,
                       @Param("startTime") LocalDateTime startTime,
                       @Param("endTime") LocalDateTime endTime);

    /**
     * 分页查询待审批预约，JOIN 会议室和用户信息（消除 N+1）。
     * 结果 status 为字符串，statusText 需调用方补充。
     */
    IPage<BookingVO> selectPendingBookingVOs(Page<BookingVO> page);

    /**
     * 分页查询全部预约，支持按状态和会议室筛选，JOIN 会议室和用户信息（消除 N+1）。
     * 结果 status 为字符串，statusText 需调用方补充。
     *
     * @param page   分页参数
     * @param status 状态筛选（null 表示不筛选）
     * @param roomId 会议室筛选（null 表示不筛选）
     */
    IPage<BookingVO> selectAllBookingVOs(Page<BookingVO> page,
                                          @Param("status") String status,
                                          @Param("roomId") Long roomId);

    /**
     * 本月预约次数 TOP5 用户（SQL 聚合，消除内存分组）
     *
     * @param monthStart 本月开始时间
     * @return [{username, count}, ...]
     */
    List<Map<String, Object>> selectTopUsersByMonth(@Param("monthStart") LocalDateTime monthStart);

    /**
     * 本月预约次数 TOP5 会议室（SQL 聚合，消除内存分组）
     *
     * @param monthStart 本月开始时间
     * @return [{roomName, count}, ...]
     */
    List<Map<String, Object>> selectTopRoomsByMonth(@Param("monthStart") LocalDateTime monthStart);

    /**
     * 本周已审批预约的总时长（小时），用于仪表盘利用率计算。
     *
     * @param weekStart 本周开始时间
     * @param weekEnd   本周结束时间
     * @return 总时长（小时），可能为 null（无记录时）
     */
    @Select("SELECT COALESCE(SUM(TIMESTAMPDIFF(HOUR, start_time, end_time)), 0) " +
            "FROM booking_record " +
            "WHERE status IN ('APPROVED', 'CHECKED_IN', 'COMPLETED') " +
            "AND start_time >= #{weekStart} " +
            "AND end_time <= #{weekEnd}")
    Long sumBookedHoursInWeek(@Param("weekStart") LocalDateTime weekStart,
                               @Param("weekEnd") LocalDateTime weekEnd);
}
