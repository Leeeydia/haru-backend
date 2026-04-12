package com.haru_backend.mapper;

import com.haru_backend.domain.QuestionDelivery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface QuestionDeliveryMapper {

    void insertDelivery(QuestionDelivery delivery);

    List<Long> findDeliveredQuestionIds(@Param("userId") Long userId);

    QuestionDelivery findByAnswerToken(@Param("answerToken") String answerToken);

    QuestionDelivery findById(@Param("id") Long id);

    void updateAnswered(@Param("id") Long id, @Param("answered") boolean answered);

    int countTodayDeliveries(@Param("userId") Long userId);

    List<QuestionDelivery> findUnansweredForReminder(@Param("hoursAgo") int hoursAgo);

    void updateReminderSent(@Param("id") Long id);
}
