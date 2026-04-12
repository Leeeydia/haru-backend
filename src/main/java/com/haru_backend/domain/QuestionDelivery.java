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
public class QuestionDelivery {

    private Long id;
    private Long userId;
    private Long questionId;
    private String answerToken;
    private LocalDateTime sentAt;
    private Boolean answered;
    private Boolean reminderSent;
}
