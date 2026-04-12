package com.haru_backend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

@Data
public class ProfileRequest {

    @NotBlank(message = "직군은 필수입니다")
    private String jobCategory;

    private List<String> techStacks;

    @Min(value = -1, message = "수신 시각은 -1(매시간) 또는 0~23 사이여야 합니다")
    @Max(value = 23, message = "수신 시각은 -1(매시간) 또는 0~23 사이여야 합니다")
    private Integer receiveTime;

    @Min(value = 1, message = "일일 질문 수는 1~3 사이여야 합니다")
    @Max(value = 3, message = "일일 질문 수는 1~3 사이여야 합니다")
    private Integer dailyQuestionCount;

    @Pattern(regexp = "EVERYDAY|WEEKDAY", message = "수신 요일은 EVERYDAY 또는 WEEKDAY여야 합니다")
    private String receiveDays;
}
