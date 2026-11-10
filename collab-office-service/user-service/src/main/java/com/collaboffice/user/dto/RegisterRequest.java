package com.collaboffice.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 20, message = "密码长度8-20位")
    private String password;

    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名最长50位")
    private String username;

    private String smsCode;

    @Pattern(regexp = "^[\\w.-]+@[\\w.-]+\\.\\w+$", message = "邮箱格式不正确")
    private String email;
}
