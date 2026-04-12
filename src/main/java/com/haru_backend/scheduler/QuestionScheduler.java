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
import com.haru_backend.service.QuestionService;
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
    private final QuestionService questionService;
    private final ObjectMapper objectMapper;

    // 테스트용: @Scheduled(cron = "0 */1 * * * *") — 1분마다 실행
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

                int count = profile.getDailyQuestionCount() != null ? profile.getDailyQuestionCount() : 1;

                // 매시간 발송(-1) 사용자: 오늘 이미 발송했으면 스킵
                if (profile.getReceiveTime() != null && profile.getReceiveTime() == -1) {
                    int todayCount = questionDeliveryMapper.countTodayDeliveries(profile.getUserId());
                    if (todayCount >= count) {
                        continue;
                    }
                }

                List<String> stacks = parseJson(profile.getTechStacks());
                List<Long> deliveredIds = questionDeliveryMapper.findDeliveredQuestionIds(profile.getUserId());

                List<Question> questions = questionMapper.findAvailableQuestions(
                        profile.getJobCategory(), stacks, deliveredIds, count);

                // 보낼 질문이 부족하면 AI로 자동 생성
                if (questions.size() < count) {
                    log.info("질문 부족 감지: userId={}, 필요={}, 보유={}, AI 생성 시작",
                            profile.getUserId(), count, questions.size());
                    try {
                        questionService.autoGenerateQuestions(profile.getJobCategory(), stacks);
                        deliveredIds = questionDeliveryMapper.findDeliveredQuestionIds(profile.getUserId());
                        questions = questionMapper.findAvailableQuestions(
                                profile.getJobCategory(), stacks, deliveredIds, count);
                    } catch (Exception ex) {
                        log.error("AI 질문 자동 생성 실패: userId={}", profile.getUserId(), ex);
                    }
                }

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
