package com.haru_backend.service;

import com.haru_backend.domain.Answer;
import com.haru_backend.domain.WrongNote;
import com.haru_backend.dto.response.WrongNoteResponse;
import com.haru_backend.mapper.AnswerMapper;
import com.haru_backend.mapper.WrongNoteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WrongNoteService {

    private final WrongNoteMapper wrongNoteMapper;
    private final AnswerMapper answerMapper;

    public List<WrongNoteResponse> getMyWrongNotes(Long userId) {
        return wrongNoteMapper.findByUserIdWithDetails(userId);
    }

    public void addWrongNote(Long userId, Long answerId) {
        Answer answer = answerMapper.findById(answerId);
        if (answer == null) {
            throw new IllegalArgumentException("답변이 존재하지 않습니다");
        }
        if (!answer.getUserId().equals(userId)) {
            throw new IllegalArgumentException("본인의 답변만 오답 노트에 추가할 수 있습니다");
        }

        WrongNote existing = wrongNoteMapper.findByUserIdAndAnswerId(userId, answerId);
        if (existing != null) {
            throw new IllegalArgumentException("이미 오답 노트에 추가된 답변입니다");
        }

        WrongNote wrongNote = WrongNote.builder()
                .userId(userId)
                .answerId(answerId)
                .addedType("MANUAL")
                .build();
        wrongNoteMapper.insertWrongNote(wrongNote);
    }

    public void deleteWrongNote(Long userId, Long id) {
        WrongNote wrongNote = wrongNoteMapper.findById(id);
        if (wrongNote == null) {
            throw new IllegalArgumentException("오답 노트가 존재하지 않습니다");
        }
        if (!wrongNote.getUserId().equals(userId)) {
            throw new IllegalArgumentException("본인의 오답 노트만 삭제할 수 있습니다");
        }

        wrongNoteMapper.deleteById(id);
    }
}
