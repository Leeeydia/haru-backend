package com.haru_backend.mapper;

import com.haru_backend.dto.response.StatsResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StatsMapper {

    int countFinalAnswers(@Param("userId") Long userId);

    Double averageScore(@Param("userId") Long userId);

    Double recentAverageScore(@Param("userId") Long userId, @Param("days") int days);

    List<String> findAnswerDatesDesc(@Param("userId") Long userId);

    List<StatsResponse.CategoryStat> categoryStats(@Param("userId") Long userId);

    List<StatsResponse.DailyActivity> dailyActivities(@Param("userId") Long userId, @Param("days") int days);

    List<StatsResponse.WeeklyTrend> weeklyTrends(@Param("userId") Long userId, @Param("weeks") int weeks);
}
