package com.csa.official.modules.biz.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csa.official.common.exception.CsaException;
import com.csa.official.modules.biz.entity.CompEditor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csa.official.modules.biz.entity.Competition;
import com.csa.official.modules.biz.mapper.CompEditorMapper;
import com.csa.official.modules.biz.mapper.CompetitionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

@Service
public class CompetitionService {

    @Autowired
    private CompetitionMapper competitionMapper;
    @Autowired
    private CompEditorMapper editorMapper;

    /**
     * 发布或修改比赛
     * 注意：权限检查已上移至 Controller (@PreAuthorize)，此处专注于业务逻辑
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveCompetition(Competition comp, Long userId) {
        // 1. 逻辑校验：结束时间必须晚于开始时间
        if (comp.getStartTime() != null && comp.getEndTime() != null) {
            if (comp.getEndTime().isBefore(comp.getStartTime())) {
                throw new CsaException("比赛结束时间不能早于开始时间！");
            }
        }

        // 2. 安全校验：XSS 过滤
        if (comp.getContent() != null) {
            String safeContent = Jsoup.clean(comp.getContent(), Safelist.basicWithImages());
            comp.setContent(safeContent);
        }

        // 3. 保存逻辑
        if (comp.getId() == null) {
            comp.setPublisherId(userId);
            competitionMapper.insert(comp);
        } else {
            competitionMapper.updateById(comp);
        }
    }

    // 授权给其他成员
    @Transactional(rollbackFor = Exception.class)
    public void addEditor(Long compId, Long targetUserId, Long operatorId) {
        // 这里依然保留基本的校验，或者也可以完全信任 Controller 的 hasRole('LEVEL_3')
        // 为了业务严谨性，这里只做简单的判空
        if (competitionMapper.selectById(compId) == null) {
            throw new CsaException("比赛不存在");
        }

        // 防止重复授权
        if (editorMapper.exists(new LambdaQueryWrapper<CompEditor>()
                .eq(CompEditor::getCompetitionId, compId)
                .eq(CompEditor::getUserId, targetUserId))) {
            return; // 已经授权过了，直接返回
        }

        CompEditor editor = new CompEditor();
        editor.setCompetitionId(compId);
        editor.setUserId(targetUserId);
        editorMapper.insert(editor);
    }

    /**
     * 分页查询比赛列表
     */
    public Page<Competition> getCompetitionPage(int page, int size) {
        Page<Competition> pageParam = new Page<>(page, size);
        return competitionMapper.selectPage(pageParam, null);
    }

}
