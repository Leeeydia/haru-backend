package com.haru_backend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerHistoryResponse {

    private Long id;
    private Long userId;
    private Long questionId;
    private Long deliveryId;
    private String content;
    private Integer version;
    @JsonProperty("isFinal")
    private Boolean isFinal;
    private LocalDateTime submittedAt;

    // 질문 정보
    private String questionContent;
    private String category;
    private String difficulty;

    // 피드백 점수
    private Integer score;

    // 답변 토큰
    private String answerToken;
}
