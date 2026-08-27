package com.csa.official.modules.resume.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csa.official.modules.resume.service.ResumeGitSyncService;
import com.csa.official.modules.resume.service.ResumeService;
import com.csa.official.modules.resume.vo.ResumeGitSyncVO;
import com.csa.official.modules.resume.vo.ResumeReviewListVO;
import com.csa.official.modules.sys.entity.User;
import com.csa.official.modules.sys.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
class ResumeControllerAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ResumeService resumeService;

    @MockBean
    private ResumeGitSyncService resumeGitSyncService;

    @MockBean
    private UserMapper userMapper;

    @Test
    @WithMockUser(username = "core", authorities = {"ROLE_LEVEL_2"})
    void coreMemberCannotOpenReviewQueue() throws Exception {
        mockMvc.perform(get("/api/resume/reviews"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        verify(resumeService, never()).getReviewPage(1, null, 1);
    }

    @Test
    @WithMockUser(username = "minister", authorities = {"ROLE_LEVEL_3"})
    void ministerCanOpenReviewQueue() throws Exception {
        when(resumeService.getReviewPage(1, 20, 1))
                .thenReturn(new Page<ResumeReviewListVO>(1, 20));

        mockMvc.perform(get("/api/resume/reviews")
                        .param("page", "1")
                        .param("size", "20")
                        .param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(resumeService).getReviewPage(1, 20, 1);
    }

    @Test
    @WithMockUser(username = "member", authorities = {"ROLE_LEVEL_1"})
    void registeredMemberCannotStartGitSync() throws Exception {
        mockMvc.perform(post("/api/resume/git-sync").with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        verify(resumeGitSyncService, never()).startMySync(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @WithMockUser(username = "core", authorities = {"ROLE_LEVEL_2"})
    void coreMemberCanReadGitSyncStatus() throws Exception {
        User user = new User();
        user.setId(101L);
        user.setUsername("core");
        when(userMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(user);
        when(resumeGitSyncService.getMyStatus(101L)).thenReturn(ResumeGitSyncVO.syncing());

        mockMvc.perform(get("/api/resume/git-sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("SYNCING"));

        verify(resumeGitSyncService).getMyStatus(101L);
    }
}
