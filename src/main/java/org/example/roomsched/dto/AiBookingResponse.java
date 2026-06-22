package org.example.roomsched.dto;

import lombok.Data;
import org.example.roomsched.entity.MeetingRoom;
import java.time.LocalDateTime;

@Data
public class AiBookingResponse {
    // 解析出的信息
    private String meetingTitle;
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
    private Integer attendeeCount;

    // 系统推荐的房间
    private MeetingRoom recommendedRoom;
    
    // 如果没有找到房间，返回提示信息
    private String message;
}
