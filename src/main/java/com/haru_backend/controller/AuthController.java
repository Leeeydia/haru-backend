package com.haru_backend.controller;

import com.haru_backend.dto.request.LoginRequest;
import com.haru_backend.dto.request.SignupRequest;
import com.haru_backend.dto.response.ApiResponse;
import com.haru_backend.dto.response.AuthResponse;
import com.haru_backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthResponse>> signup(@Valid @RequestBody SignupRequest request) {
        AuthResponse data = authService.signup(request);
        return ResponseEntity.ok(ApiResponse.success(data, "회원가입 성공"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse data = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(data, "로그인 성공"));
    }
}
