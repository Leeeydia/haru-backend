package com.haru_backend.controller;

import com.haru_backend.domain.Question;
import com.haru_backend.domain.QuestionDelivery;
import com.haru_backend.domain.User;
import com.haru_backend.dto.response.ApiResponse;
import com.haru_backend.mapper.QuestionDeliveryMapper;
import com.haru_backend.mapper.QuestionMapper;
import com.haru_backend.mapper.UserMapper;
import com.haru_backend.service.GitHubService;
import com.haru_backend.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private final UserMapper userMapper;
    private final QuestionMapper questionMapper;
    private final QuestionDeliveryMapper questionDeliveryMapper;
    private final MailService mailService;
    private final GitHubService gitHubService;

    @GetMapping("/send-question")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendTestQuestion(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        User user = userMapper.findById(userId);

        // 질문이 없으면 테스트 질문 3개 자동 INSERT
        List<Question> questions = questionMapper.findAll();
        if (questions.isEmpty()) {
            insertTestQuestions();
            questions = questionMapper.findAll();
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

        log.info("테스트 이메일 발송: userId={}, email={}, questionId={}", userId, user.getEmail(), question.getId());

        return ResponseEntity.ok(ApiResponse.success(
                Map.of("userId", userId,
                        "email", user.getEmail(),
                        "questionId", question.getId(),
                        "answerToken", answerToken),
                "테스트 이메일 발송 완료"));
    }

    @GetMapping("/update-readme")
    public ResponseEntity<ApiResponse<String>> testUpdateReadme(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        gitHubService.updateReadmeForUser(userId);
        return ResponseEntity.ok(ApiResponse.success("README 업데이트 완료"));
    }

    private void insertTestQuestions() {
        questionMapper.insertQuestion(Question.builder()
                .content("Spring Boot에서 의존성 주입(DI)의 세 가지 방식(생성자, 세터, 필드 주입)의 차이점과 각각의 장단점을 설명해주세요.")
                .category("백엔드")
                .difficulty("중")
                .relatedStacks("[\"Spring\",\"Java\"]")
                .answerKeywords("생성자 주입, 불변성, 순환 참조, @Autowired, final")
                .build());

        questionMapper.insertQuestion(Question.builder()
                .content("HTTP 상태 코드 중 2xx, 3xx, 4xx, 5xx 각 범주의 의미와 대표적인 코드를 설명해주세요.")
                .category("CS")
                .difficulty("하")
                .relatedStacks("[\"공통\"]")
                .answerKeywords("200 OK, 301 Redirect, 404 Not Found, 500 Internal Server Error, REST")
                .build());

        questionMapper.insertQuestion(Question.builder()
                .content("데이터베이스 인덱스(Index)의 동작 원리와 B-Tree 인덱스의 구조를 설명하고, 인덱스를 사용할 때 주의할 점을 말해주세요.")
                .category("백엔드")
                .difficulty("상")
                .relatedStacks("[\"MySQL\",\"DB\"]")
                .answerKeywords("B-Tree, 탐색 시간 복잡도, 쓰기 성능, 카디널리티, 복합 인덱스")
                .build());

        log.info("테스트 질문 3개 자동 INSERT 완료");
    }
}
