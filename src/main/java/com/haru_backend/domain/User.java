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
public class User {

    private Long id;
    private String email;
    private String password;
    private String name;
    private String githubAccessToken;
    private String githubUsername;
    private String githubRepo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
