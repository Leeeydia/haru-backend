package com.haru_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {

    private Long userId;
    private String jobCategory;
    private List<String> techStacks;
    private Integer receiveTime;
    private Integer dailyQuestionCount;
    private String receiveDays;
}
