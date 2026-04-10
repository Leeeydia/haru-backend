package com.haru_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResponse {

    private Long id;
    private String content;
    private String category;
    private String difficulty;
    private List<String> relatedStacks;
    private String answerKeywords;
    private LocalDateTime createdAt;
}
