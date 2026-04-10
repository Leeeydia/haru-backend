package com.haru_backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        } catch (MessagingException e) {
            log.error("이메일 발송 실패: {}", to, e);
        }
    }
}
