package com.vims.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("이메일 인증 코드");
        message.setText("인증코드: " + code);

        mailSender.send(message);
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