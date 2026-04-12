package com.haru_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class QuestionRequest {

    @NotBlank(message = "질문 내용은 필수입니다")
    private String content;

    @NotBlank(message = "카테고리는 필수입니다")
    private String category;

    @NotBlank(message = "난이도는 필수입니다")
    private String difficulty;

    private List<String> relatedStacks;

    private String answerKeywords;
}
