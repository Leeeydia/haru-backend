package com.haru_backend.service;

import jakarta.annotation.PostConstruct;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @PostConstruct
    public void checkMailConfig() {
        boolean hasUsername = mailUsername != null && !mailUsername.isEmpty();
        log.info("=== 메일 설정 확인 ===");
        log.info("mail.host={}", mailHost);
        log.info("mail.username={}", hasUsername ? mailUsername : "(비어있음 - MAIL_USERNAME 환경변수 확인 필요)");
        log.info("mail.password={}", hasUsername ? "(설정됨)" : "(비어있음 - MAIL_PASSWORD 환경변수 확인 필요)");
    }

    public void sendQuestionEmail(String to, String questionContent, String category, String answerToken) {
        Context context = new Context();
        context.setVariable("questionContent", questionContent);
        context.setVariable("category", category);
        context.setVariable("answerUrl", "http://localhost:5173/answer/" + answerToken);

        String html = templateEngine.process("mail/question", context);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject("[하루한답] 오늘의 면접 질문이 도착했습니다!");
            helper.setText(html, true);
            mailSender.send(message);
            log.debug("이메일 발송 성공: {}", to);
        } catch (Exception e) {
            log.error("이메일 발송 실패: {}", to, e);
            throw new RuntimeException("이메일 발송에 실패했습니다: " + e.getMessage(), e);
        }
    }
}
