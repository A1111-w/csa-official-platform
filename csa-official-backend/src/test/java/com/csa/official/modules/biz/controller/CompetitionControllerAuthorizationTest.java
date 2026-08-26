package com.csa.official.modules.biz.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csa.official.common.cache.KeyValueStore;
import com.csa.official.common.constant.RoleConsts;
import com.csa.official.common.util.JwtUtils;
import com.csa.official.modules.biz.entity.Competition;
import com.csa.official.modules.biz.mapper.CompEditorMapper;
import com.csa.official.modules.biz.mapper.CompetitionMapper;
import com.csa.official.modules.biz.service.CompetitionService;
import com.csa.official.modules.sys.entity.User;
import com.csa.official.modules.sys.mapper.ContributionLogMapper;
import com.csa.official.modules.sys.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
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
class CompetitionControllerAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private KeyValueStore keyValueStore;

    @MockBean
    private CompetitionService competitionService;

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private CompetitionMapper competitionMapper;

    @MockBean
    private CompEditorMapper compEditorMapper;

    @MockBean
    private ContributionLogMapper contributionLogMapper;

    @Test
    void publicCompetitionsCanBeViewedWithoutLogin() throws Exception {
        when(competitionService.getPublicCompetitionPage(1, 6)).thenReturn(new Page<>(1, 6));

        mockMvc.perform(get("/api/public/competitions")
                        .param("page", "1")
                        .param("size", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(competitionService).getPublicCompetitionPage(1, 6);
    }

    @Test
    @WithMockUser(username = "minister", authorities = { "ROLE_LEVEL_3" })
    void ministerCanCreateCompetition() throws Exception {
        when(userMapper.selectOne(any())).thenReturn(buildUser(101L, "minister", RoleConsts.MINISTER));

        mockMvc.perform(post("/api/biz/comp/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CompetitionRequest(
                                null,
                                "Competition",
                                "Details"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(competitionService).saveCompetition(any(Competition.class), eq(101L));
    }

    @Test
    void cookieAuthenticatedMutationRequiresCsrfHeader() throws Exception {
        User user = buildUser(105L, "cookie-minister", RoleConsts.MINISTER);
        String token = jwtUtils.generateToken(user, 0);
        when(userMapper.selectOne(any())).thenReturn(user);

        mockMvc.perform(post("/api/biz/comp/save")
                        .cookie(new Cookie("CSA_AUTH_TOKEN", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CompetitionRequest(
                                null,
                                "Competition",
                                "Details"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        verify(competitionService, never()).saveCompetition(any(Competition.class), any());
    }

    @Test
    void cookieAuthenticatedMutationAcceptsMatchingCsrfHeader() throws Exception {
        String csrfToken = "csrf-token";
        User user = buildUser(106L, "cookie-minister", RoleConsts.MINISTER);
        String token = jwtUtils.generateToken(user, 0);
        when(userMapper.selectOne(any())).thenReturn(user);

        mockMvc.perform(post("/api/biz/comp/save")
                        .cookie(new Cookie("CSA_AUTH_TOKEN", token))
                        .cookie(new Cookie("CSA_CSRF_TOKEN", csrfToken))
                        .header("X-CSRF-Token", csrfToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CompetitionRequest(
                                null,
                                "Competition",
                                "Details"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(competitionService).saveCompetition(any(Competition.class), eq(106L));
    }

    @Test
    void revokedBearerTokenCannotCreateCompetition() throws Exception {
        User user = buildUser(107L, "revoked-minister", RoleConsts.MINISTER);
        String token = jwtUtils.generateToken(user, 0);
        String tokenId = jwtUtils.getTokenIdFromToken(token);
        keyValueStore.setString("auth:jwt:revoked:jti:" + tokenId, "1", 1, java.util.concurrent.TimeUnit.HOURS);
        when(userMapper.selectOne(any())).thenReturn(user);

        mockMvc.perform(post("/api/biz/comp/save")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CompetitionRequest(
                                null,
                                "Competition",
                                "Details"))))
                .andExpect(status().isUnauthorized());

        verify(competitionService, never()).saveCompetition(any(Competition.class), any());
    }

    @Test
    void staleMinisterClaimCannotBypassDatabaseRoleDowngrade() throws Exception {
        User tokenUser = buildUser(108L, "downgraded-user", RoleConsts.MINISTER);
        String token = jwtUtils.generateToken(tokenUser, 0);
        User databaseUser = buildUser(108L, "downgraded-user", RoleConsts.MEMBER);
        when(userMapper.selectOne(any())).thenReturn(databaseUser);

        mockMvc.perform(post("/api/biz/comp/save")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CompetitionRequest(
                                null,
                                "Competition",
                                "Details"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        verify(competitionService, never()).saveCompetition(any(Competition.class), any());
    }

    @Test
    @WithMockUser(username = "member", authorities = { "ROLE_LEVEL_1" })
    void memberCannotCreateCompetition() throws Exception {
        when(userMapper.selectOne(any())).thenReturn(buildUser(102L, "member", RoleConsts.MEMBER));

        mockMvc.perform(post("/api/biz/comp/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CompetitionRequest(
                                null,
                                "Competition",
                                "Details"))))
                .andExpect(status().isForbidden());

        verify(competitionService, never()).saveCompetition(any(Competition.class), any());
    }

    @Test
    @WithMockUser(username = "publisher", authorities = { "ROLE_LEVEL_3" })
    void publisherCanGrantEditor() throws Exception {
        when(userMapper.selectOne(any())).thenReturn(buildUser(103L, "publisher", RoleConsts.MINISTER));
        when(competitionMapper.selectById(1L)).thenReturn(buildCompetition(1L, 103L));

        mockMvc.perform(post("/api/biz/comp/grant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GrantRequest(1L, 201L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(competitionService).addEditor(1L, 201L, 103L);
    }

    @Test
    @WithMockUser(username = "other-minister", authorities = { "ROLE_LEVEL_3" })
    void nonPublisherMinisterCannotGrantEditor() throws Exception {
        when(userMapper.selectOne(any())).thenReturn(buildUser(104L, "other-minister", RoleConsts.MINISTER));
        when(competitionMapper.selectById(1L)).thenReturn(buildCompetition(1L, 999L));
        when(compEditorMapper.exists(any())).thenReturn(false);

        mockMvc.perform(post("/api/biz/comp/grant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GrantRequest(1L, 201L))))
                .andExpect(status().isForbidden());

        verify(competitionService, never()).addEditor(any(), any(), any());
    }

    private User buildUser(Long id, String username, int roleLevel) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRoleLevel(roleLevel);
        user.setDeleted(0);
        return user;
    }

    private Competition buildCompetition(Long id, Long publisherId) {
        Competition competition = new Competition();
        competition.setId(id);
        competition.setPublisherId(publisherId);
        competition.setTitle("Competition");
        competition.setContent("Details");
        return competition;
    }

    private record CompetitionRequest(Long id, String title, String content) {
    }

    private record GrantRequest(Long compId, Long targetUserId) {
    }
}
