package com.haru_backend.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WrongNote {

    private Long id;
    private Long userId;
    private Long answerId;
    private String addedType;
    private Boolean resolved;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
}
