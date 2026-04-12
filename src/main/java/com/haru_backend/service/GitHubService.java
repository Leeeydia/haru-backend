package com.haru_backend.service;

import com.haru_backend.domain.*;
import com.haru_backend.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubService {

    private final UserMapper userMapper;

    private static final String GITHUB_API_BASE = "https://api.github.com";

    public void commitFeedback(Long userId, Question question, Answer answer, Feedback feedback) {
        User user = userMapper.findById(userId);
        if (user == null || user.getGithubAccessToken() == null
                || user.getGithubUsername() == null || user.getGithubRepo() == null) {
            log.debug("GitHub 설정 미완료, 커밋 건너뜀: userId={}", userId);
            return;
        }

        String path = buildFilePath(question);
        String markdown = buildMarkdown(question, answer, feedback);
        String encodedContent = Base64.getEncoder().encodeToString(markdown.getBytes(StandardCharsets.UTF_8));

        WebClient gitHubClient = WebClient.builder()
                .baseUrl(GITHUB_API_BASE)
                .defaultHeader("Authorization", "Bearer " + user.getGithubAccessToken())
                .defaultHeader("Accept", "application/vnd.github.v3+json")
                .build();

        try {
            String sha = getFileSha(gitHubClient, user.getGithubUsername(), user.getGithubRepo(), path);
            boolean isUpdate = sha != null;

            String commitMessage = buildCommitMessage(question, isUpdate);

            Map<String, Object> body = new HashMap<>();
            body.put("message", commitMessage);
            body.put("content", encodedContent);
            if (isUpdate) {
                body.put("sha", sha);
            }

            gitHubClient.put()
                    .uri("/repos/{owner}/{repo}/contents/{path}",
                            user.getGithubUsername(), user.getGithubRepo(), path)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            log.info("GitHub 커밋 성공: userId={}, path={}, update={}", userId, path, isUpdate);
        } catch (Exception e) {
            log.error("GitHub 커밋 실패: userId={}, path={}", userId, path, e);
        }
    }

    private String getFileSha(WebClient client, String owner, String repo, String path) {
        try {
            Map<String, Object> response = client.get()
                    .uri("/repos/{owner}/{repo}/contents/{path}", owner, repo, path)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            return response != null ? (String) response.get("sha") : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 파일 경로: {year}/{month}/{YYYY-MM-DD}-{카테고리}-{질문ID}.md
     * 예: 2026/04/2026-04-12-백엔드-42.md
     */
    private String buildFilePath(Question question) {
        LocalDate today = LocalDate.now();
        String year = String.valueOf(today.getYear());
        String month = String.format("%02d", today.getMonthValue());
        String date = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String category = question.getCategory();

        return String.format("%s/%s/%s-%s-%d.md", year, month, date, category, question.getId());
    }

    /**
     * 커밋 메시지: docs: {날짜} 면접 답변 - {질문 제목 앞 20자}
     * 재제출 시: docs: {날짜} 면접 답변 (수정) - {질문 제목 앞 20자}
     */
    private String buildCommitMessage(Question question, boolean isUpdate) {
        LocalDate today = LocalDate.now();
        String date = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String titlePrefix = question.getContent().length() > 20
                ? question.getContent().substring(0, 20) + "..."
                : question.getContent();
        String updateMark = isUpdate ? " (수정)" : "";

        return String.format("docs: %s 면접 답변%s - %s", date, updateMark, titlePrefix);
    }

    private String buildMarkdown(Question question, Answer answer, Feedback feedback) {
        StringBuilder sb = new StringBuilder();

        sb.append("# ").append(question.getContent()).append("\n\n");
        sb.append("> **카테고리**: ").append(question.getCategory())
                .append(" | **난이도**: ").append(question.getDifficulty())
                .append(" | **점수**: ").append(feedback.getTotalScore()).append("/100")
                .append("\n\n");

        sb.append("---\n\n");

        sb.append("## 내 답변\n\n");
        sb.append(answer.getContent()).append("\n\n");

        sb.append("---\n\n");

        sb.append("## AI 피드백\n\n");

        sb.append("### 내용 완성도\n");
        sb.append(feedback.getCompleteness()).append("\n\n");

        sb.append("### 답변 구조\n");
        sb.append(feedback.getStructure()).append("\n\n");

        sb.append("### 표현/말투\n");
        sb.append(feedback.getExpression()).append("\n\n");

        sb.append("### 구체성\n");
        sb.append(feedback.getSpecificity()).append("\n\n");

        sb.append("---\n\n");

        sb.append("## 모범 답변\n\n");
        sb.append(feedback.getImprovedAnswer()).append("\n");

        return sb.toString();
    }
}
