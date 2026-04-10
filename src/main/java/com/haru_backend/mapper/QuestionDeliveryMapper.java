package com.haru_backend.mapper;

import com.haru_backend.domain.QuestionDelivery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface QuestionDeliveryMapper {

    void insertDelivery(QuestionDelivery delivery);

    List<Long> findDeliveredQuestionIds(@Param("userId") Long userId);
}
