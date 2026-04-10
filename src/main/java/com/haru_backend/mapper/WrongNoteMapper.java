package com.haru_backend.mapper;

import com.haru_backend.domain.WrongNote;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WrongNoteMapper {

    void insertWrongNote(WrongNote wrongNote);

    List<WrongNote> findByUserId(@Param("userId") Long userId);

    void updateResolved(@Param("id") Long id, @Param("resolved") boolean resolved);
}
