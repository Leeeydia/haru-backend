package com.haru_backend.service;

import com.haru_backend.domain.Answer;
import com.haru_backend.domain.Question;
import com.haru_backend.domain.QuestionDelivery;
import com.haru_backend.dto.request.AnswerRequest;
import com.haru_backend.dto.response.AnswerResponse;
import com.haru_backend.mapper.AnswerMapper;
import com.haru_backend.mapper.QuestionDeliveryMapper;
import com.haru_backend.mapper.QuestionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnswerService {

    private final AnswerMapper answerMapper;
    private final QuestionDeliveryMapper questionDeliveryMapper;
    private final QuestionMapper questionMapper;

    public AnswerResponse getQuestionByToken(String answerToken) {
        QuestionDelivery delivery = questionDeliveryMapper.findByAnswerToken(answerToken);
        if (delivery == null) {
            throw new IllegalArgumentException("유효하지 않은 답변 토큰입니다");
        }

        Question question = questionMapper.findById(delivery.getQuestionId());

        Answer latest = answerMapper.findLatestByDeliveryId(delivery.getId());

        return AnswerResponse.builder()
                .deliveryId(delivery.getId())
                .questionId(question.getId())
                .questionContent(question.getContent())
                .category(question.getCategory())
                .content(latest != null ? latest.getContent() : null)
                .version(latest != null ? latest.getVersion() : 0)
                .isFinal(latest != null ? latest.getIsFinal() : false)
                .submittedAt(latest != null ? latest.getSubmittedAt() : null)
                .build();
    }

    public AnswerResponse submitAnswer(Long userId, AnswerRequest request) {
        QuestionDelivery delivery = questionDeliveryMapper.findById(request.getDeliveryId());
        if (delivery == null) {
            throw new IllegalArgumentException("발송 내역이 존재하지 않습니다");
        }

        if (!delivery.getUserId().equals(userId)) {
            throw new IllegalArgumentException("본인의 질문에만 답변할 수 있습니다");
        }

        Integer maxVersion = answerMapper.getMaxVersionByDeliveryId(request.getDeliveryId());
        int newVersion = (maxVersion != null ? maxVersion : 0) + 1;

        boolean isFinal = request.getIsFinal() != null && request.getIsFinal();

        Answer answer = Answer.builder()
                .userId(userId)
                .questionId(delivery.getQuestionId())
                .deliveryId(request.getDeliveryId())
                .content(request.getContent())
                .version(newVersion)
                .isFinal(isFinal)
                .build();

        answerMapper.insertAnswer(answer);

        if (isFinal) {
            questionDeliveryMapper.updateAnswered(delivery.getId(), true);
        }

        Question question = questionMapper.findById(delivery.getQuestionId());

        return AnswerResponse.builder()
                .id(answer.getId())
                .deliveryId(delivery.getId())
                .questionId(question.getId())
                .questionContent(question.getContent())
                .category(question.getCategory())
                .content(answer.getContent())
                .version(answer.getVersion())
                .isFinal(answer.getIsFinal())
                .submittedAt(answer.getSubmittedAt())
                .build();
    }

    public List<AnswerResponse> getAnswersByDeliveryId(Long deliveryId) {
        List<Answer> answers = answerMapper.findByDeliveryId(deliveryId);
        return answers.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<AnswerResponse> getMyAnswers(Long userId) {
        List<Answer> answers = answerMapper.findByUserId(userId);
        return answers.stream().map(this::toResponse).collect(Collectors.toList());
    }

    private AnswerResponse toResponse(Answer answer) {
        Question question = questionMapper.findById(answer.getQuestionId());
        return AnswerResponse.builder()
                .id(answer.getId())
                .deliveryId(answer.getDeliveryId())
                .questionId(answer.getQuestionId())
                .questionContent(question != null ? question.getContent() : null)
                .category(question != null ? question.getCategory() : null)
                .content(answer.getContent())
                .version(answer.getVersion())
                .isFinal(answer.getIsFinal())
                .submittedAt(answer.getSubmittedAt())
                .build();
    }
}
