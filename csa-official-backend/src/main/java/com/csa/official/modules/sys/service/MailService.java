package com.csa.official.modules.sys.service;

import com.csa.official.common.exception.CsaException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class MailService {

    @Resource
    private JavaMailSender mailSender;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${spring.mail.username}")
    private String from;

    /**
     * 发送验证码
     * 
     * @param to 目标邮箱
     */
    @SuppressWarnings("null")
    public void sendCode(String to) {
        // 1. 检查是否频繁发送 (60秒防刷)
        String limitKey = "verify:limit:" + to;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(limitKey))) {
            throw new CsaException("请勿频繁发送验证码");
        }

        // 2. 生成 6 位随机验证码
        String code = String.valueOf(new Random().nextInt(900000) + 100000);

        try {
            // 3. 发送邮件
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject("【CSA计算机协会】注册验证码");
            message.setText("您的验证码是：" + code + "，有效期5分钟。如非本人操作，请忽略。");
            mailSender.send(message);

            // 4. 存入 Redis (有效期 5 分钟)
            // Key: verify:code:xxxx@qq.com
            String codeKey = "verify:code:" + to;
            redisTemplate.opsForValue().set(codeKey, code, 5, TimeUnit.MINUTES);

            // 5. 设置 60秒 防刷限制
            redisTemplate.opsForValue().set(limitKey, "1", 60, TimeUnit.SECONDS);

            log.info("邮件发送成功: {} -> {}", to, code);
        } catch (Exception e) {
            log.error("邮件发送失败", e);
            throw new CsaException("邮件发送失败，请检查邮箱是否正确");
        }
    }

    /**
     * 校验验证码 (注册时调用)
     */
    public void verifyCode(String email, String inputCode) {
        String codeKey = "verify:code:" + email;
        String realCode = (String) redisTemplate.opsForValue().get(codeKey);

        if (realCode == null) {
            throw new CsaException("验证码已过期，请重新获取");
        }
        if (!realCode.equals(inputCode)) {
            throw new CsaException("验证码错误");
        }

        // 验证通过后删除，防止重复使用
        redisTemplate.delete(codeKey);
    }
}