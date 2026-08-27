package com.csa.official.modules.sys.controller;

import com.csa.official.common.constant.RoleConsts;
import com.csa.official.modules.sys.entity.Carousel;
import com.csa.official.modules.sys.entity.StoredFile;
import com.csa.official.modules.sys.entity.User;
import com.csa.official.modules.sys.mapper.CarouselMapper;
import com.csa.official.modules.sys.mapper.StoredFileMapper;
import com.csa.official.modules.sys.mapper.UserMapper;
import com.csa.official.modules.sys.service.AuditService;
import com.csa.official.modules.sys.service.ContributionLogWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
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
class CarouselControllerAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CarouselMapper carouselMapper;

    @MockBean
    private StoredFileMapper storedFileMapper;

    @MockBean
    private AuditService auditService;

    @MockBean
    private ContributionLogWriter contributionLogWriter;

    @MockBean
    private UserMapper userMapper;

    @Test
    void publicListOnlyReturnsRenderingFields() throws Exception {
        Carousel carousel = carousel(1L, 1);
        when(carouselMapper.selectList(any())).thenReturn(List.of(carousel));

        mockMvc.perform(get("/api/public/carousel/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].title").value("首页活动"))
                .andExpect(jsonPath("$.data[0].status").doesNotExist())
                .andExpect(jsonPath("$.data[0].sortOrder").doesNotExist());
    }

    @Test
    @WithMockUser(username = "minister", authorities = {"ROLE_LEVEL_3"})
    void ministerCannotOpenAdminList() throws Exception {
        mockMvc.perform(get("/api/sys/carousel/list"))
                .andExpect(status().isForbidden());

        verify(carouselMapper, never()).selectList(any());
    }

    @Test
    @WithMockUser(username = "president", authorities = {"ROLE_LEVEL_4"})
    void presidentCanOpenAdminList() throws Exception {
        when(carouselMapper.selectList(any())).thenReturn(List.of(carousel(7L, 0)));

        mockMvc.perform(get("/api/sys/carousel/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(7))
                .andExpect(jsonPath("$.data[0].status").value(0))
                .andExpect(jsonPath("$.data[0].sortOrder").value(3));
    }

    @Test
    @WithMockUser(username = "president", authorities = {"ROLE_LEVEL_4"})
    void saveUsesWhitelistedFieldsAndNormalizesValues() throws Exception {
        when(userMapper.selectOne(any())).thenReturn(user(10L, RoleConsts.PRESIDENT));
        doAnswer(invocation -> {
            Carousel carousel = invocation.getArgument(0);
            carousel.setId(11L);
            return 1;
        }).when(carouselMapper).insert(any(Carousel.class));

        mockMvc.perform(post("/api/sys/carousel/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "  招新开放日  ",
                                  "imgUrl": "https://cdn.example.com/banner.png",
                                  "targetUrl": " /register ",
                                  "sortOrder": 8,
                                  "status": 1,
                                  "deleted": 1,
                                  "createTime": "2020-01-01T00:00:00"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Carousel> captor = ArgumentCaptor.forClass(Carousel.class);
        verify(carouselMapper).insert(captor.capture());
        Carousel saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo(11L);
        assertThat(saved.getTitle()).isEqualTo("招新开放日");
        assertThat(saved.getTargetUrl()).isEqualTo("/register");
        assertThat(saved.getDeleted()).isNull();
        assertThat(saved.getCreateTime()).isNull();
    }

    @Test
    @WithMockUser(username = "president", authorities = {"ROLE_LEVEL_4"})
    void unsafeTargetUrlIsRejected() throws Exception {
        mockMvc.perform(post("/api/sys/carousel/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SaveRequest(
                                null,
                                "首页活动",
                                "https://cdn.example.com/banner.png",
                                "javascript:alert(1)",
                                0,
                                1))))
                .andExpect(status().isBadRequest());

        verify(carouselMapper, never()).insert(any(Carousel.class));
    }

    @Test
    @WithMockUser(username = "president", authorities = {"ROLE_LEVEL_4"})
    void backslashCannotTurnInternalTargetIntoExternalNavigation() throws Exception {
        mockMvc.perform(post("/api/sys/carousel/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SaveRequest(
                                null,
                                "首页活动",
                                "https://cdn.example.com/banner.png",
                                "/\\evil.example",
                                0,
                                1))))
                .andExpect(status().isBadRequest());

        verify(carouselMapper, never()).insert(any(Carousel.class));
    }

    @Test
    @WithMockUser(username = "president", authorities = {"ROLE_LEVEL_4"})
    void updateCanClearTargetUrl() throws Exception {
        Carousel existing = carousel(7L, 1);
        when(carouselMapper.selectById(7L)).thenReturn(existing);
        when(userMapper.selectOne(any())).thenReturn(user(10L, RoleConsts.PRESIDENT));
        when(carouselMapper.updateManagedFields(
                7L,
                "首页活动",
                "https://cdn.example.com/banner.png",
                null,
                3,
                1)).thenReturn(1);

        mockMvc.perform(post("/api/sys/carousel/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SaveRequest(
                                7L,
                                "首页活动",
                                "https://cdn.example.com/banner.png",
                                " ",
                                3,
                                1))))
                .andExpect(status().isOk());

        verify(carouselMapper).updateManagedFields(
                7L,
                "首页活动",
                "https://cdn.example.com/banner.png",
                null,
                3,
                1);
    }

    @Test
    @WithMockUser(username = "president", authorities = {"ROLE_LEVEL_4"})
    void cannotPublishAnotherUsersPrivateUpload() throws Exception {
        when(userMapper.selectOne(any())).thenReturn(user(10L, RoleConsts.PRESIDENT));
        StoredFile metadata = new StoredFile();
        metadata.setOwnerUserId(99L);
        metadata.setStorageKey("/files/99/private.png");
        metadata.setExtension("png");
        metadata.setStatus("ACTIVE");
        when(storedFileMapper.findActiveByStorageKey("/files/99/private.png")).thenReturn(metadata);

        mockMvc.perform(post("/api/sys/carousel/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SaveRequest(
                                null,
                                "首页活动",
                                "/files/99/private.png",
                                "/competitions",
                                0,
                                1))))
                .andExpect(status().isForbidden());

        verify(carouselMapper, never()).insert(any(Carousel.class));
    }

    private Carousel carousel(Long id, int status) {
        Carousel carousel = new Carousel();
        carousel.setId(id);
        carousel.setTitle("首页活动");
        carousel.setImgUrl("https://cdn.example.com/banner.png");
        carousel.setTargetUrl("/competitions");
        carousel.setSortOrder(3);
        carousel.setStatus(status);
        carousel.setCreateTime(LocalDateTime.of(2026, 8, 27, 9, 0));
        carousel.setUpdateTime(LocalDateTime.of(2026, 8, 27, 9, 30));
        return carousel;
    }

    private User user(Long id, int roleLevel) {
        User user = new User();
        user.setId(id);
        user.setUsername("president");
        user.setRoleLevel(roleLevel);
        user.setDeleted(0);
        return user;
    }

    private record SaveRequest(Long id,
                               String title,
                               String imgUrl,
                               String targetUrl,
                               Integer sortOrder,
                               Integer status) {
    }
}
