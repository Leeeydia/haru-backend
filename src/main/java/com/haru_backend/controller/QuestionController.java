package com.haru_backend.controller;

import com.haru_backend.dto.request.QuestionRequest;
import com.haru_backend.dto.response.ApiResponse;
import com.haru_backend.dto.response.QuestionResponse;
import com.haru_backend.service.QuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @PostMapping
    public ResponseEntity<ApiResponse<QuestionResponse>> createQuestion(
            @Valid @RequestBody QuestionRequest request) {
        QuestionResponse data = questionService.createQuestion(request);
        return ResponseEntity.ok(ApiResponse.success(data, "질문 등록 성공"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<QuestionResponse>>> getQuestions(
            @RequestParam(required = false) String category) {
        List<QuestionResponse> data = questionService.getQuestions(category);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<QuestionResponse>> getQuestion(@PathVariable Long id) {
        QuestionResponse data = questionService.getQuestionById(id);
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
