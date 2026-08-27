package com.csa.official.common.util;

import com.csa.official.modules.sys.entity.User;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilsTest {

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "expiration", 60_000L);
    }

    @Test
    void startupRejectsSecretShorterThanHs256Minimum() {
        ReflectionTestUtils.setField(jwtUtils, "secret", "too-short");

        assertThatThrownBy(jwtUtils::validateSecret)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }

    @Test
    void validSecretSignsAndParsesToken() {
        ReflectionTestUtils.setField(jwtUtils, "secret", "01234567890123456789012345678901");
        jwtUtils.validateSecret();
        User user = buildUser();

        String token = jwtUtils.generateToken(user, 60_000L);

        assertThat(jwtUtils.getUsernameFromToken(token)).isEqualTo("member");
        assertThat(jwtUtils.getUserIdFromToken(token)).isEqualTo(42L);
        assertThat(jwtUtils.getTokenIdFromToken(token)).isNotBlank();
        assertThat(jwtUtils.isTokenExpired(token)).isFalse();
    }

    /**
     * 篡改 payload（用户身份声明）后签名必须校验失败。
     *
     * <p>这里改的是中间那段 claims，而不是签名的最后一个字符。原因见
     * {@link #tamperedSignatureFailsVerification()}：HS256 签名是 32 字节，
     * base64url 编码后是 43 个字符，最后一个字符只承载 4 个有效 bit，
     * 剩下 2 个 bit 会被解码器忽略。所以改最后一个字符<b>不一定</b>真的改到签名字节，
     * 用它来构造「被篡改的 token」会随机失败。
     */
    @Test
    void tamperedPayloadFailsSignatureVerification() {
        ReflectionTestUtils.setField(jwtUtils, "secret", "01234567890123456789012345678901");
        jwtUtils.validateSecret();
        String token = jwtUtils.generateToken(buildUser(), 60_000L);

        String[] parts = token.split("\\.");
        assertThat(parts).hasSize(3);
        // 把 payload 换成一个声称自己是 root、roleLevel=99 的伪造 claims
        String forgedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"sub\":\"root\",\"userId\":1,\"roleLevel\":99}".getBytes(StandardCharsets.UTF_8));
        String tampered = parts[0] + "." + forgedPayload + "." + parts[2];

        assertThatThrownBy(() -> jwtUtils.getUsernameFromToken(tampered))
                .isInstanceOf(JwtException.class);
    }

    /**
     * 篡改签名本身后必须校验失败。
     *
     * <p>翻转签名首字符的高位，保证解码出来的字节一定变了
     * （不像末位字符那样存在 4 个字符解码结果相同的情况）。
     */
    @Test
    void tamperedSignatureFailsVerification() {
        ReflectionTestUtils.setField(jwtUtils, "secret", "01234567890123456789012345678901");
        jwtUtils.validateSecret();
        String token = jwtUtils.generateToken(buildUser(), 60_000L);

        String[] parts = token.split("\\.");
        char first = parts[2].charAt(0);
        // 'A' 和 'Q' 的 6bit 值相差 16，高位不同，解码后的首字节必然不同
        char replacement = first == 'A' ? 'Q' : 'A';
        String tampered = parts[0] + "." + parts[1] + "." + replacement + parts[2].substring(1);

        assertThatThrownBy(() -> jwtUtils.getUsernameFromToken(tampered))
                .isInstanceOf(JwtException.class);
    }

    private User buildUser() {
        User user = new User();
        user.setId(42L);
        user.setUsername("member");
        user.setRoleLevel(1);
        return user;
    }
}
