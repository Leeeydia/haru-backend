package com.haru_backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.haru_backend.domain.Question;
import com.haru_backend.dto.request.QuestionRequest;
import com.haru_backend.dto.response.QuestionResponse;
import com.haru_backend.mapper.QuestionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionMapper questionMapper;
    private final ObjectMapper objectMapper;

    public QuestionResponse createQuestion(QuestionRequest request) {
        Question question = Question.builder()
                .content(request.getContent())
                .category(request.getCategory())
                .difficulty(request.getDifficulty())
                .relatedStacks(toJson(request.getRelatedStacks()))
                .answerKeywords(request.getAnswerKeywords())
                .build();

        questionMapper.insertQuestion(question);

        return toResponse(questionMapper.findById(question.getId()));
    }

    public List<QuestionResponse> getQuestions(String category) {
        List<Question> questions;
        if (category != null && !category.isBlank()) {
            questions = questionMapper.findByCategory(category);
        } else {
            questions = questionMapper.findAll();
        }
        return questions.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public QuestionResponse getQuestionById(Long id) {
        Question question = questionMapper.findById(id);
        if (question == null) {
            throw new IllegalArgumentException("질문이 존재하지 않습니다");
        }
        return toResponse(question);
    }

    private QuestionResponse toResponse(Question question) {
        return QuestionResponse.builder()
                .id(question.getId())
                .content(question.getContent())
                .category(question.getCategory())
                .difficulty(question.getDifficulty())
                .relatedStacks(fromJson(question.getRelatedStacks()))
                .answerKeywords(question.getAnswerKeywords())
                .createdAt(question.getCreatedAt())
                .build();
    }

    private String toJson(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list != null ? list : Collections.emptyList());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("relatedStacks JSON 변환 실패", e);
        }
    }

    private List<String> fromJson(String json) {
        try {
            if (json == null || json.isBlank()) {
                return Collections.emptyList();
            }
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("relatedStacks JSON 파싱 실패", e);
        }
    }
}
