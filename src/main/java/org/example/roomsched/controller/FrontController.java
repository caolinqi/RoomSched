package org.example.roomsched.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.roomsched.dto.BookingForm;
import org.example.roomsched.entity.BookingRecord;
import org.example.roomsched.entity.MeetingRoom;
import org.example.roomsched.security.CustomUserDetails;
import org.example.roomsched.service.BookingService;
import org.example.roomsched.service.RoomService;
import org.example.roomsched.vo.BookingVO;
import org.example.roomsched.vo.RoomVO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户端页面控制器
 * 处理普通用户的页面请求
 */
@Controller
@RequiredArgsConstructor
public class FrontController {

    private final RoomService roomService;
    private final BookingService bookingService;
    private final org.example.roomsched.service.UserService userService;
    private final org.example.roomsched.service.UserDeviceService userDeviceService;

    /**
     * 首页
     */
    @GetMapping("/")
    public String index(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        Long userId = userDetails.getUser().getId();

        // 查询用户今日预约并转为 VO（填充会议室名称）
        List<BookingRecord> todayRecords = bookingService.list(
                new LambdaQueryWrapper<BookingRecord>()
                        .eq(BookingRecord::getUserId, userId)
                        .apply("DATE(start_time) = CURDATE()")
                        .orderByAsc(BookingRecord::getStartTime));

        Set<Long> roomIds = todayRecords.stream()
                .map(BookingRecord::getRoomId)
                .collect(Collectors.toSet());
        Map<Long, String> roomNameMap = roomIds.isEmpty()
                ? Map.of()
                : roomService.listByIds(roomIds).stream()
                        .collect(Collectors.toMap(MeetingRoom::getId, MeetingRoom::getRoomName));

        List<BookingVO> todayBookings = todayRecords.stream()
                .map(record -> {
                    BookingVO vo = BookingVO.fromEntity(record);
                    vo.setRoomName(roomNameMap.getOrDefault(record.getRoomId(),
                            "会议室" + record.getRoomId()));
                    return vo;
                })
                .collect(Collectors.toList());

        // 查询可用会议室数量
        long availableRoomCount = roomService.count(
                new LambdaQueryWrapper<MeetingRoom>()
                        .eq(MeetingRoom::getStatus, 1));

        model.addAttribute("todayBookings", todayBookings);
        model.addAttribute("availableRoomCount", availableRoomCount);
        return "front/index";
    }

    /**
     * 会议室列表
     */
    @GetMapping("/rooms")
    public String roomList(Model model) {
        List<MeetingRoom> rooms = roomService.listAvailableRooms();
        List<RoomVO> roomVOList = rooms.stream()
                .map(RoomVO::fromEntity)
                .collect(Collectors.toList());
        model.addAttribute("rooms", roomVOList);
        model.addAttribute("roomVOList", roomVOList);
        return "front/room-list";
    }

    /**
     * 会议室详情
     */
    @GetMapping("/room/{id}")
    public String roomDetail(@PathVariable Long id, Model model) {
        MeetingRoom room = roomService.getRoomById(id);
        if (room == null) {
            return "redirect:/rooms";
        }
        RoomVO roomVO = RoomVO.fromEntity(room);
        model.addAttribute("roomVO", roomVO);
        return "front/room-detail";
    }

    /**
     * 预约申请页面
     */
    @GetMapping("/booking/new")
    public String bookingForm(@RequestParam Long roomId, Model model) {
        MeetingRoom room = roomService.getRoomById(roomId);
        if (room == null) {
            return "redirect:/rooms";
        }
        RoomVO roomVO = RoomVO.fromEntity(room);
        model.addAttribute("roomVO", roomVO);
        return "front/booking-form";
    }

