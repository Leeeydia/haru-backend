package com.haru_backend.controller;

import com.haru_backend.dto.response.ApiResponse;
import com.haru_backend.dto.response.StatsResponse;
import com.haru_backend.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping
    public ResponseEntity<ApiResponse<StatsResponse>> getStats(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        StatsResponse data = statsService.getStats(userId);
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
