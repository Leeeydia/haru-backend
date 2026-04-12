package com.haru_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WrongNoteResponse {

    private Long id;
    private Long answerId;
    private String addedType;
    private Boolean resolved;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;

    // 질문 정보 (JOIN)
    private Long questionId;
    private String questionContent;
    private String category;
    private String difficulty;

    // 피드백 점수 (JOIN)
    private Integer totalScore;
}
