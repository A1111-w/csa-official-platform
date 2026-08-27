package com.csa.official.modules.biz.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 竞赛详情，包含完整的富文本正文。
 *
 * <p>和 {@link CompetitionListVO} 的区别就是多了 {@code content}。
 * 编辑弹窗和详情页按 id 单独拉这个接口，列表接口则只传摘要。
 */
@Data
public class CompetitionDetailVO {
    private Long id;
    private String title;
    /** 完整正文，入库前已经过 Jsoup 白名单清洗。 */
    private String content;
    private String coverImg;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long publisherId;
    /** 状态码 0未发布/1进行中/2已结束，理由同 {@link CompetitionListVO#getStatus()}。 */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Boolean canEdit;
    private Boolean canGrant;
}
