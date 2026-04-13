package com.haru_backend.controller;

import com.haru_backend.dto.request.AnswerRequest;
import com.haru_backend.dto.response.AnswerHistoryResponse;
import com.haru_backend.dto.response.AnswerResponse;
import com.haru_backend.dto.response.ApiResponse;
import com.haru_backend.dto.response.QuestionDetailResponse;
import com.haru_backend.service.AnswerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/answers")
@RequiredArgsConstructor
public class AnswerController {

    private final AnswerService answerService;

    @GetMapping("/question")
    public ResponseEntity<ApiResponse<QuestionDetailResponse>> getQuestionByToken(@RequestParam String token) {
        QuestionDetailResponse data = answerService.getQuestionByToken(token);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AnswerResponse>> submitAnswer(
            Authentication authentication,
            @Valid @RequestBody AnswerRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        AnswerResponse data = answerService.submitAnswer(userId, request);
        return ResponseEntity.ok(ApiResponse.success(data, "답변 제출 성공"));
    }

    @GetMapping("/delivery/{deliveryId}")
    public ResponseEntity<ApiResponse<List<AnswerResponse>>> getAnswers(@PathVariable Long deliveryId) {
        List<AnswerResponse> data = answerService.getAnswersByDeliveryId(deliveryId);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<AnswerHistoryResponse>>> getMyAnswers(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<AnswerHistoryResponse> data = answerService.getMyAnswers(userId);
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
