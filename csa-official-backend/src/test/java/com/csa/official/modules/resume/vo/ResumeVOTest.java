package com.csa.official.modules.resume.vo;

import com.csa.official.modules.resume.entity.Resume;
import com.csa.official.modules.resume.enums.ResumeStatusEnum;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 钉住简历接口的对外契约。
 *
 * <p>背景：`Resume.status` 是枚举，Jackson 默认序列化成名字 `"APPROVED"`，
 * 但前端 `services/resume.ts` 声明的是 `status: number`，
 * 并用 `RESUME_STATUS.APPROVED === 2` 比较 —— 字符串永远等不上数字，
 * 导致简历页状态标签一直显示成「草稿」。ResumeVO 改成返回 code 修掉了这个问题。
 */
class ResumeVOTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void statusIsSerializedAsNumericCodeNotEnumName() throws Exception {
        Resume resume = new Resume();
        resume.setId(1L);
        resume.setStatus(ResumeStatusEnum.APPROVED);

        String json = objectMapper.writeValueAsString(ResumeVO.from(resume));

        assertThat(json).contains("\"status\":2");
        assertThat(json).doesNotContain("APPROVED");
    }

    @Test
    void everyStatusMapsToItsCode() {
        for (ResumeStatusEnum status : ResumeStatusEnum.values()) {
            Resume resume = new Resume();
            resume.setStatus(status);
            assertThat(ResumeVO.from(resume).getStatus()).isEqualTo(status.getCode());
        }
    }

    @Test
    void doesNotLeakUserIdOrDeletedFlag() throws Exception {
        Resume resume = new Resume();
        resume.setId(1L);
        resume.setUserId(42L);
        resume.setDeleted(0);
        resume.setStatus(ResumeStatusEnum.DRAFT);

        String json = objectMapper.writeValueAsString(ResumeVO.from(resume));

        assertThat(json).doesNotContain("userId").doesNotContain("deleted");
    }

    @Test
    void nullResumeStaysNullSoFrontendFallsBackToDraft() {
        // 用户还没建过简历时 getMyResume 返回 null，不能在这里 NPE
        assertThat(ResumeVO.from(null)).isNull();
    }

    @Test
    void nullStatusIsTolerated() {
        Resume resume = new Resume();
        resume.setId(1L);

        assertThat(ResumeVO.from(resume).getStatus()).isNull();
    }
}
