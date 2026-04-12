package com.haru_backend.controller;

import com.haru_backend.domain.Question;
import com.haru_backend.domain.QuestionDelivery;
import com.haru_backend.domain.User;
import com.haru_backend.dto.response.ApiResponse;
import com.haru_backend.mapper.QuestionDeliveryMapper;
import com.haru_backend.mapper.QuestionMapper;
import com.haru_backend.mapper.UserMapper;
import com.haru_backend.service.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private final UserMapper userMapper;
    private final QuestionMapper questionMapper;
    private final QuestionDeliveryMapper questionDeliveryMapper;
    private final MailService mailService;

    @GetMapping("/send-question")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendTestQuestion(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        User user = userMapper.findById(userId);

        List<Question> questions = questionMapper.findAll();
        if (questions.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error("question 테이블에 데이터가 없습니다"));
        }

        Question question = questions.get(0);
        String answerToken = UUID.randomUUID().toString();

        QuestionDelivery delivery = QuestionDelivery.builder()
                .userId(userId)
                .questionId(question.getId())
                .answerToken(answerToken)
                .build();
        questionDeliveryMapper.insertDelivery(delivery);

        mailService.sendQuestionEmail(user.getEmail(), question.getContent(), question.getCategory(), answerToken);

        return ResponseEntity.ok(ApiResponse.success(
                Map.of("email", user.getEmail(), "questionId", question.getId(), "answerToken", answerToken),
                "테스트 이메일 발송 완료"));
    }
}
