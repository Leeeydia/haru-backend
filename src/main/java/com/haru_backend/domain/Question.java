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
public class Question {

    private Long id;
    private String content;
    private String category;
    private String difficulty;
    private String relatedStacks;
    private String answerKeywords;
    private LocalDateTime createdAt;
}
