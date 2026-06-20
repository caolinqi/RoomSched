package org.example.roomsched.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class BookingForm {

    @NotNull(message = "请选择会议室")
    private Long roomId;

    @NotBlank(message = "会议主题不能为空")
    @Size(max = 100, message = "会议主题不能超过100个字符")
    private String meetingTitle;

    @NotNull(message = "开始时间不能为空")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime endTime;

    @NotNull(message = "参会人数不能为空")
    @Min(value = 1, message = "参会人数必须大于0")
    private Integer attendeeCount;

    @Size(max = 500, message = "备注不能超过500个字符")
    private String remark;
}
