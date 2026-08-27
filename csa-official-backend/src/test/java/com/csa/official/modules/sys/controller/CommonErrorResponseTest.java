package com.csa.official.modules.sys.controller;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "csa.cache.type=memory",
        "csa.jwt.secret=01234567890123456789012345678901",
        "csa.jwt.expiration=604800000"
})
@AutoConfigureMockMvc
@Import(CommonErrorResponseTest.FailureController.class)
class CommonErrorResponseTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void malformedJsonReturnsBadRequestJson() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\","))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Request-ID", matchesPattern("[A-Za-z0-9._-]{1,64}")))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.traceId").value(matchesPattern("[A-Za-z0-9._-]{1,64}")))
                .andExpect(jsonPath("$.message").value("请求体格式错误或缺少必要字段"))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void validationFailureReturnsBadRequestJson() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "",
                                  "password": "",
                                  "email": "not-an-email",
                                  "realName": "Tester",
                                  "studentId": "20230001",
                                  "college": "CSA",
                                  "className": "Class 1",
                                  "code": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("参数错误")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("用户名不能为空")));
    }

    @Test
    void missingRequestParamReturnsBadRequestJson() throws Exception {
        mockMvc.perform(post("/api/auth/send-code"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.errorCode").value("MISSING_PARAMETER"))
                .andExpect(jsonPath("$.message").value("缺少必填参数: email"));
    }

    @Test
    void runtimeFailureReturnsHttp500WithStableTraceId() throws Exception {
        mockMvc.perform(get("/api/public/test-errors/runtime")
                        .header("X-Request-ID", "client-request-123"))
                .andExpect(status().isInternalServerError())
                .andExpect(header().string("X-Request-ID", "client-request-123"))
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.traceId").value("client-request-123"))
                .andExpect(jsonPath("$.message").value("系统运行异常，请联系管理员"));
    }

    @Test
    void unsafeIncomingRequestIdIsReplaced() throws Exception {
        mockMvc.perform(get("/api/public/test-errors/runtime")
                        .header("X-Request-ID", "bad request id with spaces"))
                .andExpect(status().isInternalServerError())
                .andExpect(header().string("X-Request-ID", not("bad request id with spaces")))
                .andExpect(jsonPath("$.traceId").value(not("bad request id with spaces")));
    }

    @RestController
    static class FailureController {
        @GetMapping("/api/public/test-errors/runtime")
        String runtimeFailure() {
            throw new IllegalStateException("test failure");
        }
    }
}
