package com.csa.official.modules.biz.service;

import com.csa.official.common.constant.RoleConsts;
import com.csa.official.common.exception.CsaException;
import com.csa.official.modules.biz.entity.CompEditor;
import com.csa.official.modules.biz.entity.Competition;
import com.csa.official.modules.biz.enums.CompetitionStatusEnum;
import com.csa.official.modules.biz.mapper.CompEditorMapper;
import com.csa.official.modules.biz.mapper.CompetitionMapper;
import com.csa.official.modules.biz.vo.CompetitionDetailVO;
import com.csa.official.modules.sys.entity.User;
import com.csa.official.modules.sys.mapper.UserMapper;
import com.csa.official.modules.sys.service.AuditService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompetitionServiceTest {

    @Mock
    private CompetitionMapper competitionMapper;

    @Mock
    private CompEditorMapper editorMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private CompetitionService competitionService;

    @Test
    void addEditorRejectsUnauthorizedOperator() {
        when(competitionMapper.selectById(1L)).thenReturn(buildCompetition(1L, 200L));
        when(userMapper.selectById(100L)).thenReturn(buildUser(100L, RoleConsts.MINISTER));

        assertThatThrownBy(() -> competitionService.addEditor(1L, 300L, 100L))
                .isInstanceOf(CsaException.class)
                .hasMessageContaining("grant editors");

        verify(editorMapper, never()).insert(any(CompEditor.class));
    }

    @Test
    void presidentCanGrantEditorFromServiceLayer() {
        when(competitionMapper.selectById(1L)).thenReturn(buildCompetition(1L, 200L));
        when(userMapper.selectById(100L)).thenReturn(buildUser(100L, RoleConsts.PRESIDENT));
        when(userMapper.selectById(300L)).thenReturn(buildUser(300L, RoleConsts.MEMBER));
        when(editorMapper.exists(any())).thenReturn(false);

        competitionService.addEditor(1L, 300L, 100L);

        ArgumentCaptor<CompEditor> captor = ArgumentCaptor.forClass(CompEditor.class);
        verify(editorMapper).insert(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getCompetitionId()).isEqualTo(1L);
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getUserId()).isEqualTo(300L);
    }

    @Test
    void summaryStripsHtmlTagsInsteadOfCuttingThemInHalf() {
        String html = "<p>欢迎参加 <strong>ACM 校赛</strong>，报名从今天开始。</p>";

        String summary = CompetitionService.buildSummary(html);

        assertThat(summary).isEqualTo("欢迎参加 ACM 校赛，报名从今天开始。");
        assertThat(summary).doesNotContain("<").doesNotContain(">");
    }

    @Test
    void summaryTruncatesLongContentAndAppendsEllipsis() {
        String longText = "赛".repeat(500);

        String summary = CompetitionService.buildSummary("<p>" + longText + "</p>");

        assertThat(summary).hasSize(CompetitionService.SUMMARY_MAX_LENGTH + 1); // +1 是省略号
        assertThat(summary).endsWith("…");
    }

    @Test
    void summaryHandlesNullAndBlankContent() {
        assertThat(CompetitionService.buildSummary(null)).isEmpty();
        assertThat(CompetitionService.buildSummary("   ")).isEmpty();
    }

    @Test
    void publicDetailHidesUnpublishedCompetition() {
        Competition unpublished = buildCompetition(1L, 200L);
        unpublished.setStatus(CompetitionStatusEnum.UNPUBLISHED);
        when(competitionMapper.selectById(1L)).thenReturn(unpublished);

        // 未发布的活动对未登录用户必须表现为「不存在」，
        // 否则可以靠详情接口枚举出还没公开的比赛
        assertThatThrownBy(() -> competitionService.getPublicCompetitionDetail(1L))
                .isInstanceOf(CsaException.class)
                .extracting(e -> ((CsaException) e).getCode())
                .isEqualTo(404);
    }

    @Test
    void publicDetailReturnsFullContentForPublishedCompetition() {
        Competition published = buildCompetition(1L, 200L);
        published.setStatus(CompetitionStatusEnum.ONGOING);
        published.setContent("<p>完整正文</p>");
        when(competitionMapper.selectById(1L)).thenReturn(published);

        CompetitionDetailVO detail = competitionService.getPublicCompetitionDetail(1L);

        assertThat(detail.getContent()).isEqualTo("<p>完整正文</p>");
        assertThat(detail.getCanEdit()).isFalse();
        assertThat(detail.getCanGrant()).isFalse();
    }

    /**
     * status 必须以数字 code 返回，不能是枚举名。
     *
     * <p>Jackson 默认把枚举序列化成名字（"FINISHED"），而前端编辑弹窗用
     * {@code Number.parseInt(String(status)) || 1} 还原状态，
     * 遇到 "FINISHED" 会得到 NaN 再回退成 1 —— 结果是编辑一个已结束的比赛，
     * 保存后状态被悄悄改成「进行中」。这个用例把契约钉死。
     */
    @Test
    void detailReturnsNumericStatusCodeNotEnumName() {
        Competition finished = buildCompetition(1L, 200L);
        finished.setStatus(CompetitionStatusEnum.FINISHED);
        when(competitionMapper.selectById(1L)).thenReturn(finished);

        CompetitionDetailVO detail = competitionService.getPublicCompetitionDetail(1L);

        assertThat(detail.getStatus()).isEqualTo(CompetitionStatusEnum.FINISHED.getCode());
        assertThat(detail.getStatus()).isEqualTo(2);
    }

    @Test
    void detailOfMissingCompetitionReports404() {
        when(competitionMapper.selectById(9L)).thenReturn(null);

        assertThatThrownBy(() -> competitionService.getPublicCompetitionDetail(9L))
                .isInstanceOf(CsaException.class)
                .extracting(e -> ((CsaException) e).getCode())
                .isEqualTo(404);
    }

    private Competition buildCompetition(Long id, Long publisherId) {
        Competition competition = new Competition();
        competition.setId(id);
        competition.setPublisherId(publisherId);
        return competition;
    }

    private User buildUser(Long id, int roleLevel) {
        User user = new User();
        user.setId(id);
        user.setRoleLevel(roleLevel);
        user.setDeleted(0);
        return user;
    }
}
