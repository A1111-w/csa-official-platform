package com.csa.official.modules.resume.vo;

import com.csa.official.modules.resume.entity.Resume;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 简历对外视图。
 *
 * <p>除了屏蔽 {@code userId}、{@code deleted} 这些持久层字段，这个 VO 还修掉了一个
 * 真实存在的前后端契约 bug：
 *
 * <p>{@code Resume.status} 是 {@code ResumeStatusEnum}，而 Jackson 默认把枚举序列化成
 * <b>名字</b>，也就是 {@code "status":"APPROVED"}。但前端 {@code services/resume.ts} 里
 * 声明的是 {@code status: number}，并用 {@code RESUME_STATUS.APPROVED === 2} 去比较 ——
 * {@code "APPROVED" === 2} 恒为 false，所以简历页的状态标签永远走不到「已通过 / 已驳回 /
 * 待审核」分支，一律显示成草稿。
 *
 * <p>这里显式返回枚举的 {@code code}（0草稿/1待审核/2已通过/3已驳回），
 * 和数据库里存的值、以及前端本来就期望的类型对齐。
 */
@Data
public class ResumeVO {

    private Long id;
    private String content;
    private String gitRepoUrl;
    /** 状态码 0草稿/1待审核/2已通过/3已驳回，对应 {@code ResumeStatusEnum.code}。 */
    private Integer status;
    private String rejectReason;
    private Long auditBy;
    private LocalDateTime auditTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /**
     * @param resume 可能为 null（用户还没建过简历），此时返回 null，
     *               前端按「草稿」处理
     */
    public static ResumeVO from(Resume resume) {
        if (resume == null) {
            return null;
        }

        ResumeVO vo = new ResumeVO();
        vo.setId(resume.getId());
        vo.setContent(resume.getContent());
        vo.setGitRepoUrl(resume.getGitRepoUrl());
        vo.setStatus(resume.getStatus() == null ? null : resume.getStatus().getCode());
        vo.setRejectReason(resume.getRejectReason());
        vo.setAuditBy(resume.getAuditBy());
        vo.setAuditTime(resume.getAuditTime());
        vo.setCreateTime(resume.getCreateTime());
        vo.setUpdateTime(resume.getUpdateTime());
        return vo;
    }
}
