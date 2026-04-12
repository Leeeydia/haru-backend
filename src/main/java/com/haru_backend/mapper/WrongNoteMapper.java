package com.haru_backend.mapper;

import com.haru_backend.domain.WrongNote;
import com.haru_backend.dto.response.WrongNoteResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WrongNoteMapper {

    void insertWrongNote(WrongNote wrongNote);

    List<WrongNote> findByUserId(@Param("userId") Long userId);

    List<WrongNoteResponse> findByUserIdWithDetails(@Param("userId") Long userId);

    WrongNote findById(@Param("id") Long id);

    WrongNote findByUserIdAndAnswerId(@Param("userId") Long userId, @Param("answerId") Long answerId);

    void updateResolved(@Param("id") Long id, @Param("resolved") boolean resolved);

    void deleteById(@Param("id") Long id);
}
