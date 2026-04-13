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

                // 당일 이미 발송된 사용자는 중복 발송 방지
                int count = profile.getDailyQuestionCount() != null ? profile.getDailyQuestionCount() : 1;
                int todayCount = questionDeliveryMapper.countTodayDeliveries(profile.getUserId());
                if (todayCount >= count) {
                    continue;
                }

                sendQuestionsToUser(profile.getUserId());
            } catch (Exception e) {
                log.error("질문 발송 실패: userId={}", profile.getUserId(), e);
            }
        }
    }

    /**
     * 특정 사용자에게 질문을 선정하여 이메일로 발송한다.
     * 스케줄러와 온보딩 즉시 발송에서 공통으로 사용된다.
     */
    public void sendQuestionsToUser(Long userId) {
        var user = userMapper.findById(userId);
        if (user == null) {
            log.warn("질문 발송 대상 사용자 없음: userId={}", userId);
            return;
        }

        UserProfile profile = userProfileMapper.findByUserId(userId);
        if (profile == null) {
            log.warn("질문 발송 대상 프로필 없음: userId={}", userId);
            return;
        }

        int count = profile.getDailyQuestionCount() != null ? profile.getDailyQuestionCount() : 1;

        List<String> stacks = parseJson(profile.getTechStacks());
        List<Long> deliveredIds = questionDeliveryMapper.findDeliveredQuestionIds(userId);

        List<Question> questions = questionMapper.findAvailableQuestions(
                profile.getJobCategory(), stacks, deliveredIds, count);

        // 보낼 질문이 부족하면 AI로 자동 생성
        if (questions.size() < count) {
            log.info("질문 부족 감지: userId={}, 필요={}, 보유={}, AI 생성 시작",
                    userId, count, questions.size());
            try {
                questionService.autoGenerateQuestions(profile.getJobCategory(), stacks);
                deliveredIds = questionDeliveryMapper.findDeliveredQuestionIds(userId);
                questions = questionMapper.findAvailableQuestions(
                        profile.getJobCategory(), stacks, deliveredIds, count);
            } catch (Exception ex) {
                log.error("AI 질문 자동 생성 실패: userId={}", userId, ex);
            }
        }

        for (Question question : questions) {
            String answerToken = UUID.randomUUID().toString();

            QuestionDelivery delivery = QuestionDelivery.builder()
                    .userId(userId)
                    .questionId(question.getId())
                    .answerToken(answerToken)
                    .build();
            questionDeliveryMapper.insertDelivery(delivery);

            mailService.sendQuestionEmail(
                    user.getEmail(), question.getContent(), question.getCategory(), answerToken);

            log.debug("질문 발송 완료: userId={}, questionId={}", userId, question.getId());
        }
    }

    @Scheduled(cron = "0 30 * * * *")
    public void sendReminders() {
        log.debug("리마인더 스케줄러 실행");

        List<QuestionDelivery> unanswered = questionDeliveryMapper.findUnansweredForReminder(12);

        for (QuestionDelivery delivery : unanswered) {
            try {
                var profile = userProfileMapper.findByUserId(delivery.getUserId());
                if (profile != null && Boolean.FALSE.equals(profile.getReminderEnabled())) {
                    continue;
                }

                var user = userMapper.findById(delivery.getUserId());
                if (user == null) {
                    continue;
                }

                var question = questionMapper.findById(delivery.getQuestionId());
                if (question == null) {
                    continue;
                }

                mailService.sendReminderEmail(
                        user.getEmail(), question.getContent(), question.getCategory(), delivery.getAnswerToken());

                questionDeliveryMapper.updateReminderSent(delivery.getId());

                log.debug("리마인더 발송 완료: userId={}, deliveryId={}", delivery.getUserId(), delivery.getId());
            } catch (Exception e) {
                log.error("리마인더 발송 실패: deliveryId={}", delivery.getId(), e);
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
