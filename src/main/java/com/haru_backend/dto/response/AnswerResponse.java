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
public class AnswerResponse {

    private Long id;
    private Long userId;
    private Long deliveryId;
    private Long questionId;
    private String questionContent;
    private String category;
    private String content;
    private Integer version;
    @JsonProperty("isFinal")
    private Boolean isFinal;
    private LocalDateTime submittedAt;
    private Long feedbackId;
}
