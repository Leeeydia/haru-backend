package com.haru_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitHubStatusResponse {

    private boolean connected;
    private String githubUsername;
    private String githubRepo;
}