    /**
     * 提交预约
     */
    @PostMapping("/booking")
    public String submitBooking(@Valid BookingForm form,
            BindingResult bindingResult,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        if (form.getRoomId() == null) {
            redirectAttributes.addFlashAttribute("error", "请选择会议室");
            return "redirect:/rooms";
        }
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/booking/new?roomId=" + form.getRoomId();
        }
        try {
            Long userId = userDetails.getUser().getId();
            BookingRecord record = new BookingRecord();
            record.setRoomId(form.getRoomId());
            record.setMeetingTitle(form.getMeetingTitle());
            record.setStartTime(form.getStartTime());
            record.setEndTime(form.getEndTime());
            record.setAttendeeCount(form.getAttendeeCount());
            record.setRemark(form.getRemark());

            bookingService.submitBooking(record, userId);
            redirectAttributes.addFlashAttribute("success", "预约提交成功，等待管理员审批");
            return "redirect:/my-booking";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/booking/new?roomId=" + form.getRoomId();
        }
    }

    /**
     * 我的预约
     */
    @GetMapping("/my-booking")
    public String myBooking(@RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {
        Long userId = userDetails.getUser().getId();
        Page<BookingRecord> bookingPage = bookingService.pageByUserId(userId, page, size);

        // 转换为 VO 并填充会议室名称
        List<BookingVO> bookingVOList = bookingPage.getRecords().stream()
                .map(record -> {
                    BookingVO vo = BookingVO.fromEntity(record);
                    MeetingRoom room = roomService.getRoomById(record.getRoomId());
                    if (room != null) {
                        vo.setRoomName(room.getRoomName());
                    }
                    return vo;
                })
                .collect(Collectors.toList());

        Page<BookingVO> pageResult = new Page<>(page, size);
        pageResult.setRecords(bookingVOList);
        pageResult.setTotal(bookingPage.getTotal());
        pageResult.setCurrent(bookingPage.getCurrent());
        pageResult.setPages(bookingPage.getPages());
        pageResult.setSize(bookingPage.getSize());

        model.addAttribute("page", pageResult);
        model.addAttribute("bookingPage", bookingPage);
        model.addAttribute("bookingVOList", bookingVOList);
        return "front/my-booking";
    }

    /**
     * 状态大屏（无需登录）
     */
    @GetMapping("/screen")
    public String screen() {
        return "front/screen";
    }

    /**
     * 个人中心
     */
    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails != null && userDetails.getUser() != null) {
            Long userId = userDetails.getUser().getId();
            
            // 获取最新数据库用户信息，以获取新加的字段（头像、时区、偏好）
            org.example.roomsched.entity.SysUser dbUser = userService.getById(userId);
            
            String name = dbUser.getRealName();
            if (name == null || name.isEmpty()) {
                name = dbUser.getUsername();
            }
            model.addAttribute("userName", name);
            model.addAttribute("userInitials", name != null && name.length() > 0 ? name.substring(0, 1).toUpperCase() : "U");
            model.addAttribute("userRole", dbUser.getRole().replace("ROLE_", ""));
            
            // 确保如果 email 为空但 username 是邮箱格式，则渲染 username
            String displayEmail = dbUser.getEmail();
            if ((displayEmail == null || displayEmail.trim().isEmpty()) && 
                dbUser.getUsername() != null && dbUser.getUsername().contains("@")) {
                displayEmail = dbUser.getUsername();
            }
            model.addAttribute("userEmail", displayEmail);
            model.addAttribute("userPhone", dbUser.getPhone());
            model.addAttribute("userDept", "系统用户"); // 暂无部门字段
            
            // 新增扩展字段
            model.addAttribute("userAvatar", dbUser.getAvatarUrl());
            model.addAttribute("emailNotify", dbUser.getEmailNotify() != null && dbUser.getEmailNotify() == 1);
            model.addAttribute("userTimezone", dbUser.getTimezone() != null ? dbUser.getTimezone() : "Asia/Shanghai");
            
            // 查询登录设备
            List<org.example.roomsched.entity.UserDevice> devices = userDeviceService.list(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<org.example.roomsched.entity.UserDevice>()
                            .eq(org.example.roomsched.entity.UserDevice::getUserId, userId)
                            .orderByDesc(org.example.roomsched.entity.UserDevice::getLastActiveTime)
            );
            model.addAttribute("userDevices", devices);
            
            // 1. 获取我的日程（即将到来的5个预约）
            List<BookingRecord> upcomingRecords = bookingService.list(
                    new LambdaQueryWrapper<BookingRecord>()
                            .eq(BookingRecord::getUserId, userId)
                            .in(BookingRecord::getStatus, org.example.roomsched.entity.BookingStatus.PENDING, org.example.roomsched.entity.BookingStatus.APPROVED)
                            .orderByAsc(BookingRecord::getStartTime)
                            .last("LIMIT 5"));
            
            Set<Long> roomIds = upcomingRecords.stream().map(BookingRecord::getRoomId).collect(Collectors.toSet());
            Map<Long, String> roomMap = roomIds.isEmpty() ? Map.of() : 
                    roomService.listByIds(roomIds).stream().collect(Collectors.toMap(MeetingRoom::getId, MeetingRoom::getRoomName));
            
            List<BookingVO> upcomingBookings = upcomingRecords.stream().map(record -> {
                BookingVO vo = BookingVO.fromEntity(record);
                vo.setRoomName(roomMap.getOrDefault(record.getRoomId(), "会议室" + record.getRoomId()));
                return vo;
            }).collect(Collectors.toList());
            model.addAttribute("upcomingBookings", upcomingBookings);

            // 2. 统计所有历史记录
            List<BookingRecord> allRecords = bookingService.list(
                    new LambdaQueryWrapper<BookingRecord>().eq(BookingRecord::getUserId, userId)
            );

            // 计算本月会议时长 (小时)
            int currentMonth = java.time.LocalDate.now().getMonthValue();
            int currentYear = java.time.LocalDate.now().getYear();
            double monthHours = allRecords.stream()
                .filter(r -> (r.getStatus() == org.example.roomsched.entity.BookingStatus.COMPLETED || r.getStatus() == org.example.roomsched.entity.BookingStatus.CHECKED_IN))
                .filter(r -> r.getStartTime().getMonthValue() == currentMonth && r.getStartTime().getYear() == currentYear)
                .mapToDouble(r -> java.time.Duration.between(r.getStartTime(), r.getEndTime()).toMinutes() / 60.0)
                .sum();
            
            // 计算违约/取消次数
            long lateCancelCount = allRecords.stream()
                .filter(r -> r.getStatus() == org.example.roomsched.entity.BookingStatus.CANCELLED || r.getStatus() == org.example.roomsched.entity.BookingStatus.AUTO_RELEASED)
                .count();

            model.addAttribute("statTotalBookings", allRecords.size());
            model.addAttribute("statMonthHours", String.format("%.1f", monthHours));
            model.addAttribute("statLateCancel", lateCancelCount);
            
            // 信誉分
            long creditScore = Math.max(0, 100 - (lateCancelCount * 5));
            model.addAttribute("creditScore", creditScore);
            
            // 常用会议室偏好
            Map<Long, Long> roomUsageCount = allRecords.stream()
                .collect(Collectors.groupingBy(BookingRecord::getRoomId, Collectors.counting()));
            
            List<Map<String, Object>> roomPreferences = roomUsageCount.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(3)
                .map(entry -> {
                    MeetingRoom room = roomService.getRoomById(entry.getKey());
                    long percentage = allRecords.isEmpty() ? 0 : (entry.getValue() * 100 / allRecords.size());
                    return Map.<String, Object>of(
                        "roomName", room != null ? room.getRoomName() : "会议室" + entry.getKey(),
                        "percentage", percentage
                    );
                })
                .collect(Collectors.toList());
            
            model.addAttribute("roomPreferences", roomPreferences);
        }
        return "front/profile";
    }

    /**
     * 个人中心 - 修改密码
     */
    @PostMapping("/profile/password")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> updatePassword(
            @RequestParam String oldPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        if (!newPassword.equals(confirmPassword)) {
            return org.springframework.http.ResponseEntity.badRequest().body(Map.of("message", "两次输入的新密码不一致"));
        }
        try {
            Long userId = userDetails.getUser().getId();
            userService.updatePassword(userId, oldPassword, newPassword);
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
        return org.springframework.http.ResponseEntity.ok(Map.of("message", "密码修改成功，请重新登录"));
    }
}
