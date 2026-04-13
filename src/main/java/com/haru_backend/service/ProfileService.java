package com.haru_backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.haru_backend.domain.UserProfile;
import com.haru_backend.dto.request.ProfileRequest;
import com.haru_backend.dto.response.ProfileResponse;
import com.haru_backend.mapper.UserProfileMapper;
import com.haru_backend.scheduler.QuestionScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserProfileMapper userProfileMapper;
    private final ObjectMapper objectMapper;
    private final QuestionScheduler questionScheduler;

    public ProfileResponse saveProfile(Long userId, ProfileRequest request) {
        String techStacksJson = toJson(request.getTechStacks());

        UserProfile existing = userProfileMapper.findByUserId(userId);
        boolean isNewProfile = (existing == null);

        if (existing != null) {
            existing.setJobCategory(request.getJobCategory());
            existing.setTechStacks(techStacksJson);
            existing.setReceiveTime(request.getReceiveTime());
            existing.setDailyQuestionCount(1);
            existing.setReceiveDays(request.getReceiveDays());
            existing.setReminderEnabled(request.getReminderEnabled());
            userProfileMapper.updateProfile(existing);
        } else {
            UserProfile profile = UserProfile.builder()
                    .userId(userId)
                    .jobCategory(request.getJobCategory())
                    .techStacks(techStacksJson)
                    .receiveTime(request.getReceiveTime())
                    .dailyQuestionCount(1)
                    .receiveDays(request.getReceiveDays())
                    .reminderEnabled(request.getReminderEnabled() != null ? request.getReminderEnabled() : true)
                    .build();
            userProfileMapper.insertProfile(profile);
        }

        // 온보딩(최초 프로필 생성) 완료 직후 첫 질문 이메일 즉시 발송 (비동기, 실패해도 프로필 저장은 성공)
        if (isNewProfile) {
            final Long uid = userId;
            CompletableFuture.runAsync(() -> {
                try {
                    questionScheduler.sendQuestionsToUser(uid);
                    log.info("온보딩 즉시 질문 발송 완료: userId={}", uid);
                } catch (Exception e) {
                    log.error("온보딩 즉시 질문 발송 실패: userId={}", uid, e);
                }
            });
        }

        return getProfile(userId);
    }

    public ProfileResponse getProfile(Long userId) {
        UserProfile profile = userProfileMapper.findByUserId(userId);
        if (profile == null) {
            throw new IllegalArgumentException("프로필이 존재하지 않습니다");
        }

        return ProfileResponse.builder()
                .userId(profile.getUserId())
                .jobCategory(profile.getJobCategory())
                .techStacks(fromJson(profile.getTechStacks()))
                .receiveTime(profile.getReceiveTime())
                .dailyQuestionCount(profile.getDailyQuestionCount())
                .receiveDays(profile.getReceiveDays())
                .reminderEnabled(profile.getReminderEnabled())
                .build();
    }

    private String toJson(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list != null ? list : Collections.emptyList());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("techStacks JSON 변환 실패", e);
        }
    }

    private List<String> fromJson(String json) {
        try {
            if (json == null || json.isBlank()) {
                return Collections.emptyList();
            }
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("techStacks JSON 파싱 실패", e);
        }
    }
}
