package org.example.roomsched.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.roomsched.dto.AiBookingRequest;
import org.example.roomsched.dto.AiBookingResponse;
import org.example.roomsched.dto.AiChatResponse;
import org.example.roomsched.entity.MeetingRoom;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    @Value("${ai.base-url}")
    private String baseUrl;

    @Value("${ai.api-key}")
    private String apiKey;

    @Value("${ai.model}")
    private String model;

    private final ObjectMapper objectMapper;
    private final RoomService roomService;
    private final BookingService bookingService;
    private final RestClient restClient = RestClient.builder().build();

    public AiBookingResponse parseBooking(AiBookingRequest request) {
        String text = request.getText();
        if (text == null || text.trim().isEmpty()) {
            throw new RuntimeException("输入内容不能为空");
        }

        AiBookingResponse parsedData = callAiModel(text);

        // 寻找推荐会议室
        MeetingRoom recommendedRoom = findBestRoom(parsedData);
        if (recommendedRoom != null) {
            parsedData.setRecommendedRoom(recommendedRoom);
            parsedData.setMessage("解析成功，已为您推荐最合适的会议室");
        } else {
            parsedData.setMessage("解析成功，但没有找到匹配的空闲会议室");
        }

        return parsedData;
    }

    private AiBookingResponse callAiModel(String userInput) {
        if (apiKey == null || apiKey.contains("your-api-key")) {
            log.warn("未配置真实的 AI API Key，使用 Mock 数据返回。");
            return mockResponse();
        }

        try {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String sysPrompt = String.format(
                    "你是一个会议室调度助手。现在的系统时间是 %s。" +
                    "请将用户的自然语言转化为严格的 JSON 数据。不要返回任何其他内容，不要使用 markdown 格式包裹（比如不要有 ```json）。" +
                    "必需字段如下：" +
                    "meetingTitle: 会议主题字符串（如果没说，默认生成个合适的），" +
                    "startTime: 开始时间字符串(yyyy-MM-dd HH:mm:ss)，" +
                    "endTime: 结束时间字符串(yyyy-MM-dd HH:mm:ss，如果没说时长，默认1小时)，" +
                    "attendeeCount: 整数（如果没说，默认2人）。", now.format(formatter)
            );

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("temperature", 0.0);
            
            Map<String, String> sysMsg = new HashMap<>();
            sysMsg.put("role", "system");
            sysMsg.put("content", sysPrompt);

            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userInput);

            requestBody.put("messages", List.of(sysMsg, userMsg));

            String responseJson = restClient.post()
                    .uri(baseUrl + "/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            AiChatResponse chatResponse = objectMapper.readValue(responseJson, AiChatResponse.class);
            if (chatResponse != null && chatResponse.getChoices() != null && !chatResponse.getChoices().isEmpty()) {
                String content = chatResponse.getChoices().get(0).getMessage().getContent();
                content = content.replace("```json", "").replace("```", "").trim();
                return objectMapper.readValue(content, AiBookingResponse.class);
            }
            throw new RuntimeException("大模型返回数据为空");
        } catch (Exception e) {
            log.error("调用 AI 解析异常", e);
            throw new RuntimeException("AI 解析失败，请重试或手动预定。");
        }
    }

    private MeetingRoom findBestRoom(AiBookingResponse parsedData) {
        if (parsedData.getStartTime() == null || parsedData.getEndTime() == null || parsedData.getAttendeeCount() == null) {
            return null;
        }
        
        List<MeetingRoom> rooms = roomService.list(
                new LambdaQueryWrapper<MeetingRoom>()
                        .eq(MeetingRoom::getStatus, 1)
                        .ge(MeetingRoom::getCapacity, parsedData.getAttendeeCount())
                        .orderByAsc(MeetingRoom::getCapacity) // 尽量找最小且满足条件的房间
        );

        for (MeetingRoom room : rooms) {
            boolean conflict = bookingService.hasConflict(room.getId(), parsedData.getStartTime(), parsedData.getEndTime());
            if (!conflict) {
                return room;
            }
        }
        return null;
    }

    private AiBookingResponse mockResponse() {
        AiBookingResponse mock = new AiBookingResponse();
        mock.setMeetingTitle("需求讨论会 (AI 模拟)");
        LocalDateTime now = LocalDateTime.now();
        // 模拟安排在明天下午两点
        LocalDateTime startTime = now.plusDays(1).withHour(14).withMinute(0).withSecond(0).withNano(0);
        mock.setStartTime(startTime);
        mock.setEndTime(startTime.plusHours(1));
        mock.setAttendeeCount(5);
        return mock;
    }
}
