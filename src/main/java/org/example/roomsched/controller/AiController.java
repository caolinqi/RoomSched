package org.example.roomsched.controller;

import lombok.RequiredArgsConstructor;
import org.example.roomsched.dto.AiBookingRequest;
import org.example.roomsched.dto.AiBookingResponse;
import org.example.roomsched.service.AiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/parse")
    public ResponseEntity<AiBookingResponse> parseBooking(@RequestBody AiBookingRequest request) {
        try {
            AiBookingResponse response = aiService.parseBooking(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            AiBookingResponse errorResponse = new AiBookingResponse();
            errorResponse.setMessage(e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}
