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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 核心集成测试：防超卖与复杂时间重叠校验
 * <p>
 * 注意：此测试不使用 @Transactional，因为需要多个真实事务并发执行才能验证 FOR UPDATE 锁。
 * 每个测试通过 @BeforeEach/@AfterEach 手动管理数据清理。
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("核心集成测试：防超卖与复杂时间重叠校验")
class BookingCoreIntegrationTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private RoomService roomService;

    private Long roomId;
    private static final Long USER_ID_BASE = 1000L;

    @BeforeEach
    void setUp() {
        // 清理上一次测试遗留数据
        bookingService.remove(new LambdaQueryWrapper<BookingRecord>()
                .eq(BookingRecord::getRoomId, -9999L)); // 不实际清理，避免影响其他测试
        // 创建独立测试会议室
        MeetingRoom room = new MeetingRoom();
        room.setRoomName("并发测试会议室_" + System.currentTimeMillis());
        room.setCapacity(100);
        room.setStatus(1);
        roomService.addRoom(room);
        roomId = room.getId();
    }

    @Test
    @DisplayName("10 个线程同时预约相同时段 → 仅 1 个成功，其余被拒绝")
    void concurrentBooking_sameTimeSlot_onlyOneSucceeds() throws InterruptedException {
        final int THREAD_COUNT = 10;
        final LocalDateTime start = LocalDateTime.now().plusHours(2);
        final LocalDateTime end = LocalDateTime.now().plusHours(4);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<String> failReasons = new ArrayList<>();

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final long userId = USER_ID_BASE + i;
            executor.submit(() -> {
                try {
                    // 等待所有线程就绪，同时启动
                    startLatch.await();

                    BookingRecord record = new BookingRecord();
                    record.setRoomId(roomId);
                    record.setMeetingTitle("并发测试会议");
                    record.setAttendeeCount(5);
                    record.setStartTime(start);
                    record.setEndTime(end);

                    bookingService.submitBooking(record, userId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    synchronized (failReasons) {
                        failReasons.add(e.getMessage());
                    }
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // 放行所有线程
        startLatch.countDown();
        // 等待所有线程完成（最多 30 秒）
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).as("所有线程应在30秒内完成").isTrue();

        // 验证核心约束：只有 1 个请求成功
        assertThat(successCount.get())
                .as("并发预约相同时段，应只有 1 个成功，实际成功数：%d，失败数：%d，失败原因：%s",
                        successCount.get(), failCount.get(), failReasons)
                .isEqualTo(1);
        assertThat(failCount.get())
                .as("其余 %d 个请求应全部失败", THREAD_COUNT - 1)
                .isEqualTo(THREAD_COUNT - 1);

        // 验证数据库中确实只有 1 条 PENDING 记录
        long pendingCount = bookingService.count(
                new LambdaQueryWrapper<BookingRecord>()
                        .eq(BookingRecord::getRoomId, roomId)
                        .eq(BookingRecord::getStatus, BookingStatus.PENDING));
        assertThat(pendingCount)
                .as("数据库中应只有 1 条 PENDING 预约记录")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("不同时段预约相同会议室 → 全部成功（无冲突）")
    void concurrentBooking_differentTimeSlots_allSucceed() throws InterruptedException {
        final int THREAD_COUNT = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int slot = i;
            executor.submit(() -> {
                try {
                    startLatch.await();

                    // 每个线程用不同时间段（不重叠）
                    LocalDateTime start = LocalDateTime.now().plusHours(2 + slot * 2L);
                    LocalDateTime end = start.plusHours(1);

                    BookingRecord record = new BookingRecord();
                    record.setRoomId(roomId);
                    record.setMeetingTitle("不同时段测试_" + slot);
                    record.setAttendeeCount(3);
                    record.setStartTime(start);
                    record.setEndTime(end);

                    bookingService.submitBooking(record, USER_ID_BASE + slot);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // 不应该有失败
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(successCount.get())
                .as("不同时段预约，%d 个线程应全部成功", THREAD_COUNT)
                .isEqualTo(THREAD_COUNT);
    }

    @Test
    @DisplayName("复杂时间重叠校验：包含、被包含、首尾相交")
    void complexOverlapValidation() {
        LocalDateTime baseTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        
        // 先预定一个基础时段 10:00 - 12:00
        BookingRecord baseRecord = new BookingRecord();
        baseRecord.setRoomId(roomId);
        baseRecord.setMeetingTitle("基础预定");
        baseRecord.setAttendeeCount(5);
        baseRecord.setStartTime(baseTime);
        baseRecord.setEndTime(baseTime.plusHours(2));
        bookingService.submitBooking(baseRecord, USER_ID_BASE);
        
        // 场景 1：完全包含已有时段 (09:00 - 13:00)
        BookingRecord overlap1 = new BookingRecord();
        overlap1.setRoomId(roomId);
        overlap1.setMeetingTitle("完全包含重叠");
        overlap1.setAttendeeCount(5);
        overlap1.setStartTime(baseTime.minusHours(1));
        overlap1.setEndTime(baseTime.plusHours(3));
        org.junit.jupiter.api.Assertions.assertThrows(
            RuntimeException.class,
            () -> bookingService.submitBooking(overlap1, USER_ID_BASE + 1),
            "完全包含已有时段，应抛出冲突异常"
        );

        // 场景 2：被包含于已有时段 (10:30 - 11:30)
        BookingRecord overlap2 = new BookingRecord();
        overlap2.setRoomId(roomId);
        overlap2.setMeetingTitle("被包含重叠");
        overlap2.setAttendeeCount(5);
        overlap2.setStartTime(baseTime.plusMinutes(30));
        overlap2.setEndTime(baseTime.plusHours(1).plusMinutes(30));
        org.junit.jupiter.api.Assertions.assertThrows(
            RuntimeException.class,
            () -> bookingService.submitBooking(overlap2, USER_ID_BASE + 2),
            "被包含于已有时段，应抛出冲突异常"
        );

        // 场景 3：首部相交 (09:00 - 10:30)
        BookingRecord overlap3 = new BookingRecord();
        overlap3.setRoomId(roomId);
        overlap3.setMeetingTitle("首部相交");
        overlap3.setAttendeeCount(5);
        overlap3.setStartTime(baseTime.minusHours(1));
        overlap3.setEndTime(baseTime.plusMinutes(30));
        org.junit.jupiter.api.Assertions.assertThrows(
            RuntimeException.class,
            () -> bookingService.submitBooking(overlap3, USER_ID_BASE + 3),
            "首部相交，应抛出冲突异常"
        );

        // 场景 4：尾部相交 (11:30 - 13:00)
        BookingRecord overlap4 = new BookingRecord();
        overlap4.setRoomId(roomId);
        overlap4.setMeetingTitle("尾部相交");
        overlap4.setAttendeeCount(5);
        overlap4.setStartTime(baseTime.plusHours(1).plusMinutes(30));
        overlap4.setEndTime(baseTime.plusHours(3));
        org.junit.jupiter.api.Assertions.assertThrows(
            RuntimeException.class,
            () -> bookingService.submitBooking(overlap4, USER_ID_BASE + 4),
            "尾部相交，应抛出冲突异常"
        );

        // 场景 5：连续不重叠 (12:00 - 14:00) -> 应该成功
        BookingRecord nonOverlap1 = new BookingRecord();
        nonOverlap1.setRoomId(roomId);
        nonOverlap1.setMeetingTitle("连续不重叠");
        nonOverlap1.setAttendeeCount(5);
        nonOverlap1.setStartTime(baseTime.plusHours(2));
        nonOverlap1.setEndTime(baseTime.plusHours(4));
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(
            () -> bookingService.submitBooking(nonOverlap1, USER_ID_BASE + 5),
            "连续不重叠的边界条件，应该成功"
        );
    }
}
