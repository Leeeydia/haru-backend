package com.haru_backend.controller;

import com.haru_backend.dto.response.ApiResponse;
import com.haru_backend.dto.response.WrongNoteResponse;
import com.haru_backend.service.WrongNoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wrong-notes")
@RequiredArgsConstructor
public class WrongNoteController {

    private final WrongNoteService wrongNoteService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<WrongNoteResponse>>> getMyWrongNotes(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<WrongNoteResponse> data = wrongNoteService.getMyWrongNotes(userId);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PostMapping("/{answerId}")
    public ResponseEntity<ApiResponse<Void>> addWrongNote(
            Authentication authentication,
            @PathVariable Long answerId) {
        Long userId = (Long) authentication.getPrincipal();
        wrongNoteService.addWrongNote(userId, answerId);
        return ResponseEntity.ok(ApiResponse.success(null, "오답 노트에 추가되었습니다"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteWrongNote(
            Authentication authentication,
            @PathVariable Long id) {
        Long userId = (Long) authentication.getPrincipal();
        wrongNoteService.deleteWrongNote(userId, id);
        return ResponseEntity.ok(ApiResponse.success(null, "오답 노트가 삭제되었습니다"));
    }
}
