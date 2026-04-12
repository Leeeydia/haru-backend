package com.haru_backend.service;

import com.haru_backend.domain.*;
import com.haru_backend.dto.response.GitHubStatusResponse;
import com.haru_backend.dto.response.StatsResponse;
import com.haru_backend.mapper.StatsMapper;
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
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubService {

    private final UserMapper userMapper;
    private final StatsMapper statsMapper;

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

            // README 자동 업데이트
            try {
                updateReadme(userId, gitHubClient, user.getGithubUsername(), user.getGithubRepo());
            } catch (Exception ex) {
                log.error("README 업데이트 실패: userId={}", userId, ex);
            }
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

    public void updateReadmeForUser(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null || user.getGithubAccessToken() == null) {
            throw new IllegalArgumentException("GitHub 연동이 필요합니다");
        }

        WebClient gitHubClient = WebClient.builder()
                .baseUrl(GITHUB_API_BASE)
                .defaultHeader("Authorization", "Bearer " + user.getGithubAccessToken())
                .defaultHeader("Accept", "application/vnd.github.v3+json")
                .build();

        updateReadme(userId, gitHubClient, user.getGithubUsername(), user.getGithubRepo());
    }

    @SuppressWarnings("unchecked")
    private void updateReadme(Long userId, WebClient gitHubClient, String owner, String repo) {
        int totalCount = statsMapper.countFinalAnswers(userId);
        Double avgScore = statsMapper.averageScore(userId);
        List<StatsResponse.CategoryStat> categoryStats = statsMapper.categoryStats(userId);
        List<StatsResponse.RecentAnswer> recentAnswers = statsMapper.recentAnswers(userId, 10);

        String readme = buildReadmeMarkdown(totalCount, avgScore, categoryStats, recentAnswers);
        String encodedContent = Base64.getEncoder().encodeToString(readme.getBytes(StandardCharsets.UTF_8));

        // 최대 2회 시도 (409 Conflict 대비 — 직전 답변 커밋으로 sha가 변경될 수 있음)
        for (int attempt = 0; attempt < 2; attempt++) {
            String sha = getFileSha(gitHubClient, owner, repo, "README.md");

            Map<String, Object> body = new HashMap<>();
            body.put("message", "docs: README 자동 업데이트");
            body.put("content", encodedContent);
            if (sha != null) {
                body.put("sha", sha);
            }

            try {
                gitHubClient.put()
                        .uri("/repos/{owner}/{repo}/contents/{path}", owner, repo, "README.md")
                        .bodyValue(body)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();

                log.info("README 업데이트 성공: userId={}", userId);
                return;
            } catch (Exception e) {
                log.warn("README 업데이트 시도 {}/2 실패: userId={}, error={}", attempt + 1, userId, e.getMessage());
                if (attempt == 0) {
                    try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                }
            }
        }
        log.error("README 업데이트 최종 실패: userId={}", userId);
    }

    private String buildReadmeMarkdown(int totalCount, Double avgScore,
                                        List<StatsResponse.CategoryStat> categoryStats,
                                        List<StatsResponse.RecentAnswer> recentAnswers) {
        StringBuilder sb = new StringBuilder();

        sb.append("# 하루한답 - 면접 답변 기록\n\n");
        sb.append("> 매일 한 문제, 꾸준히 준비하는 면접 습관\n\n");

        // 전체 통계
        sb.append("## 📊 전체 통계\n\n");
        sb.append("| 항목 | 값 |\n");
        sb.append("|------|----|\n");
        sb.append(String.format("| 총 답변 수 | **%d**개 |\n", totalCount));
        sb.append(String.format("| 평균 점수 | **%.1f**점 |\n", avgScore != null ? avgScore : 0.0));
        sb.append("\n");

        // 카테고리별 통계
        if (!categoryStats.isEmpty()) {
            sb.append("## 📂 카테고리별 성적\n\n");
            sb.append("| 카테고리 | 답변 수 | 평균 점수 |\n");
            sb.append("|----------|---------|----------|\n");
            for (StatsResponse.CategoryStat stat : categoryStats) {
                sb.append(String.format("| %s | %d개 | %.1f점 |\n",
                        stat.getCategory(), stat.getAnswerCount(), stat.getAverageScore()));
            }
            sb.append("\n");
        }

        // 최근 답변
        if (!recentAnswers.isEmpty()) {
            sb.append("## 📝 최근 답변\n\n");
            sb.append("| 날짜 | 질문 | 점수 |\n");
            sb.append("|------|------|------|\n");
            for (StatsResponse.RecentAnswer answer : recentAnswers) {
                String title = answer.getQuestionContent().length() > 40
                        ? answer.getQuestionContent().substring(0, 40) + "..."
                        : answer.getQuestionContent();
                sb.append(String.format("| %s | %s | %d점 |\n",
                        answer.getSubmittedAt(), title, answer.getTotalScore()));
            }
            sb.append("\n");
        }

        sb.append("---\n\n");
        sb.append("*이 README는 [하루한답](https://github.com)에 의해 자동 생성됩니다.*\n");

        return sb.toString();
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
