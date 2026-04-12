package com.haru_backend.service;

import com.haru_backend.domain.*;
import com.haru_backend.dto.response.GitHubStatusResponse;
import com.haru_backend.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${github.client-id}")
    private String clientId;

    @Value("${github.client-secret}")
    private String clientSecret;

    @Value("${github.redirect-uri:http://localhost:5173/github/callback}")
    private String redirectUri;

    private static final String GITHUB_API_BASE = "https://api.github.com";
    private static final String GITHUB_OAUTH_AUTHORIZE = "https://github.com/login/oauth/authorize";
    private static final String GITHUB_OAUTH_ACCESS_TOKEN = "https://github.com/login/oauth/access_token";

    public GitHubStatusResponse getStatus(Long userId) {
        User user = userMapper.findById(userId);
        boolean connected = user != null
                && user.getGithubAccessToken() != null
                && !user.getGithubAccessToken().isEmpty();

        return GitHubStatusResponse.builder()
                .connected(connected)
                .githubUsername(connected ? user.getGithubUsername() : null)
                .githubRepo(connected ? user.getGithubRepo() : null)
                .build();
    }

    public String getAuthorizationUrl() {
        return GITHUB_OAUTH_AUTHORIZE
                + "?client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&scope=repo";
    }

    @SuppressWarnings("unchecked")
    public void processCallback(Long userId, String code) {
        // 1. code -> access token 교환
        Map<String, String> tokenRequest = new HashMap<>();
        tokenRequest.put("client_id", clientId);
        tokenRequest.put("client_secret", clientSecret);
        tokenRequest.put("code", code);
        tokenRequest.put("redirect_uri", redirectUri);

        Map<String, Object> tokenResponse = WebClient.create()
                .post()
                .uri(GITHUB_OAUTH_ACCESS_TOKEN)
                .header("Accept", "application/json")
                .bodyValue(tokenRequest)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (tokenResponse == null || tokenResponse.containsKey("error")) {
            String error = tokenResponse != null ? (String) tokenResponse.get("error_description") : "응답 없음";
            throw new IllegalArgumentException("GitHub 인증 실패: " + error);
        }

        String accessToken = (String) tokenResponse.get("access_token");

        // 2. access token으로 사용자 정보 조회
        Map<String, Object> userInfo = WebClient.create(GITHUB_API_BASE)
                .get()
                .uri("/user")
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/vnd.github.v3+json")
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (userInfo == null) {
            throw new IllegalArgumentException("GitHub 사용자 정보 조회 실패");
        }

        String githubUsername = (String) userInfo.get("login");

        // 3. 기본 레포 이름 설정 (haru-handap-notes)
        String repoName = "haru-handap-notes";
        ensureRepoExists(accessToken, githubUsername, repoName);

        // 4. DB 저장
        userMapper.updateGithubInfo(userId, accessToken, githubUsername, repoName);
        log.info("GitHub 연동 완료: userId={}, username={}", userId, githubUsername);
    }

    public void disconnect(Long userId) {
        userMapper.updateGithubInfo(userId, null, null, null);
        log.info("GitHub 연동 해제: userId={}", userId);
    }

    @SuppressWarnings("unchecked")
    private void ensureRepoExists(String accessToken, String username, String repoName) {
        WebClient client = WebClient.create(GITHUB_API_BASE);
        try {
            client.get()
                    .uri("/repos/{owner}/{repo}", username, repoName)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/vnd.github.v3+json")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            log.debug("GitHub 레포 이미 존재: {}/{}", username, repoName);
        } catch (Exception e) {
            // 레포가 없으면 생성
            Map<String, Object> repoRequest = new HashMap<>();
            repoRequest.put("name", repoName);
            repoRequest.put("description", "하루한답 - 매일 면접 답변 기록");
            repoRequest.put("private", false);
            repoRequest.put("auto_init", true);

            client.post()
                    .uri("/user/repos")
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/vnd.github.v3+json")
                    .bodyValue(repoRequest)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            log.info("GitHub 레포 생성: {}/{}", username, repoName);
        }
    }

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
