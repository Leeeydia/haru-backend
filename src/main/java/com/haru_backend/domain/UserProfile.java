package com.haru_backend.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {

    private Long id;
    private Long userId;
    private String jobCategory;
    private String techStacks;
    private Integer receiveTime;
    private Integer dailyQuestionCount;
    private String receiveDays;
}
