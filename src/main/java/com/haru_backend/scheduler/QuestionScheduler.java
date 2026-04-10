package com.haru_backend.scheduler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.haru_backend.domain.Question;
import com.haru_backend.domain.QuestionDelivery;
import com.haru_backend.domain.UserProfile;
import com.haru_backend.mapper.QuestionDeliveryMapper;
import com.haru_backend.mapper.QuestionMapper;
import com.haru_backend.mapper.UserMapper;
import com.haru_backend.mapper.UserProfileMapper;
import com.haru_backend.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuestionScheduler {

    private final UserProfileMapper userProfileMapper;
    private final UserMapper userMapper;
    private final QuestionMapper questionMapper;
    private final QuestionDeliveryMapper questionDeliveryMapper;
    private final MailService mailService;
    private final ObjectMapper objectMapper;

    @Scheduled(cron = "0 0 * * * *")
    public void sendDailyQuestions() {
        int currentHour = LocalDateTime.now().getHour();
        log.debug("질문 발송 스케줄러 실행: {}시", currentHour);

        List<UserProfile> profiles = userProfileMapper.findByReceiveTime(currentHour);

        for (UserProfile profile : profiles) {
            try {
                if (!shouldSendToday(profile.getReceiveDays())) {
                    continue;
                }

                var user = userMapper.findById(profile.getUserId());
                if (user == null) {
                    continue;
                }

                List<String> stacks = parseJson(profile.getTechStacks());
                List<Long> deliveredIds = questionDeliveryMapper.findDeliveredQuestionIds(profile.getUserId());
                int count = profile.getDailyQuestionCount() != null ? profile.getDailyQuestionCount() : 1;

                List<Question> questions = questionMapper.findAvailableQuestions(
                        profile.getJobCategory(), stacks, deliveredIds, count);

                for (Question question : questions) {
                    String answerToken = UUID.randomUUID().toString();

                    QuestionDelivery delivery = QuestionDelivery.builder()
                            .userId(profile.getUserId())
                            .questionId(question.getId())
                            .answerToken(answerToken)
                            .build();
                    questionDeliveryMapper.insertDelivery(delivery);

                    mailService.sendQuestionEmail(
                            user.getEmail(), question.getContent(), question.getCategory(), answerToken);

                    log.debug("질문 발송 완료: userId={}, questionId={}", profile.getUserId(), question.getId());
                }
            } catch (Exception e) {
                log.error("질문 발송 실패: userId={}", profile.getUserId(), e);
            }
        }
    }

    private boolean shouldSendToday(String receiveDays) {
        if ("EVERYDAY".equals(receiveDays)) {
            return true;
        }
        DayOfWeek day = LocalDateTime.now().getDayOfWeek();
        return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;
    }

    private List<String> parseJson(String json) {
        try {
            if (json == null || json.isBlank()) {
                return Collections.emptyList();
            }
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
