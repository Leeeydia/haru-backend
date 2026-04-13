package com.haru_backend.mapper;

import com.haru_backend.domain.Answer;
import com.haru_backend.dto.response.AnswerHistoryResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AnswerMapper {

    void insertAnswer(Answer answer);

    List<Answer> findByDeliveryId(@Param("deliveryId") Long deliveryId);

    List<Answer> findByDeliveryIdAndUserId(@Param("deliveryId") Long deliveryId, @Param("userId") Long userId);

    Answer findById(@Param("id") Long id);

    Answer findLatestByDeliveryId(@Param("deliveryId") Long deliveryId);

    List<Answer> findByUserId(@Param("userId") Long userId);

    Integer getMaxVersionByDeliveryId(@Param("deliveryId") Long deliveryId);

    int countByDeliveryId(@Param("deliveryId") Long deliveryId);

    List<AnswerHistoryResponse> findMyAnswersWithDetail(@Param("userId") Long userId);

    Answer findDraftByDeliveryId(@Param("deliveryId") Long deliveryId);

    void updateAnswer(Answer answer);

    void deleteDraftsByDeliveryId(@Param("deliveryId") Long deliveryId);
}
