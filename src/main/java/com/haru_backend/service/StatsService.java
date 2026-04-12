package com.haru_backend.service;

import com.haru_backend.dto.response.StatsResponse;
import com.haru_backend.mapper.StatsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final StatsMapper statsMapper;

    public StatsResponse getStats(Long userId) {
        int totalCount = statsMapper.countFinalAnswers(userId);

        // 답변 없으면 빈 데이터 반환
        if (totalCount == 0) {
            return StatsResponse.builder()
                    .totalAnswerCount(0)
                    .averageScore(0)
                    .recentAverageScore(0)
                    .currentStreak(0)
                    .maxStreak(0)
                    .categoryStats(Collections.emptyList())
                    .strongestCategory(null)
                    .weakestCategory(null)
                    .dailyActivities(Collections.emptyList())
                    .weeklyTrends(Collections.emptyList())
                    .build();
        }

        Double avgScore = statsMapper.averageScore(userId);
        Double recentAvg = statsMapper.recentAverageScore(userId, 30);

        // 스트릭 계산
        List<String> dates = statsMapper.findAnswerDatesDesc(userId);
        int[] streaks = calculateStreaks(dates);

        // 카테고리별 통계
        List<StatsResponse.CategoryStat> categoryStats = statsMapper.categoryStats(userId);
        String strongest = categoryStats.isEmpty() ? null : categoryStats.get(0).getCategory();
        String weakest = categoryStats.isEmpty() ? null : categoryStats.get(categoryStats.size() - 1).getCategory();
        if (categoryStats.size() == 1) {
            weakest = null;
        }

        // 일별 활동, 주간 추이
        List<StatsResponse.DailyActivity> dailyActivities = statsMapper.dailyActivities(userId, 365);
        List<StatsResponse.WeeklyTrend> weeklyTrends = statsMapper.weeklyTrends(userId, 12);

        return StatsResponse.builder()
                .totalAnswerCount(totalCount)
                .averageScore(avgScore != null ? Math.round(avgScore * 10) / 10.0 : 0)
                .recentAverageScore(recentAvg != null ? Math.round(recentAvg * 10) / 10.0 : 0)
                .currentStreak(streaks[0])
                .maxStreak(streaks[1])
                .categoryStats(categoryStats)
                .strongestCategory(strongest)
                .weakestCategory(weakest)
                .dailyActivities(dailyActivities)
                .weeklyTrends(weeklyTrends)
                .build();
    }

    private int[] calculateStreaks(List<String> dateStrings) {
        if (dateStrings.isEmpty()) {
            return new int[]{0, 0};
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<LocalDate> dates = dateStrings.stream()
                .map(s -> LocalDate.parse(s, formatter))
                .toList();

        LocalDate today = LocalDate.now();

        // 현재 스트릭: 오늘 또는 어제부터 시작해서 연속된 일수
        int currentStreak = 0;
        LocalDate expected = today;
        if (!dates.isEmpty() && !dates.get(0).equals(today)) {
            expected = today.minusDays(1);
        }
        for (LocalDate date : dates) {
            if (date.equals(expected)) {
                currentStreak++;
                expected = expected.minusDays(1);
            } else if (date.isBefore(expected)) {
                break;
            }
        }

        // 최대 스트릭
        int maxStreak = 0;
        int streak = 1;
        for (int i = 1; i < dates.size(); i++) {
            if (dates.get(i).equals(dates.get(i - 1).minusDays(1))) {
                streak++;
            } else {
                maxStreak = Math.max(maxStreak, streak);
                streak = 1;
            }
        }
        maxStreak = Math.max(maxStreak, streak);
        maxStreak = Math.max(maxStreak, currentStreak);

        return new int[]{currentStreak, maxStreak};
    }
}
