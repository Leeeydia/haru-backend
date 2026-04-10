package com.haru_backend.service;

import com.haru_backend.domain.Answer;
import com.haru_backend.domain.Feedback;
import com.haru_backend.domain.Question;
import com.haru_backend.domain.WrongNote;
import com.haru_backend.dto.response.FeedbackResponse;
import com.haru_backend.mapper.AnswerMapper;
import com.haru_backend.mapper.FeedbackMapper;
import com.haru_backend.mapper.QuestionMapper;
import com.haru_backend.mapper.WrongNoteMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackMapper feedbackMapper;
    private final AnswerMapper answerMapper;
    private final QuestionMapper questionMapper;
    private final WrongNoteMapper wrongNoteMapper;
    private final AiService aiService;

    public FeedbackResponse generateFeedback(Long answerId) {
        Answer answer = answerMapper.findById(answerId);
        if (answer == null) {
            throw new IllegalArgumentException("답변이 존재하지 않습니다");
        }

        Question question = questionMapper.findById(answer.getQuestionId());

        Feedback feedback = aiService.analyzeFeedback(
                answerId,
                question.getContent(),
                answer.getContent(),
                question.getCategory(),
                question.getAnswerKeywords());

        feedbackMapper.insertFeedback(feedback);

        // 50점 미만이면 오답 노트 자동 추가
        if (feedback.getTotalScore() < 50) {
            WrongNote wrongNote = WrongNote.builder()
                    .userId(answer.getUserId())
                    .answerId(answerId)
                    .addedType("AUTO")
                    .build();
            wrongNoteMapper.insertWrongNote(wrongNote);
            log.debug("오답 노트 자동 추가: answerId={}, score={}", answerId, feedback.getTotalScore());
        }

        return toResponse(feedback);
    }

    public FeedbackResponse getFeedback(Long answerId) {
        Feedback feedback = feedbackMapper.findLatestByAnswerId(answerId);
        if (feedback == null) {
            throw new IllegalArgumentException("피드백이 존재하지 않습니다");
        }
        return toResponse(feedback);
    }

    private FeedbackResponse toResponse(Feedback feedback) {
        return FeedbackResponse.builder()
                .id(feedback.getId())
                .answerId(feedback.getAnswerId())
                .totalScore(feedback.getTotalScore())
                .completeness(feedback.getCompleteness())
                .structure(feedback.getStructure())
                .expression(feedback.getExpression())
                .specificity(feedback.getSpecificity())
                .improvedAnswer(feedback.getImprovedAnswer())
                .createdAt(feedback.getCreatedAt())
                .build();
    }
}
