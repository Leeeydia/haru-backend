package com.haru_backend.controller;

import com.haru_backend.dto.response.ApiResponse;
import com.haru_backend.dto.response.GitHubStatusResponse;
import com.haru_backend.service.GitHubService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/github")
@RequiredArgsConstructor
public class GitHubController {

    private final GitHubService gitHubService;

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<GitHubStatusResponse>> getStatus(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        GitHubStatusResponse data = gitHubService.getStatus(userId);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PostMapping("/connect")
    public ResponseEntity<ApiResponse<Map<String, String>>> connect(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        String url = gitHubService.getAuthorizationUrl(userId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("authUrl", url)));
    }

    @PostMapping("/callback")
    public ResponseEntity<ApiResponse<Void>> callback(
            Authentication authentication,
            @RequestBody Map<String, String> request) {
        Long userId = (Long) authentication.getPrincipal();
        String code = request.get("code");
        String state = request.get("state");
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("code 파라미터가 필요합니다");
        }
        gitHubService.verifyOAuthState(userId, state);
        gitHubService.processCallback(userId, code);
        return ResponseEntity.ok(ApiResponse.success(null, "GitHub 연동 완료"));
    }

    @DeleteMapping("/disconnect")
    public ResponseEntity<ApiResponse<Void>> disconnect(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        gitHubService.disconnect(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "GitHub 연동 해제 완료"));
    }
}
