package com.haru_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AnswerRequest {

    @NotNull(message = "deliveryId는 필수입니다")
    private Long deliveryId;

    @NotBlank(message = "답변 내용은 필수입니다")
    private String content;

    private Boolean isFinal;
}
