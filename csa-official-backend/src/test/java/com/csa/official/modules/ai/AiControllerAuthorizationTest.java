package com.csa.official.modules.ai;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AiControllerAuthorizationTest {
    @Autowired MockMvc mvc;
    @MockBean KnowledgeService knowledge;
    @MockBean AgentService agent;
    @MockBean EvaluationService evaluation;
    @Test void anonymousCannotReadAiStatusOrEvaluations() throws Exception {
        mvc.perform(get("/api/ai/status")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/ai/evaluations/example")).andExpect(status().isUnauthorized());
    }
    @Test @WithMockUser(authorities="ROLE_LEVEL_0")
    void guestCannotUseAgent() throws Exception {
        mvc.perform(post("/api/ai/runs").contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"Java\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(agent);
    }
    @Test @WithMockUser(authorities="ROLE_LEVEL_1")
    void memberCannotIndexOrEvaluate() throws Exception {
        mvc.perform(post("/api/ai/documents").contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"x\",\"content\":\"text\",\"source\":\"\",\"minRoleLevel\":1}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/ai/documents/example")).andExpect(status().isForbidden());
        mvc.perform(get("/api/ai/evaluations/example")).andExpect(status().isForbidden());
        mvc.perform(post("/api/ai/evaluations").contentType(MediaType.APPLICATION_JSON)
                .content("{\"cases\":[{\"question\":\"Java\",\"expectedDocumentIds\":[],\"expectedTools\":[\"search_knowledge\"],\"expectedAnswerTerms\":[]}]}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(knowledge,evaluation);
    }
    @Test @WithMockUser(authorities="ROLE_LEVEL_1")
    void statusReadsConfigurationWithoutCallingProvider() throws Exception {
        when(agent.status()).thenReturn(new AiModels.Status("offline","extractive-bm25","bm25"));
        mvc.perform(get("/api/ai/status")).andExpect(status().isOk()).andExpect(jsonPath("$.data.mode").value("offline"));
        verify(agent).status(); verifyNoMoreInteractions(agent);
    }
}
