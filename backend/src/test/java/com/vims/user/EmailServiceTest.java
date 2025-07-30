package com.vims.user;

import com.vims.user.service.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import jakarta.mail.internet.MimeMessage;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
//이메일 단위테스트
public class EmailServiceTest {

    @Test
    @DisplayName("이메일 코드 전송 확인 테스트")
    void sendVerificationCodeTest() throws Exception {
        String email = "test@example.com";
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        EmailService emailService = new EmailService();
        java.lang.reflect.Field mailSenderField = EmailService.class.getDeclaredField("mailSender");
        mailSenderField.setAccessible(true);
        mailSenderField.set(emailService, mailSender);

        emailService.sendVerificationCode(email);

        verify(mailSender, times(1)).send(any(MimeMessage.class));
        java.lang.reflect.Field codeStorageField = EmailService.class.getDeclaredField("codeStorage");
        codeStorageField.setAccessible(true);
        Map<String, String> codeStorage = (Map<String, String>) codeStorageField.get(emailService);
        assertTrue(codeStorage.containsKey(email));
    }

    @Test
    @DisplayName("이메일 인증 코드 확인 성공/실패 테스트")
    void verifyCodeTest() throws Exception {
        String email = "test@example.com";
        String code = "ABC123";

        EmailService emailService = new EmailService();
        java.lang.reflect.Field codeStorageField = EmailService.class.getDeclaredField("codeStorage");
        codeStorageField.setAccessible(true);
        Map<String, String> codeStorage = (Map<String, String>) codeStorageField.get(emailService);
        codeStorage.put(email, code);

        assertTrue(emailService.verifyCode(email, code)); // 성공
        assertFalse(emailService.verifyCode(email, "WRONGCODE")); // 실패
    }
}
