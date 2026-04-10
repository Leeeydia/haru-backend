package com.haru_backend.mapper;

import com.haru_backend.domain.Feedback;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FeedbackMapper {

    void insertFeedback(Feedback feedback);

    Feedback findByAnswerId(@Param("answerId") Long answerId);

    Feedback findLatestByAnswerId(@Param("answerId") Long answerId);
}
