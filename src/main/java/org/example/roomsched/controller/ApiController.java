package org.example.roomsched.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.example.roomsched.dto.BookingForm;
import org.example.roomsched.entity.BookingRecord;
import org.example.roomsched.entity.BookingStatus;
import org.example.roomsched.entity.MeetingRoom;
import org.example.roomsched.entity.SysUser;
import org.example.roomsched.mapper.BookingMapper;
import org.example.roomsched.security.CustomUserDetails;
import org.example.roomsched.service.BookingService;
import org.example.roomsched.service.RoomService;
import org.example.roomsched.service.UserService;
import org.example.roomsched.util.Result;
import org.example.roomsched.vo.BookingVO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AJAX API 接口控制器
 * 处理前端 AJAX 请求，返回 JSON 数据
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final BookingService bookingService;
    private final RoomService roomService;
    private final UserService userService;
    private final BookingMapper bookingMapper;

    /**
     * 冲突检测（前端实时检测，无锁，正式提交时 Service 层有 FOR UPDATE 保护）
     */
    @PostMapping("/booking/check-conflict")
    public Result<Map<String, Boolean>> checkConflict(
            @RequestBody Map<String, Object> params) {
        try {
            Long roomId = Long.valueOf(params.get("roomId").toString());
            String startTimeStr = params.get("startTime").toString().replace("T", " ");
            String endTimeStr = params.get("endTime").toString().replace("T", " ");

            LocalDateTime startTime = LocalDateTime.parse(startTimeStr,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            LocalDateTime endTime = LocalDateTime.parse(endTimeStr,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

            boolean hasConflict = bookingService.hasConflict(roomId, startTime, endTime);

            Map<String, Boolean> result = new HashMap<>();
            result.put("conflict", hasConflict);
            return Result.ok(result);
        } catch (Exception e) {
            return Result.error("参数错误：" + e.getMessage());
        }
    }

    /**
     * 创建预约（AJAX 提交，返回 JSON）
     */
    @PostMapping("/booking/create")
    public Result<Void> createBooking(@RequestBody BookingForm form,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            if (form.getRoomId() == null) {
                return Result.error("请选择会议室");
            }
            if (form.getMeetingTitle() == null || form.getMeetingTitle().isBlank()) {
                return Result.error("会议主题不能为空");
            }
            if (form.getStartTime() == null || form.getEndTime() == null) {
                return Result.error("请选择会议时间");
            }
            if (!form.getEndTime().isAfter(form.getStartTime())) {
                return Result.error("结束时间必须晚于开始时间");
            }
            if (form.getAttendeeCount() == null || form.getAttendeeCount() < 1) {
                return Result.error("参会人数必须大于0");
            }

            Long userId = userDetails.getUser().getId();
            BookingRecord record = new BookingRecord();
            record.setRoomId(form.getRoomId());
            record.setMeetingTitle(form.getMeetingTitle());
            record.setStartTime(form.getStartTime());
            record.setEndTime(form.getEndTime());
            record.setAttendeeCount(form.getAttendeeCount());
            record.setRemark(form.getRemark());

            bookingService.submitBooking(record, userId);
            return Result.ok();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 取消预约（PENDING/APPROVED 均可取消，APPROVED 状态开始前30分钟截止）
     */
    @PutMapping("/booking/{id}/cancel")
    public Result<Void> cancelBooking(@PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            Long userId = userDetails.getUser().getId();
            bookingService.cancelBooking(id, userId);
            return Result.ok();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 签到（会议开始前后15分钟窗口）
     */
    @PutMapping("/booking/{id}/checkin")
    public Result<Void> checkIn(@PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            Long userId = userDetails.getUser().getId();
            bookingService.checkIn(id, userId);
            return Result.ok();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取会议室日程（用于 FullCalendar）
     */
    @GetMapping("/room/{id}/schedule")
    public Result<List<Map<String, Object>>> getRoomSchedule(
            @PathVariable Long id,
            @RequestParam String start,
            @RequestParam String end) {
        try {
            LocalDateTime startTime = LocalDateTime.parse(start.substring(0, 19));
            LocalDateTime endTime = LocalDateTime.parse(end.substring(0, 19));

            List<BookingRecord> bookings = bookingService.list(
                    new LambdaQueryWrapper<BookingRecord>()
                            .eq(BookingRecord::getRoomId, id)
                            .in(BookingRecord::getStatus,
                                    BookingStatus.APPROVED, BookingStatus.CHECKED_IN, BookingStatus.COMPLETED)
                            .and(wrapper -> wrapper
                                    .lt(BookingRecord::getStartTime, endTime)
                                    .gt(BookingRecord::getEndTime, startTime)));

            List<Map<String, Object>> events = bookings.stream()
                    .map(booking -> {
                        Map<String, Object> event = new HashMap<>();
                        event.put("id", booking.getId().toString());
                        event.put("title", booking.getMeetingTitle());
                        event.put("start", booking.getStartTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                        event.put("end", booking.getEndTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

                        switch (booking.getStatus()) {
                            case CHECKED_IN -> event.put("color", "#5FB878");
                            case COMPLETED -> event.put("color", "#999");
                            default -> event.put("color", "#1e9fff");
                        }
                        return event;
                    })
                    .collect(Collectors.toList());

            return Result.ok(events);
        } catch (Exception e) {
            return Result.error("获取日程失败：" + e.getMessage());
        }
    }

    /**
     * 审批通过
     */
    @PutMapping("/booking/{id}/approve")
    public Result<Void> approve(@PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody(required = false) Map<String, String> params) {
        try {
            Long approverId = userDetails.getUser().getId();
            String remark = params != null ? params.get("remark") : null;
            bookingService.approve(id, approverId, remark);
            return Result.ok();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 审批拒绝
     */
    @PutMapping("/booking/{id}/reject")
    public Result<Void> reject(@PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody(required = false) Map<String, String> params) {
        try {
            Long approverId = userDetails.getUser().getId();
            String remark = params != null ? params.get("remark") : null;
            bookingService.reject(id, approverId, remark);
            return Result.ok();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取待审批列表（管理员，JOIN 查询消除 N+1）
     */
    @GetMapping("/admin/bookings/pending")
    public Result<Map<String, Object>> getPendingBookings(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        try {
            IPage<BookingVO> bookingPage = bookingMapper.selectPendingBookingVOs(new Page<>(page, size));

            // 补充 statusText（枚举 → 中文）
            bookingPage.getRecords().forEach(vo -> {
                if (vo.getStatus() != null) {
                    vo.setStatusText(BookingStatus.valueOf(vo.getStatus()).getDisplayName());
                }
            });

            Map<String, Object> result = new HashMap<>();
            result.put("records", bookingPage.getRecords());
            result.put("current", bookingPage.getCurrent());
            result.put("pages", bookingPage.getPages());
            result.put("total", bookingPage.getTotal());

            return Result.ok(result);
        } catch (Exception e) {
            return Result.error("获取待审批列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取会议室实时状态（状态大屏，批量查询消除 N+1）
     */
    @GetMapping("/screen/rooms")
    public Result<List<Map<String, Object>>> getRoomStatus() {
        try {
            List<MeetingRoom> rooms = roomService.listAvailableRooms();
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime thirtyMinutesLater = now.plusMinutes(30);

            List<Map<String, Object>> roomStatusList = rooms.stream()
                    .map(room -> {
                        Map<String, Object> statusMap = new HashMap<>();
                        statusMap.put("id", room.getId());
                        statusMap.put("roomName", room.getRoomName());
                        statusMap.put("location", room.getLocation());
                        statusMap.put("capacity", room.getCapacity());

                        List<BookingRecord> currentMeetings = bookingService.list(
                                new LambdaQueryWrapper<BookingRecord>()
                                        .eq(BookingRecord::getRoomId, room.getId())
                                        .eq(BookingRecord::getStatus, BookingStatus.CHECKED_IN)
                                        .le(BookingRecord::getStartTime, now)
                                        .ge(BookingRecord::getEndTime, now));

                        List<BookingRecord> upcomingMeetings = bookingService.list(
                                new LambdaQueryWrapper<BookingRecord>()
                                        .eq(BookingRecord::getRoomId, room.getId())
                                        .eq(BookingRecord::getStatus, BookingStatus.APPROVED)
                                        .gt(BookingRecord::getStartTime, now)
                                        .le(BookingRecord::getStartTime, thirtyMinutesLater)
                                        .orderByAsc(BookingRecord::getStartTime)
                                        .last("LIMIT 1"));

                        if (!currentMeetings.isEmpty()) {
                            statusMap.put("status", "BUSY");
                            BookingRecord current = currentMeetings.get(0);
                            Map<String, Object> meetingInfo = new HashMap<>();
                            meetingInfo.put("title", current.getMeetingTitle());
                            meetingInfo.put("startTime", current.getStartTime());
                            meetingInfo.put("endTime", current.getEndTime());
                            statusMap.put("currentMeeting", meetingInfo);
                        } else if (!upcomingMeetings.isEmpty()) {
                            statusMap.put("status", "UPCOMING");
                            BookingRecord upcoming = upcomingMeetings.get(0);
                            Map<String, Object> meetingInfo = new HashMap<>();
                            meetingInfo.put("title", upcoming.getMeetingTitle());
                            meetingInfo.put("startTime", upcoming.getStartTime());
                            meetingInfo.put("endTime", upcoming.getEndTime());
                            statusMap.put("upcomingMeeting", meetingInfo);
                        } else {
                            statusMap.put("status", "FREE");
                        }

                        return statusMap;
                    })
                    .collect(Collectors.toList());

            return Result.ok(roomStatusList);
        } catch (Exception e) {
            return Result.error("获取会议室状态失败：" + e.getMessage());
        }
    }

    /**
     * 获取单个会议室详情（编辑用）
     */
    @GetMapping("/admin/room/{id}")
    public Result<Map<String, Object>> getRoomDetail(@PathVariable Long id) {
        try {
            MeetingRoom room = roomService.getRoomById(id);
            if (room == null) {
                return Result.error("会议室不存在");
            }
            Map<String, Object> map = new HashMap<>();
            map.put("id", room.getId());
            map.put("roomName", room.getRoomName());
            map.put("capacity", room.getCapacity());
            map.put("location", room.getLocation());
            map.put("facilities", room.getFacilities());
            map.put("status", room.getStatus());
            return Result.ok(map);
        } catch (Exception e) {
            return Result.error("获取会议室详情失败：" + e.getMessage());
        }
    }

    /**
     * 保存会议室（新增或编辑，AJAX 提交，返回 JSON）
     */
    @PostMapping("/admin/room/save")
    public Result<Void> saveRoom(@RequestBody Map<String, Object> params) {
        try {
            MeetingRoom room = new MeetingRoom();
            Object idObj = params.get("id");
            if (idObj != null && !idObj.toString().isEmpty()) {
                room.setId(Long.valueOf(idObj.toString()));
            }
            room.setRoomName((String) params.get("roomName"));
            Object capObj = params.get("capacity");
            if (capObj != null) {
                room.setCapacity(Integer.valueOf(capObj.toString()));
            }
            room.setLocation((String) params.get("location"));
            room.setFacilities((String) params.get("facilities"));
            Object statusObj = params.get("status");
            if (statusObj != null) {
                room.setStatus(Integer.valueOf(statusObj.toString()));
            }

            if (room.getId() == null) {
                roomService.addRoom(room);
            } else {
                roomService.updateRoom(room);
            }
            return Result.ok();
        } catch (Exception e) {
            return Result.error("保存失败：" + e.getMessage());
        }
    }

    /**
     * 删除会议室（AJAX 提交，返回 JSON）
     */
    @PostMapping("/admin/room/delete/{id}")
    public Result<Void> deleteRoom(@PathVariable Long id) {
        try {
            roomService.deleteRoom(id);
            return Result.ok();
        } catch (Exception e) {
            return Result.error("删除失败：" + e.getMessage());
        }
    }

    /**
     * 批量审批通过
     */
    @PutMapping("/admin/bookings/batch-approve")
    public Result<Void> batchApprove(@RequestBody Map<String, List<Long>> params,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            Long approverId = userDetails.getUser().getId();
            List<Long> ids = params.get("ids");
            if (ids == null || ids.isEmpty()) {
                return Result.error("请选择要审批的预约");
            }
            for (Long id : ids) {
                bookingService.approve(id, approverId, "批量审批通过");
            }
            return Result.ok();
        } catch (Exception e) {
            return Result.error("批量审批失败：" + e.getMessage());
        }
    }

    /**
     * 获取全部会议室列表（筛选下拉框）
     */
    @GetMapping("/admin/rooms")
    public Result<List<Map<String, Object>>> getAllRooms() {
        try {
            List<MeetingRoom> rooms = roomService.list(
                    new LambdaQueryWrapper<MeetingRoom>()
                            .orderByAsc(MeetingRoom::getId));
            List<Map<String, Object>> result = rooms.stream()
                    .map(room -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", room.getId());
                        map.put("roomName", room.getRoomName());
                        return map;
                    })
                    .collect(Collectors.toList());
            return Result.ok(result);
        } catch (Exception e) {
            return Result.error("获取会议室列表失败：" + e.getMessage());
        }
    }

    /**
     * 刷新仪表盘统计数据（修复 weeklyUtilizationRate 硬编码 0.0 问题）
     */
    @GetMapping("/admin/dashboard/refresh")
    public Result<Map<String, Object>> refreshDashboard() {
        try {
            Map<String, Object> data = new HashMap<>();
            LocalDateTime now = LocalDateTime.now();
            LocalDate today = now.toLocalDate();

            data.put("todayBookingCount", bookingService.count(
                    new LambdaQueryWrapper<BookingRecord>()
                            .apply("DATE(start_time) = {0}", today)));
            data.put("pendingApprovalCount", bookingService.count(
                    new LambdaQueryWrapper<BookingRecord>()
                            .eq(BookingRecord::getStatus, BookingStatus.PENDING)));

            long totalRoomCount = roomService.count(
                    new LambdaQueryWrapper<MeetingRoom>().eq(MeetingRoom::getStatus, 1));
            data.put("totalRoomCount", totalRoomCount);

            long todayShouldCheckIn = bookingService.count(
                    new LambdaQueryWrapper<BookingRecord>()
                            .apply("DATE(start_time) = {0}", today)
                            .in(BookingRecord::getStatus,
                                    BookingStatus.APPROVED, BookingStatus.CHECKED_IN, BookingStatus.COMPLETED));
            long todayCheckedIn = bookingService.count(
                    new LambdaQueryWrapper<BookingRecord>()
                            .apply("DATE(start_time) = {0}", today)
                            .in(BookingRecord::getStatus, BookingStatus.CHECKED_IN, BookingStatus.COMPLETED));
            data.put("todayCheckInRate", todayShouldCheckIn > 0
                    ? (double) todayCheckedIn / todayShouldCheckIn * 100 : 0);

            // 本周利用率（修复硬编码 0.0，改用 SQL 计算）
            LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
            Long bookedHours = bookingMapper.sumBookedHoursInWeek(
                    weekStart.atTime(LocalTime.MIN), weekEnd.atTime(LocalTime.MAX));
            long totalAvailableHours = totalRoomCount * 7 * 14;
            double weeklyUtilizationRate = totalAvailableHours > 0
                    ? (double) (bookedHours != null ? bookedHours : 0L) / totalAvailableHours * 100 : 0;
            data.put("weeklyUtilizationRate", weeklyUtilizationRate);

            return Result.ok(data);
        } catch (Exception e) {
            return Result.error("刷新失败：" + e.getMessage());
        }
    }

    /**
     * 获取全部预约列表（管理员，JOIN 查询消除 N+1）
     */
    @GetMapping("/admin/bookings")
    public Result<Map<String, Object>> getAllBookings(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long roomId) {
        try {
            IPage<BookingVO> bookingPage = bookingMapper.selectAllBookingVOs(
                    new Page<>(page, size), status, roomId);

            // 补充 statusText
            bookingPage.getRecords().forEach(vo -> {
                if (vo.getStatus() != null) {
                    try {
                        vo.setStatusText(BookingStatus.valueOf(vo.getStatus()).getDisplayName());
                    } catch (IllegalArgumentException ignored) {
                        vo.setStatusText(vo.getStatus());
                    }
                }
            });

            Map<String, Object> result = new HashMap<>();
            result.put("records", bookingPage.getRecords());
            result.put("current", bookingPage.getCurrent());
            result.put("pages", bookingPage.getPages());
            result.put("total", bookingPage.getTotal());

            return Result.ok(result);
        } catch (Exception e) {
            return Result.error("获取预约列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取单个预约详情（管理员）
     */
    @GetMapping("/admin/booking/{id}")
    public Result<Map<String, Object>> getBookingDetail(@PathVariable Long id) {
        try {
            BookingRecord record = bookingService.getById(id);
            if (record == null) {
                return Result.error("预约不存在");
            }

            BookingVO vo = BookingVO.fromEntity(record);
            MeetingRoom room = roomService.getRoomById(record.getRoomId());
            if (room != null) vo.setRoomName(room.getRoomName());
            SysUser user = userService.getById(record.getUserId());
            if (user != null) vo.setUserName(user.getUsername());

            Map<String, Object> map = new HashMap<>();
            map.put("id", vo.getId());
            map.put("roomId", vo.getRoomId());
            map.put("roomName", vo.getRoomName());
            map.put("userId", vo.getUserId());
            map.put("userName", vo.getUserName());
            map.put("meetingTitle", vo.getMeetingTitle());
            map.put("startTime", vo.getStartTime());
            map.put("endTime", vo.getEndTime());
            map.put("attendeeCount", vo.getAttendeeCount());
            map.put("remark", vo.getRemark());
            map.put("status", vo.getStatus());
            map.put("statusText", vo.getStatusText());
            map.put("approveRemark", vo.getApproveRemark());
            map.put("approveTime", vo.getApproveTime());
            map.put("checkInTime", vo.getCheckInTime());
            map.put("createTime", vo.getCreateTime());

            return Result.ok(map);
        } catch (Exception e) {
            return Result.error("获取预约详情失败：" + e.getMessage());
        }
    }

    /**
     * 获取用户列表（管理员，支持搜索）
     */
    @GetMapping("/admin/users")
    public Result<Map<String, Object>> getUsers(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword) {
        try {
            LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                    .orderByDesc(SysUser::getCreateTime);
            if (keyword != null && !keyword.isEmpty()) {
                wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                        .or().like(SysUser::getRealName, keyword)
                        .or().like(SysUser::getPhone, keyword));
            }

            Page<SysUser> userPage = userService.page(new Page<>(page, size), wrapper);

            Map<String, Object> result = new HashMap<>();
            result.put("records", userPage.getRecords());
            result.put("current", userPage.getCurrent());
            result.put("pages", userPage.getPages());
            result.put("total", userPage.getTotal());

            return Result.ok(result);
        } catch (Exception e) {
            return Result.error("获取用户列表失败：" + e.getMessage());
        }
    }

    /**
     * 切换用户状态（启用/禁用）
     */
    @PutMapping("/admin/users/{id}/status")
    public Result<Void> updateUserStatus(@PathVariable Long id,
            @RequestBody Map<String, Integer> params) {
        try {
            Integer status = params.get("status");
            SysUser user = userService.getById(id);
            if (user == null) {
                return Result.error("用户不存在");
            }
            if ("ADMIN".equals(user.getRole())) {
                return Result.error("不能修改管理员状态");
            }
            user.setStatus(status);
            userService.updateById(user);
            return Result.ok();
        } catch (Exception e) {
            return Result.error("操作失败：" + e.getMessage());
        }
    }
}
