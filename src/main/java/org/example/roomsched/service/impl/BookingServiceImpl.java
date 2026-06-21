package org.example.roomsched.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.roomsched.entity.BookingRecord;
import org.example.roomsched.entity.BookingStatus;
import org.example.roomsched.entity.MeetingRoom;
import org.example.roomsched.mapper.BookingMapper;
import org.example.roomsched.mapper.RoomMapper;
import org.example.roomsched.service.BookingService;
import org.example.roomsched.service.RoomService;
import org.example.roomsched.service.MailService;
import org.example.roomsched.service.UserService;
import org.example.roomsched.entity.SysUser;
import org.example.roomsched.annotation.LogAction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 预约记录 Service 实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl extends ServiceImpl<BookingMapper, BookingRecord> implements BookingService {

    private final RoomService roomService;
    private final RoomMapper roomMapper;
    private final MailService mailService;
    private final UserService userService;

    @Override
    @Transactional
    @LogAction("提交会议室预约")
    public void submitBooking(BookingRecord record, Long userId) {
        // ---- 基本参数校验 ----
        if (record.getRoomId() == null) {
            throw new RuntimeException("请选择会议室");
        }
        if (record.getMeetingTitle() == null || record.getMeetingTitle().isBlank()) {
            throw new org.example.roomsched.exception.BusinessException(org.example.roomsched.exception.ErrorCode.BOOKING_TITLE_EMPTY);
        }
        if (record.getMeetingTitle().length() > 100) {
            throw new RuntimeException("会议主题不能超过100个字符");
        }
        if (record.getAttendeeCount() == null || record.getAttendeeCount() <= 0) {
            throw new org.example.roomsched.exception.BusinessException(org.example.roomsched.exception.ErrorCode.BOOKING_ATTENDEES_INVALID);
        }
        if (record.getStartTime() == null || record.getEndTime() == null) {
            throw new org.example.roomsched.exception.BusinessException(org.example.roomsched.exception.ErrorCode.BOOKING_TIME_NULL);
        }
        if (!record.getStartTime().isBefore(record.getEndTime())) {
            throw new RuntimeException("开始时间必须早于结束时间");
        }
        if (record.getStartTime().isBefore(LocalDateTime.now())) {
            throw new org.example.roomsched.exception.BusinessException(org.example.roomsched.exception.ErrorCode.BOOKING_TIME_PAST);
        }
        if (record.getStartTime().plusHours(8).isBefore(record.getEndTime())) {
            throw new org.example.roomsched.exception.BusinessException(org.example.roomsched.exception.ErrorCode.BOOKING_DURATION_EXCEED);
        }
        if (record.getStartTime().isAfter(LocalDateTime.now().plusDays(30))) {
            throw new RuntimeException("最多提前30天预约");
        }

        // ---- 并发防护：锁定会议室行（FOR UPDATE）----
        // 同一会议室的并发预约请求在此排队，第一个事务完成后锁释放，后续事务执行时冲突已存在
        MeetingRoom room = roomMapper.selectByIdForUpdate(record.getRoomId());
        if (room == null) {
            throw new org.example.roomsched.exception.BusinessException(org.example.roomsched.exception.ErrorCode.ROOM_NOT_FOUND);
        }
        if (!Integer.valueOf(1).equals(room.getStatus())) {
            throw new RuntimeException("会议室当前不可预约");
        }
        if (room.getCapacity() != null && record.getAttendeeCount() > room.getCapacity()) {
            throw new RuntimeException("参会人数（" + record.getAttendeeCount()
                    + "）不能超过会议室容量（" + room.getCapacity() + "）");
        }

        // ---- 在锁保护内执行时间冲突检测 ----
        if (hasConflict(record.getRoomId(), record.getStartTime(), record.getEndTime())) {
            throw new RuntimeException("该时间段已被预约，存在时间冲突");
        }

        // ---- 保存 ----
        record.setUserId(userId);
        record.setStatus(BookingStatus.PENDING);
        save(record);
        log.info("用户 {} 提交预约，会议室 {}，时间 {} ~ {}",
                userId, record.getRoomId(), record.getStartTime(), record.getEndTime());
    }

    @Override
    public boolean hasConflict(Long roomId, LocalDateTime startTime, LocalDateTime endTime) {
        // 用于前端实时检测（无锁），业务保护依靠 submitBooking 内的 FOR UPDATE 锁
        long count = baseMapper.countConflict(roomId, startTime, endTime);
        return count > 0;
    }

    @Override
    @Transactional
    @LogAction("取消会议室预约")
    public void cancelBooking(Long bookingId, Long userId) {
        BookingRecord record = getById(bookingId);
        if (record == null) {
            throw new org.example.roomsched.exception.BusinessException(org.example.roomsched.exception.ErrorCode.BOOKING_NOT_FOUND);
        }
        if (!record.getUserId().equals(userId)) {
            throw new RuntimeException("只能取消自己的预约");
        }
        if (!record.getStatus().canCancel()) {
            throw new RuntimeException("当前状态（" + record.getStatus().getDisplayName() + "）不允许取消");
        }

        // APPROVED 状态需检查：会议开始前 30 分钟截止取消
        if (record.getStatus() == BookingStatus.APPROVED) {
            LocalDateTime cancelDeadline = record.getStartTime().minusMinutes(30);
            if (LocalDateTime.now().isAfter(cancelDeadline)) {
                throw new RuntimeException("会议开始前30分钟内不允许取消，请联系管理员处理");
            }
        }

        record.setStatus(BookingStatus.CANCELLED);
        updateById(record);
        log.info("用户 {} 取消预约 {}（原状态：{}）", userId, bookingId, record.getStatus().name());
    }

    @Override
    @Transactional
    @LogAction("会议室签到")
    public void checkIn(Long bookingId, Long userId) {
        BookingRecord record = getById(bookingId);
        if (record == null) {
            throw new org.example.roomsched.exception.BusinessException(org.example.roomsched.exception.ErrorCode.BOOKING_NOT_FOUND);
        }
        if (!record.getUserId().equals(userId)) {
            throw new RuntimeException("只能签到自己的预约");
        }
        if (!record.getStatus().canCheckIn()) {
            throw new RuntimeException("当前状态（" + record.getStatus().getDisplayName() + "）不允许签到");
        }

        // 签到时间窗口：开始前15分钟 ~ 开始后15分钟
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = record.getStartTime().minusMinutes(15);
        LocalDateTime windowEnd = record.getStartTime().plusMinutes(15);

        if (now.isBefore(windowStart) || now.isAfter(windowEnd)) {
            throw new org.example.roomsched.exception.BusinessException(org.example.roomsched.exception.ErrorCode.STATUS_CHECKIN_TIME_INVALID);
        }

        record.setStatus(BookingStatus.CHECKED_IN);
        record.setCheckInTime(now);
        updateById(record);
        log.info("用户 {} 签到预约 {}", userId, bookingId);
    }

    @Override
    public Page<BookingRecord> pageByUserId(Long userId, Integer page, Integer size) {
        return page(
                new Page<>(page, size),
                new LambdaQueryWrapper<BookingRecord>()
                        .eq(BookingRecord::getUserId, userId)
                        .orderByDesc(BookingRecord::getStartTime));
    }

    @Override
    public Page<BookingRecord> pagePending(Integer page, Integer size) {
        return page(
                new Page<>(page, size),
                new LambdaQueryWrapper<BookingRecord>()
                        .eq(BookingRecord::getStatus, BookingStatus.PENDING)
                        .orderByAsc(BookingRecord::getCreateTime));
    }

    @Override
    @Transactional
    @LogAction("审批通过预约")
    public void approve(Long bookingId, Long approverId, String approveRemark) {
        BookingRecord record = getById(bookingId);
        if (record == null) {
            throw new org.example.roomsched.exception.BusinessException(org.example.roomsched.exception.ErrorCode.BOOKING_NOT_FOUND);
        }
        if (!record.getStatus().canApprove()) {
            throw new RuntimeException("当前状态（" + record.getStatus().getDisplayName() + "）不可审批");
        }
        record.setStatus(BookingStatus.APPROVED);
        record.setApproverId(approverId);
        record.setApproveTime(LocalDateTime.now());
        record.setApproveRemark(approveRemark);
        updateById(record);
        log.info("管理员 {} 审批通过预约 {}", approverId, bookingId);

        // 异步发送通过邮件
        SysUser user = userService.getById(record.getUserId());
        MeetingRoom room = roomService.getById(record.getRoomId());
        if (user != null && room != null) {
            mailService.sendReservationSuccessEmail(user, record, room);
        }
    }

    @Override
    @Transactional
    @LogAction("审批拒绝预约")
    public void reject(Long bookingId, Long approverId, String approveRemark) {
        BookingRecord record = getById(bookingId);
        if (record == null) {
            throw new org.example.roomsched.exception.BusinessException(org.example.roomsched.exception.ErrorCode.BOOKING_NOT_FOUND);
        }
        if (!record.getStatus().canApprove()) {
            throw new RuntimeException("当前状态（" + record.getStatus().getDisplayName() + "）不可审批");
        }
        record.setStatus(BookingStatus.REJECTED);
        record.setApproverId(approverId);
        record.setApproveTime(LocalDateTime.now());
        record.setApproveRemark(approveRemark);
        updateById(record);
        log.info("管理员 {} 审批拒绝预约 {}", approverId, bookingId);

        // 异步发送驳回邮件
        SysUser user = userService.getById(record.getUserId());
        MeetingRoom room = roomService.getById(record.getRoomId());
        if (user != null && room != null) {
            mailService.sendReservationRejectEmail(user, record, room);
        }
    }

    @Override
    @Transactional
    public int autoReleaseExpiredBookings() {
        // 超时条件：start_time + 15分钟 < 当前时间，且状态为 APPROVED，且未签到
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(15);

        List<BookingRecord> expiredList = list(
                new LambdaQueryWrapper<BookingRecord>()
                        .eq(BookingRecord::getStatus, BookingStatus.APPROVED)
                        .isNull(BookingRecord::getCheckInTime)
                        .lt(BookingRecord::getStartTime, deadline));

        if (expiredList.isEmpty()) {
            return 0;
        }

        for (BookingRecord record : expiredList) {
            record.setStatus(BookingStatus.AUTO_RELEASED);
            updateById(record);
            log.info("预约 {} 超时未签到，已自动释放（会议主题：{}，开始时间：{}）",
                    record.getId(), record.getMeetingTitle(), record.getStartTime());
        }

        return expiredList.size();
    }
}
