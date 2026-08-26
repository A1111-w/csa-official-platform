package com.csa.official.modules.sys.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csa.official.common.constant.AccountStatus;
import com.csa.official.modules.sys.entity.User;
import com.csa.official.modules.sys.mapper.ContributionLogMapper;
import com.csa.official.modules.sys.mapper.UserMapper;
import com.csa.official.modules.sys.service.ContributionQueryService;
import com.csa.official.modules.sys.service.ContributionService;
import com.csa.official.modules.sys.vo.ContributionAwardVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "csa.cache.type=memory",
        "csa.jwt.secret=01234567890123456789012345678901",
        "csa.jwt.expiration=604800000"
})
@AutoConfigureMockMvc
class ContributionControllerAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ContributionService contributionService;

    @MockBean
    private ContributionQueryService contributionQueryService;

    @MockBean
    private ContributionLogMapper contributionLogMapper;

    @MockBean
    private UserMapper userMapper;

    @Test
    @WithMockUser(username = "president", authorities = {"ROLE_LEVEL_4"})
    void presidentCanCreateManualContribution() throws Exception {
        when(userMapper.selectOne(any())).thenReturn(operator());

        mockMvc.perform(post("/api/sys/contribution/award")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AwardRequest(
                                7L, "DEV", "12.50", "完成官网无障碍改造"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(contributionService).award(any(), eq(99L));
    }

    @Test
    @WithMockUser(username = "president", authorities = {"ROLE_LEVEL_4"})
    void invalidManualContributionReturns400() throws Exception {
        when(userMapper.selectOne(any())).thenReturn(operator());

        mockMvc.perform(post("/api/sys/contribution/award")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AwardRequest(
                                7L, "DEV", "0", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));

        verify(contributionService, never()).award(any(), any());
    }

    @Test
    @WithMockUser(username = "minister", authorities = {"ROLE_LEVEL_3"})
    void ministerCannotCreateManualContribution() throws Exception {
        mockMvc.perform(post("/api/sys/contribution/award")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AwardRequest(
                                7L, "DEV", "12.50", "完成官网无障碍改造"))))
                .andExpect(status().isForbidden());

        verify(contributionService, never()).award(any(), any());
    }

    @Test
    @WithMockUser(username = "minister", authorities = {"ROLE_LEVEL_3"})
    void ministerCannotReadContributionHistory() throws Exception {
        mockMvc.perform(get("/api/sys/contribution/awards"))
                .andExpect(status().isForbidden());

        verify(contributionService, never()).listAwards(any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(username = "president", authorities = {"ROLE_LEVEL_4"})
    void presidentCanPageContributionHistory() throws Exception {
        when(contributionService.listAwards(1, 20, "张", "DEV", "MANUAL"))
                .thenReturn(new Page<ContributionAwardVO>(1, 20, 0));

        mockMvc.perform(get("/api/sys/contribution/awards")
                        .param("page", "1")
                        .param("size", "20")
                        .param("keyword", "张")
                        .param("type", "DEV")
                        .param("source", "MANUAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.current").value(1));

        verify(contributionService).listAwards(1, 20, "张", "DEV", "MANUAL");
    }

    private User operator() {
        User user = new User();
        user.setId(99L);
        user.setUsername("president");
        user.setRoleLevel(4);
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setDeleted(0);
        return user;
    }

    private record AwardRequest(Long userId, String type, String score, String reason) {
    }
}
