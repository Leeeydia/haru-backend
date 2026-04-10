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
public class AnswerResponse {

    private Long id;
    private Long userId;
    private Long deliveryId;
    private Long questionId;
    private String questionContent;
    private String category;
    private String content;
    private Integer version;
    private Boolean isFinal;
    private LocalDateTime submittedAt;
}
