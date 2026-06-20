package org.example.roomsched.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.example.roomsched.entity.BookingRecord;
import org.example.roomsched.entity.BookingStatus;
import org.example.roomsched.entity.MeetingRoom;
import org.example.roomsched.entity.SysUser;
import org.example.roomsched.mapper.BookingMapper;
import org.example.roomsched.service.BookingService;
import org.example.roomsched.service.RoomService;
import org.example.roomsched.service.UserService;
import org.example.roomsched.vo.BookingVO;
import org.example.roomsched.vo.DashboardVO;
import org.example.roomsched.vo.RoomVO;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理端控制器
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final RoomService roomService;
    private final BookingService bookingService;
    private final UserService userService;
    private final BookingMapper bookingMapper;

    /**
     * 管理后台根路径重定向到仪表盘
     */
    @GetMapping({"", "/"})
    public String adminIndex() {
        return "redirect:/admin/dashboard";
    }

    /**
     * 仪表盘页面
     * 性能优化：排行榜改用 SQL GROUP BY 聚合，今日列表用批量查询消除 N+1
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        DashboardVO dashboard = new DashboardVO();
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        // 1. 今日预约数
        long todayBookingCount = bookingService.count(
                new LambdaQueryWrapper<BookingRecord>()
                        .apply("DATE(start_time) = {0}", today));
        dashboard.setTodayBookingCount(todayBookingCount);

        // 2. 待审批数
        long pendingApprovalCount = bookingService.count(
                new LambdaQueryWrapper<BookingRecord>()
                        .eq(BookingRecord::getStatus, BookingStatus.PENDING));
        dashboard.setPendingApprovalCount(pendingApprovalCount);

        // 3. 会议室总数
        long totalRoomCount = roomService.count(
                new LambdaQueryWrapper<MeetingRoom>()
                        .eq(MeetingRoom::getStatus, 1));
        dashboard.setTotalRoomCount(totalRoomCount);

        // 4. 今日签到率
        long todayShouldCheckIn = bookingService.count(
                new LambdaQueryWrapper<BookingRecord>()
                        .apply("DATE(start_time) = {0}", today)
                        .in(BookingRecord::getStatus, BookingStatus.APPROVED, BookingStatus.CHECKED_IN, BookingStatus.COMPLETED));
        long todayCheckedIn = bookingService.count(
                new LambdaQueryWrapper<BookingRecord>()
                        .apply("DATE(start_time) = {0}", today)
                        .in(BookingRecord::getStatus, BookingStatus.CHECKED_IN, BookingStatus.COMPLETED));
        double todayCheckInRate = todayShouldCheckIn > 0
                ? (double) todayCheckedIn / todayShouldCheckIn * 100
                : 0;
        dashboard.setTodayCheckInRate(todayCheckInRate);

        // 5. 本周利用率（SQL 计算总预约时长）
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        LocalDateTime weekStartTime = weekStart.atTime(LocalTime.MIN);
        LocalDateTime weekEndTime = weekEnd.atTime(LocalTime.MAX);

        Long totalBookedHours = bookingMapper.sumBookedHoursInWeek(weekStartTime, weekEndTime);
        long bookedHours = totalBookedHours != null ? totalBookedHours : 0L;
        // 可用时长：每天 8:00-22:00（14小时），7天
        long totalAvailableHours = totalRoomCount * 7 * 14;
        double weeklyUtilizationRate = totalAvailableHours > 0
                ? (double) bookedHours / totalAvailableHours * 100
                : 0;
        dashboard.setWeeklyUtilizationRate(weeklyUtilizationRate);

        // 6. 本月预约排行 TOP5（用户）— SQL GROUP BY 聚合，消除内存分组
        LocalDateTime monthStart = today.withDayOfMonth(1).atTime(LocalTime.MIN);
        List<Map<String, Object>> monthlyTopUsers = bookingMapper.selectTopUsersByMonth(monthStart);
        dashboard.setMonthlyTopUsers(monthlyTopUsers);

        // 7. 本月会议室使用排行 TOP5 — SQL GROUP BY 聚合
        List<Map<String, Object>> topUsedRooms = bookingMapper.selectTopRoomsByMonth(monthStart);
        dashboard.setTopUsedRooms(topUsedRooms);

        // 8. 最近待审批预约 TOP5（JOIN 查询消除 N+1）
        IPage<BookingVO> pendingPage = bookingMapper.selectPendingBookingVOs(new Page<>(1, 5));
        pendingPage.getRecords().forEach(vo -> {
            if (vo.getStatus() != null) {
                vo.setStatusText(BookingStatus.valueOf(vo.getStatus()).getDisplayName());
            }
        });
        dashboard.setPendingBookings(pendingPage.getRecords());

        // 8. 今日预约列表（批量查询 room，消除 N+1）
        List<BookingRecord> todayBookingsList = bookingService.list(
                new LambdaQueryWrapper<BookingRecord>()
                        .apply("DATE(start_time) = {0}", today)
                        .in(BookingRecord::getStatus, BookingStatus.APPROVED, BookingStatus.CHECKED_IN, BookingStatus.COMPLETED)
                        .orderByAsc(BookingRecord::getStartTime));

        // 批量查会议室，一次查询代替 N 次
        Set<Long> roomIds = todayBookingsList.stream()
                .map(BookingRecord::getRoomId).collect(Collectors.toSet());
        Map<Long, String> roomNameMap = roomIds.isEmpty() ? Collections.emptyMap()
                : roomService.listByIds(roomIds).stream()
                .collect(Collectors.toMap(MeetingRoom::getId, MeetingRoom::getRoomName));

        List<Map<String, Object>> todayBookings = todayBookingsList.stream()
                .map(b -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("title", b.getMeetingTitle());
                    map.put("startTime", b.getStartTime());
                    map.put("endTime", b.getEndTime());
                    map.put("roomName", roomNameMap.getOrDefault(b.getRoomId(), "会议室" + b.getRoomId()));
                    return map;
                })
                .collect(Collectors.toList());
        dashboard.setTodayBookings(todayBookings);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRooms", dashboard.getTotalRoomCount());
        stats.put("totalUsers", userService.count());
        stats.put("todayCheckedIn", todayCheckedIn);
        stats.put("pendingApprovals", dashboard.getPendingApprovalCount());
        stats.put("topRooms", dashboard.getTopUsedRooms());

        model.addAttribute("dashboard", dashboard);
        model.addAttribute("stats", stats);
        return "admin/dashboard";
    }

    /**
     * 会议室管理页面
     */
    @GetMapping("/rooms")
    public String roomManagePage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            Model model) {

        Page<MeetingRoom> pageParam = new Page<>(page, size);
        Page<MeetingRoom> roomPage = roomService.page(pageParam,
                new LambdaQueryWrapper<MeetingRoom>()
                        .orderByDesc(MeetingRoom::getCreateTime));

        List<RoomVO> roomVOList = roomPage.getRecords().stream()
                .map(RoomVO::fromEntity)
                .collect(Collectors.toList());

        model.addAttribute("page", roomPage);
        model.addAttribute("roomPage", roomPage);
        model.addAttribute("roomVOList", roomVOList);
        return "admin/room-manage";
    }

    /**
     * 新增会议室
     */
    @PostMapping("/rooms")
    public String addRoom(MeetingRoom room, RedirectAttributes redirectAttributes) {
        try {
            roomService.addRoom(room);
            redirectAttributes.addFlashAttribute("success", "会议室添加成功");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "添加失败：" + e.getMessage());
        }
        return "redirect:/admin/rooms";
    }

    /**
     * 更新会议室
     */
    @PostMapping("/rooms/{id}")
    public String updateRoom(@PathVariable Long id, MeetingRoom room, RedirectAttributes redirectAttributes) {
        try {
            room.setId(id);
            roomService.updateRoom(room);
            redirectAttributes.addFlashAttribute("success", "会议室更新成功");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "更新失败：" + e.getMessage());
        }
        return "redirect:/admin/rooms";
    }

    /**
     * 删除会议室
     */
    @PostMapping("/rooms/{id}/delete")
    public String deleteRoom(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            roomService.deleteRoom(id);
            redirectAttributes.addFlashAttribute("success", "会议室删除成功");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "删除失败：" + e.getMessage());
        }
        return "redirect:/admin/rooms";
    }

    /**
     * 审批中心页面（JOIN 查询消除 N+1）
     */
    @GetMapping("/approvals")
    public String approvalPage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            Model model) {

        // JOIN 查询，一次获取会议室名称和用户名，消除 N+1
        IPage<BookingVO> bookingVOPage = bookingMapper.selectPendingBookingVOs(new Page<>(page, size));

        // 补充 statusText（无需额外查库）
        bookingVOPage.getRecords().forEach(vo -> {
            if (vo.getStatus() != null) {
                vo.setStatusText(BookingStatus.valueOf(vo.getStatus()).getDisplayName());
            }
        });

        model.addAttribute("pendingBookings", bookingVOPage.getRecords());
        model.addAttribute("bookingPage", bookingVOPage);
        model.addAttribute("bookingVOList", bookingVOPage.getRecords());
        return "admin/approval-list";
    }

    /**
     * 全部预约页面
     */
    @GetMapping("/bookings")
    public String bookingsPage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long roomId,
            Model model) {

        IPage<BookingVO> bookingPage = bookingMapper.selectAllBookingVOs(
                new Page<>(page, size), status, roomId);
        bookingPage.getRecords().forEach(vo -> {
            if (vo.getStatus() != null) {
                try {
                    vo.setStatusText(BookingStatus.valueOf(vo.getStatus()).getDisplayName());
                } catch (IllegalArgumentException ignored) {
                    vo.setStatusText(vo.getStatus());
                }
            }
        });

        model.addAttribute("page", bookingPage);
        model.addAttribute("rooms", roomService.list());
        model.addAttribute("status", status);
        model.addAttribute("roomId", roomId);
        return "admin/bookings";
    }

    /**
     * 用户管理页面
     */
    @GetMapping("/users")
    public String usersPage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            Model model) {

        Page<SysUser> userPage = userService.page(new Page<>(page, size),
                new LambdaQueryWrapper<SysUser>()
                        .orderByDesc(SysUser::getCreateTime));

        model.addAttribute("page", userPage);
        return "admin/users";
    }
}
