package com.haru_backend.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Answer {

    private Long id;
    private Long userId;
    private Long questionId;
    private Long deliveryId;
    private String content;
    private Integer version;
    private Boolean isFinal;
    private LocalDateTime submittedAt;
}
