package com.haru_backend.service;

import com.haru_backend.domain.User;
import com.haru_backend.dto.request.LoginRequest;
import com.haru_backend.dto.request.SignupRequest;
import com.haru_backend.dto.response.AuthResponse;
import com.haru_backend.mapper.UserMapper;
import com.haru_backend.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthResponse signup(SignupRequest request) {
        if (userMapper.findByEmail(request.getEmail()) != null) {
            throw new IllegalArgumentException("이메일이 이미 존재합니다");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .build();

        userMapper.insertUser(user);

        String token = jwtUtil.generateToken(user.getId(), user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userMapper.findByEmail(request.getEmail());
        if (user == null) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .build();
    }
}
