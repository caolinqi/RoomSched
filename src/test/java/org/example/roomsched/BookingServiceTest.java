package org.example.roomsched;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.roomsched.entity.BookingRecord;
import org.example.roomsched.entity.BookingStatus;
import org.example.roomsched.entity.MeetingRoom;
import org.example.roomsched.service.BookingService;
import org.example.roomsched.service.RoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * BookingService 单元测试（走 H2 内存库，每个测试用例自动回滚）
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("BookingService 业务逻辑测试")
class BookingServiceTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private RoomService roomService;

    /** 测试用会议室 ID */
    private Long roomId;
    /** 测试用用户 ID */
    private static final Long TEST_USER_ID = 1L;

    @BeforeEach
    void setUp() {
        // 创建测试会议室（每个测试隔离）
        MeetingRoom room = new MeetingRoom();
        room.setRoomName("测试会议室");
        room.setCapacity(10);
        room.setStatus(1);
        roomService.addRoom(room);
        roomId = room.getId();
    }

    // ========== 正常提交 ==========

    @Test
    @DisplayName("正常提交预约 → 成功，状态为 PENDING")
    void submitBooking_normalCase_shouldSucceed() {
        BookingRecord record = newBookingRecord(
                LocalDateTime.now().plusHours(2),
                LocalDateTime.now().plusHours(4));

        bookingService.submitBooking(record, TEST_USER_ID);

        BookingRecord saved = bookingService.getById(record.getId());
        assertThat(saved).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(saved.getUserId()).isEqualTo(TEST_USER_ID);
    }

    // ========== 时间校验 ==========

    @Test
    @DisplayName("开始时间 >= 结束时间 → 抛出异常")
    void submitBooking_startAfterEnd_shouldThrow() {
        BookingRecord record = newBookingRecord(
                LocalDateTime.now().plusHours(4),
                LocalDateTime.now().plusHours(2));

        assertThatThrownBy(() -> bookingService.submitBooking(record, TEST_USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("开始时间必须早于结束时间");
    }

    @Test
    @DisplayName("开始时间在过去 → 抛出异常")
    void submitBooking_startInPast_shouldThrow() {
        BookingRecord record = newBookingRecord(
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(1));

        assertThatThrownBy(() -> bookingService.submitBooking(record, TEST_USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("过去");
    }

    @Test
    @DisplayName("超过 8 小时 → 抛出异常")
    void submitBooking_exceeds8Hours_shouldThrow() {
        BookingRecord record = newBookingRecord(
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusHours(10));

        assertThatThrownBy(() -> bookingService.submitBooking(record, TEST_USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("8小时");
    }

    @Test
    @DisplayName("超过 30 天提前 → 抛出异常")
    void submitBooking_tooFarInAdvance_shouldThrow() {
        BookingRecord record = newBookingRecord(
                LocalDateTime.now().plusDays(31),
                LocalDateTime.now().plusDays(31).plusHours(2));

        assertThatThrownBy(() -> bookingService.submitBooking(record, TEST_USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("30天");
    }

    // ========== 容量校验 ==========

    @Test
    @DisplayName("参会人数超过容量 → 抛出异常")
    void submitBooking_exceededCapacity_shouldThrow() {
        BookingRecord record = newBookingRecord(
                LocalDateTime.now().plusHours(2),
                LocalDateTime.now().plusHours(4));
        record.setAttendeeCount(100); // 超过容量 10

        assertThatThrownBy(() -> bookingService.submitBooking(record, TEST_USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("容量");
    }

    // ========== 时间冲突 ==========

    @Test
    @DisplayName("时间段冲突 → 第二次提交抛出异常")
    void submitBooking_conflictTime_shouldThrow() {
        LocalDateTime start = LocalDateTime.now().plusHours(2);
        LocalDateTime end = LocalDateTime.now().plusHours(4);

        // 第一次提交并审批通过（产生有效占用）
        BookingRecord first = newBookingRecord(start, end);
        bookingService.submitBooking(first, TEST_USER_ID);
        // 修改状态为 APPROVED 使冲突检测生效
        first.setStatus(BookingStatus.APPROVED);
        bookingService.updateById(first);

        // 第二次提交相同时间段
        BookingRecord second = newBookingRecord(start, end);
        assertThatThrownBy(() -> bookingService.submitBooking(second, TEST_USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("冲突");
    }

    // ========== 取消预约 ==========

    @Test
    @DisplayName("取消 PENDING 预约 → 成功")
    void cancelBooking_pendingStatus_shouldSucceed() {
        BookingRecord record = newBookingRecord(
                LocalDateTime.now().plusHours(2),
                LocalDateTime.now().plusHours(4));
        bookingService.submitBooking(record, TEST_USER_ID);

        bookingService.cancelBooking(record.getId(), TEST_USER_ID);

        BookingRecord updated = bookingService.getById(record.getId());
        assertThat(updated.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    @DisplayName("取消 APPROVED 预约（开始前 60 分钟）→ 成功（超过30分钟截止线）")
    void cancelBooking_approvedStatus_beforeDeadline_shouldSucceed() {
        // 会议在 60 分钟后开始，截止取消时间是 30 分钟后，现在未超过，可以取消
        BookingRecord record = newBookingRecord(
                LocalDateTime.now().plusMinutes(60),
                LocalDateTime.now().plusHours(2));
        bookingService.submitBooking(record, TEST_USER_ID);
        record.setStatus(BookingStatus.APPROVED);
        bookingService.updateById(record);

        bookingService.cancelBooking(record.getId(), TEST_USER_ID);

        BookingRecord updated = bookingService.getById(record.getId());
        assertThat(updated.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    @DisplayName("取消 APPROVED 预约（开始前 10 分钟，超过30分钟截止）→ 抛出异常")
    void cancelBooking_approvedStatus_afterDeadline_shouldThrow() {
        // 会议在 10 分钟后开始（已超过 30 分钟截止线），不允许取消
        BookingRecord record = newBookingRecord(
                LocalDateTime.now().plusMinutes(10),
                LocalDateTime.now().plusMinutes(70));
        bookingService.submitBooking(record, TEST_USER_ID);
        record.setStatus(BookingStatus.APPROVED);
        bookingService.updateById(record);

        assertThatThrownBy(() -> bookingService.cancelBooking(record.getId(), TEST_USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("30分钟");
    }

    @Test
    @DisplayName("取消 CHECKED_IN 预约 → 抛出异常（已签到不可取消）")
    void cancelBooking_checkedInStatus_shouldThrow() {
        BookingRecord record = newBookingRecord(
                LocalDateTime.now().plusHours(2),
                LocalDateTime.now().plusHours(4));
        bookingService.submitBooking(record, TEST_USER_ID);
        record.setStatus(BookingStatus.CHECKED_IN);
        bookingService.updateById(record);

        assertThatThrownBy(() -> bookingService.cancelBooking(record.getId(), TEST_USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("不允许取消");
    }

    @Test
    @DisplayName("取消他人预约 → 抛出异常")
    void cancelBooking_otherUserBooking_shouldThrow() {
        BookingRecord record = newBookingRecord(
                LocalDateTime.now().plusHours(2),
                LocalDateTime.now().plusHours(4));
        bookingService.submitBooking(record, TEST_USER_ID);

        assertThatThrownBy(() -> bookingService.cancelBooking(record.getId(), 999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("自己");
    }

    // ========== 签到 ==========

    @Test
    @DisplayName("签到窗口外（提前 60 分钟）→ 抛出异常")
    void checkIn_tooEarly_shouldThrow() {
        BookingRecord record = newBookingRecord(
                LocalDateTime.now().plusHours(2),
                LocalDateTime.now().plusHours(4));
        bookingService.submitBooking(record, TEST_USER_ID);
        record.setStatus(BookingStatus.APPROVED);
        bookingService.updateById(record);

        assertThatThrownBy(() -> bookingService.checkIn(record.getId(), TEST_USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("签到时间");
    }

    // ========== 私有工具方法 ==========

    private BookingRecord newBookingRecord(LocalDateTime start, LocalDateTime end) {
        BookingRecord record = new BookingRecord();
        record.setRoomId(roomId);
        record.setMeetingTitle("测试会议");
        record.setAttendeeCount(5);
        record.setStartTime(start);
        record.setEndTime(end);
        return record;
    }
}
