package org.example.roomsched.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.roomsched.service.BookingService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务：超时未签到自动释放预约
 * 每分钟执行一次，扫描满足以下条件的预约：
 * - 状态为 APPROVED
 * - 未签到（check_in_time 为空）
 * - 开始时间 < 当前时间 - 15分钟
 * 满足条件的预约将被自动释放，状态更新为 AUTO_RELEASED
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingReleaseTask {

    private final BookingService bookingService;

    /**
     * 每分钟执行一次，自动释放超时未签到的预约
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void autoRelease() {
        try {
            int count = bookingService.autoReleaseExpiredBookings();
            if (count > 0) {
                log.info("定时任务：自动释放了 {} 条超时未签到的预约", count);
            }
        } catch (Exception e) {
            log.error("定时任务执行失败：自动释放超时预约", e);
        }
    }
}
