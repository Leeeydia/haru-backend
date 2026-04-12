package com.haru_backend.controller;

import com.haru_backend.domain.QuestionDelivery;
import com.haru_backend.domain.User;
import com.haru_backend.dto.request.QuestionGenerateRequest;
import com.haru_backend.dto.request.QuestionRequest;
import com.haru_backend.dto.response.ApiResponse;
import com.haru_backend.dto.response.QuestionResponse;
import com.haru_backend.mapper.QuestionDeliveryMapper;
import com.haru_backend.mapper.UserMapper;
import com.haru_backend.service.MailService;
import com.haru_backend.service.QuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;
    private final UserMapper userMapper;
    private final QuestionDeliveryMapper questionDeliveryMapper;
    private final MailService mailService;

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

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<List<QuestionResponse>>> generateQuestions(
            Authentication authentication,
            @Valid @RequestBody QuestionGenerateRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        User user = userMapper.findById(userId);

        List<QuestionResponse> data = questionService.generateQuestions(request);

        // 첫 번째 생성된 질문을 이메일로 발송
        if (!data.isEmpty()) {
            QuestionResponse first = data.get(0);
            String answerToken = UUID.randomUUID().toString();

            QuestionDelivery delivery = QuestionDelivery.builder()
                    .userId(userId)
                    .questionId(first.getId())
                    .answerToken(answerToken)
                    .build();
            questionDeliveryMapper.insertDelivery(delivery);

            mailService.sendQuestionEmail(user.getEmail(), first.getContent(), first.getCategory(), answerToken);
        }

        return ResponseEntity.ok(ApiResponse.success(data, "AI 질문 생성 및 이메일 발송 완료"));
    }
}
