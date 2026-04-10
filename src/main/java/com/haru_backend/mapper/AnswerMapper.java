package com.haru_backend.mapper;

import com.haru_backend.domain.Answer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AnswerMapper {

    void insertAnswer(Answer answer);

    List<Answer> findByDeliveryId(@Param("deliveryId") Long deliveryId);

    Answer findById(@Param("id") Long id);

    Answer findLatestByDeliveryId(@Param("deliveryId") Long deliveryId);

    List<Answer> findByUserId(@Param("userId") Long userId);

    Integer getMaxVersionByDeliveryId(@Param("deliveryId") Long deliveryId);
}
