package com.haru_backend.controller;

import com.haru_backend.dto.request.ProfileRequest;
import com.haru_backend.dto.response.ApiResponse;
import com.haru_backend.dto.response.ProfileResponse;
import com.haru_backend.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> saveProfile(
            Authentication authentication,
            @Valid @RequestBody ProfileRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        ProfileResponse data = profileService.saveProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success(data, "프로필 저장 성공"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        ProfileResponse data = profileService.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
