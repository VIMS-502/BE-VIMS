package com.vims.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PasswordChange {
    private String email;
    private String code;
    private String newPassword;
    private String newPasswordConfirm;
}