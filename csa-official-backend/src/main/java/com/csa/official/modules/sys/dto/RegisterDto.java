package com.csa.official.modules.sys.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

@Data
public class RegisterDto {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 20, message = "用户名长度需在4-20位之间")
    private String username;
    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,20}$", message = "密码必须包含字母和数字，长度6-20位")
    private String password;
    @Email(message = "邮箱格式不正确")
    private String email;
    private String realName;

    private String studentId;
    private String college;
    private String className;

    private String inviteCode; // 邀请码
    private String merchantNo; // 支付单号 (前端支付成功后填入)

    @NotBlank(message = "验证码不能为空")
    private String code; // 新增：邮箱验证码

}