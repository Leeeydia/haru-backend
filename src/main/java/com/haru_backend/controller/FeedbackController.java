package com.haru_backend.controller;

import com.haru_backend.dto.response.ApiResponse;
import com.haru_backend.dto.response.FeedbackResponse;
import com.haru_backend.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping("/{answerId}")
    public ResponseEntity<ApiResponse<FeedbackResponse>> generateFeedback(@PathVariable Long answerId) {
        FeedbackResponse data = feedbackService.generateFeedback(answerId);
        return ResponseEntity.ok(ApiResponse.success(data, "AI 피드백 생성 성공"));
    }

    @GetMapping("/{answerId}")
    public ResponseEntity<ApiResponse<FeedbackResponse>> getFeedback(
            Authentication authentication,
            @PathVariable Long answerId) {
        Long userId = (Long) authentication.getPrincipal();
        FeedbackResponse data = feedbackService.getFeedback(answerId, userId);
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
