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
            String htmlContent =
            "<div style='max-width:410px;margin:30px auto;font-family:Arial,sans-serif;background:#fff;box-shadow:0 4px 24px #0001;border-radius:12px;overflow:hidden;padding:0;'>"
            + "<div style='background:#4CAF50;padding:22px 0;text-align:center;'>"
            + "<img src='https://img.icons8.com/ios-filled/50/ffffff/email-open--v1.png' alt='인증 아이콘' width='38' height='38' style='margin-bottom:8px;'>"
            + "<h2 style='color:#fff;margin:10px 0 0 0;font-weight:bold;'>VIMS 이메일 인증</h2>"
            + "</div>"
            + "<div style='padding:28px 24px 20px 24px;'>"
            + "<p style='font-size:1.08em;color:#333;margin-bottom:16px;'>안녕하세요.<br>아래 인증코드를 입력해 주세요.</p>"
            + "<div style='margin:20px 0 12px 0;text-align:center;'>"
            + "<span style='display:inline-block;font-size:2.1em;font-weight:600;letter-spacing:8px;color:#333;background:#f4f4f4;padding:16px 28px;border-radius:11px;border:2px dashed #4CAF50;'>"
            + code
            + "</span>"
            + "</div>"
            + "<p style='font-size:0.97em;color:#687076;margin-top:23px;line-height:1.7;'>본 메일은 인증 요청에 의해 발송되었습니다.<br>"
            + "인증 코드는 <b>5분간만 유효</b>하며, 본인이 요청하지 않았다면 이메일을 무시하셔도 됩니다.</p>"
            + "</div>"
            + "<div style='background:#f7f9fa;color:#BBBBBB;text-align:center;font-size:0.92em;padding:12px 0 10px 0;border-top:1px solid #eee;'>"
            + "문의: vimsa502@gmail.com 로 문의해주세요"
            + "</div>"
            + "</div>";


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

    public void sendCodeForPasswordChange(String email) {
        String code = generateSecureCode(8);
        codeStorage.put(email, code);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");

            helper.setTo(email);
            helper.setSubject("이메일 인증 코드");

            // HTML 본문
            String htmlContent =
            "<div style='max-width:410px;margin:30px auto;font-family:Arial,sans-serif;background:#fff;box-shadow:0 4px 24px #0001;border-radius:12px;overflow:hidden;padding:0;'>"
            + "<div style='background:#4CAF50;padding:24px 0;text-align:center;'>"
            + "<img src='https://img.icons8.com/ios-filled/50/ffffff/email-open--v1.png' alt='인증 아이콘' width='38' height='38' style='margin-bottom:8px;'>"
            + "<h2 style='color:#fff;margin:8px 0 0 0;font-weight:bold;'>VIMS 비밀번호 수정 요청</h2>"
            + "</div>"
            + "<div style='padding:32px 28px 24px 28px;'>"
            + "<p style='font-size:1.11em;color:#222;'>비밀번호 변경을 위한 인증 요청이 접수되었습니다.<br>아래의 인증코드를 입력해 주세요.</p>"
            + "<div style='margin:22px 0 12px 0;text-align:center;'>"
            + "<span style='display:inline-block;font-size:2.3em;font-weight:600;letter-spacing:6px;color:#222;background:#f4f4f4;padding:18px 32px;border-radius:12px;border:2px dashed #4CAF50;'>"
            + code
            + "</span>"
            + "</div>"
            + "<p style='font-size:0.95em;color:#888;margin-top:22px;'>이 인증 코드는 <b>5분간만 유효</b>합니다.<br>본 메일은 비밀번호 변경 요청에 따라 안내용으로 발송되었습니다.<br>만약 본인이 요청하지 않았다면 즉시 무시해 주세요.</p>"
            + "</div>"
            + "<div style='background:#f7f9fa;color:#888;text-align:center;font-size:0.91em;padding:12px 0 10px 0;border-top:1px solid #eee;'>"
            + "VIMS 고객지원: vimsa502@gmail.com 로 문의주세요"
            + "</div>"
            + "</div>";
            helper.setText(htmlContent, true); // true = HTML
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            throw new RuntimeException("코드 전송 실패", e);
        }
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