package com.vims.user.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class EmailService {

    //정적으로 매핑되는 라이브러리를 인식하지 못하여 뜨는 에러 해결용
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private JavaMailSender mailSender;

    private final Map<String, String> codeStorage = new ConcurrentHashMap<>();
    private final Map<String, Boolean> verifiedEmails = new ConcurrentHashMap<>();

    public void sendVerificationCode(String email) {
        String code = generateSecureCode(6); // 6자리 영문+숫자 코드
        codeStorage.put(email, code);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");

            helper.setTo(email);
            helper.setSubject("이메일 인증 코드");

            // HTML 본문
            String htmlContent = "" +
                    "<div style='font-family:Arial,sans-serif; padding:20px;'>" +
                    "<h2 style='color:#4CAF50;'>VIMS 이메일 인증</h2>" +
                    "<p>아래 인증코드를 입력해 주세요:</p>" +
                    "<div style='font-size:2em; font-weight:bold; background:#f4f4f4; padding:10px 20px; border-radius:8px; display:inline-block;'>" +
                    code +
                    "</div>" +
                    "<p style='margin-top:20px;'>본 메일은 인증 요청에 의해 발송되었습니다.</p>" +
                    "</div>";

            helper.setText(htmlContent, true); // true = HTML

            mailSender.send(mimeMessage);
        } catch (Exception e) {
            throw new RuntimeException("이메일 전송 실패", e);
        }
    }

    public boolean verifyCode(String email, String code) {
        boolean result = code.equals(codeStorage.get(email));
        if (result) {
            verifiedEmails.put(email, true);
        }
        return result;
    }

    public boolean isEmailVerified(String email) {
        return verifiedEmails.getOrDefault(email, false);
    }

    public void clearEmailVerification(String email) {
        verifiedEmails.remove(email);
    }

    private String generateSecureCode(int length) {
        String chars = "ABCDEFGHJKMNPQRSTUVWXYZ123456789!@#$%&*";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
} 