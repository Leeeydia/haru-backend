package com.haru_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatsResponse {

    // 총 답변 수, 평균 점수, 최근 30일 평균 점수
    private int totalAnswerCount;
    private double averageScore;
    private double recentAverageScore;

    // 스트릭
    private int currentStreak;
    private int maxStreak;

    // 카테고리별 통계
    private List<CategoryStat> categoryStats;
    private String strongestCategory;
    private String weakestCategory;

    // 일별 답변 활동 (잔디용, 최근 365일)
    private List<DailyActivity> dailyActivities;

    // 카테고리별 주간 평균 점수 추이 (최근 12주)
    private List<WeeklyTrend> weeklyTrends;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryStat {
        private String category;
        private int answerCount;
        private double averageScore;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyActivity {
        private String date;
        private int count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeeklyTrend {
        private String weekStart;
        private String category;
        private double averageScore;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentAnswer {
        private String questionContent;
        private String category;
        private int totalScore;
        private String submittedAt;
    }
}
