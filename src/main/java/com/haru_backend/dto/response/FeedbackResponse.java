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
public class FeedbackResponse {

    private Long id;
    private Long answerId;
    private Integer totalScore;
    private String completeness;
    private String structure;
    private String expression;
    private String specificity;
    private String improvedAnswer;
    private LocalDateTime createdAt;
}
