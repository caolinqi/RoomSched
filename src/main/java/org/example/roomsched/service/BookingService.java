package org.example.roomsched.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.roomsched.entity.BookingRecord;

import java.time.LocalDateTime;

/**
 * 预约记录 Service 接口
 */
public interface BookingService extends IService<BookingRecord> {

    /**
     * 提交预约
     * 
     * @param record 预约记录
     * @param userId 预约人ID
     */
    void submitBooking(BookingRecord record, Long userId);

    /**
     * 检测时间冲突
     * 
     * @param roomId    会议室ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return true表示有冲突
     */
    boolean hasConflict(Long roomId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 取消预约
     * <ul>
     *   <li>PENDING：随时可取消（尚未审批）</li>
     *   <li>APPROVED：会议开始前 30 分钟之前可取消；超过截止时间则拒绝</li>
     *   <li>其他状态（CHECKED_IN 等）不可取消</li>
     * </ul>
     *
     * @param bookingId 预约ID
     * @param userId    当前用户ID
     */
    void cancelBooking(Long bookingId, Long userId);

    /**
     * 签到
     * 
     * @param bookingId 预约ID
     * @param userId    当前用户ID
     */
    void checkIn(Long bookingId, Long userId);

    /**
     * 分页查询用户的预约记录
     * 
     * @param userId 用户ID
     * @param page   页码
     * @param size   每页大小
     * @return 分页结果
     */
    Page<BookingRecord> pageByUserId(Long userId, Integer page, Integer size);

    /**
     * 分页查询待审批的预约
     * 
     * @param page 页码
     * @param size 每页大小
     * @return 分页结果
     */
    Page<BookingRecord> pagePending(Integer page, Integer size);

    /**
     * 审批通过
     * 
     * @param bookingId     预约ID
     * @param approverId    审批人ID
     * @param approveRemark 审批意见
     */
    void approve(Long bookingId, Long approverId, String approveRemark);

    /**
     * 审批拒绝
     * 
     * @param bookingId     预约ID
     * @param approverId    审批人ID
     * @param approveRemark 审批意见
     */
    void reject(Long bookingId, Long approverId, String approveRemark);

    /**
     * 自动释放超时未签到的预约
     * 扫描条件：status = APPROVED, check_in_time IS NULL, start_time < 当前时间 - 15分钟
     * 
     * @return 释放的预约数量
     */
    int autoReleaseExpiredBookings();
}
