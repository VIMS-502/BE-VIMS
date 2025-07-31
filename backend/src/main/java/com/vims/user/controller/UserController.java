package com.vims.user.controller;

import com.vims.user.config.JwtTokenProvider;
import com.vims.user.dto.DeleteUserRequest;
import com.vims.user.dto.PasswordChange;
import com.vims.user.dto.UserDto;
import com.vims.user.dto.UserNameChange;
import com.vims.user.entity.User;
import com.vims.user.service.EmailService;
import com.vims.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;


    //비밀번호 교체 용 이메일 코드 전송
    @PostMapping("/send-code/password/change")
    public ResponseEntity<?> passwordChangeCode(@RequestParam String email){
        emailService.sendCodeForPasswordChange(email);
        return ResponseEntity.ok().build();
    }

    //비밀번호 교체 용 이메일 코드 일치여부확인
    @PostMapping("/verify-code/password/change")
    public ResponseEntity<?> verifyCodeForPasswordChange(@RequestParam String email, @RequestParam String code){
        boolean result = emailService.verifyCode(email, code);
        if (result) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.badRequest().body("인증코드가 일치하지 않습니다.");
        }
    }

    //비밀번호 교체
    @PutMapping("/me/password/change")
    public ResponseEntity<?> passwordChange(@RequestBody PasswordChange request) {
        // 1. 비밀번호 일치 확인
        if (!request.getNewPassword().equals(request.getNewPasswordConfirm())) {
            return ResponseEntity.badRequest().body("비밀번호가 일치하지 않습니다.");
        }
        // 2. 인증코드 확인
        boolean codeValid = emailService.verifyCode(request.getEmail(), request.getCode());
        if (!codeValid) {
            return ResponseEntity.badRequest().body("인증코드가 일치하지 않습니다.");
        }
        // 3. 비밀번호 변경
        try {
            userService.passwordChange(request.getEmail(), request.getNewPassword());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    //유저명 변경
    @PutMapping("/me/username/change")
    public ResponseEntity<?> userNameChange(
            @AuthenticationPrincipal User user,
            @RequestBody UserNameChange request
    ) {
        Long userId = user.getId();
        User foundUser = userService.findById(userId);

        if (!userService.checkPassword(foundUser, request.getPassword())) {
            return ResponseEntity.badRequest().body("비밀번호가 일치하지 않습니다.");
        }
        userService.userNameChangeById(request.getUserName(), userId);
        return ResponseEntity.ok().build();
    }

    //탈퇴
    @PostMapping("/me/delete")
    public ResponseEntity<?> deleteUser(@AuthenticationPrincipal User user, @RequestBody DeleteUserRequest request) {
        Long userId = user.getId();
        User foundUser = userService.findById(userId);
        if (!userService.checkPassword(foundUser, request.getPassword())) {
            return ResponseEntity.badRequest().body("비밀번호가 일치하지 않습니다.");
        }
        userService.deleteUserWithPasswordCheck(userId, request.getPassword());
        return ResponseEntity.ok("회원탈퇴가 완료되었습니다.");
    }


    //일관된 에러 처리용 핸들러
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    //유저 정보 전체 조회(마이페이지용)
    @GetMapping("/me/info")
    public ResponseEntity<UserDto> getUserInfo(@AuthenticationPrincipal User user) {
        Long userId = user.getId();
        UserDto dto = userService.getUserDtoById(userId);
        return ResponseEntity.ok(dto);
    }

}
