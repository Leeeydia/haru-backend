package com.haru_backend.mapper;

import com.haru_backend.domain.Question;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface QuestionMapper {

    void insertQuestion(Question question);

    List<Question> findAll();

    Question findById(@Param("id") Long id);

    List<Question> findByCategory(@Param("category") String category);

    List<Question> findByCategoryAndStacks(@Param("category") String category, @Param("stacks") List<String> stacks);
}
