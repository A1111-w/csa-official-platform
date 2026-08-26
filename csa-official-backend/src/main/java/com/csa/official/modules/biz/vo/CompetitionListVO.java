package com.csa.official.modules.biz.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 竞赛列表项。
 *
 * <p>这里刻意<b>不</b>返回完整的 {@code content} 富文本正文，只返回 {@code summary} 摘要。
 * 原因：列表页每条只渲染 120 字左右的摘要，但正文是一整段 HTML，
 * 一页 10 条就可能传几百 KB，其中 99% 前端根本不显示。
 * 需要正文时走 {@code GET /api/biz/comp/{id}} 详情接口单独取。
 */
@Data
public class CompetitionListVO {
    private Long id;
    private String title;
    /** 正文的纯文本摘要，由后端截断，不含 HTML 标签。 */
    private String summary;
    private String coverImg;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long publisherId;
    /**
     * 状态码 0未发布/1进行中/2已结束，对应 {@code CompetitionStatusEnum.code}。
     *
     * <p>这里返回数字而不是枚举名，是为了和前端契约对齐：Jackson 默认把枚举序列化成名字
     * （{@code "ONGOING"}），而前端编辑弹窗用
     * {@code typeof status === "number" ? status : Number.parseInt(String(status)) || 1}
     * 还原状态 —— 遇到 {@code "FINISHED"} 时 parseInt 得到 NaN，回退成 1，
     * 结果是「编辑一个已结束的比赛，保存后它会被悄悄改成进行中」。返回 code 即可避免。
     */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Boolean canEdit;
    private Boolean canGrant;
}
