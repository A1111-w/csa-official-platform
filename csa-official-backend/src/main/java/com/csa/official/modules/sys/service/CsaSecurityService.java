package com.csa.official.modules.sys.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csa.official.common.constant.RoleConsts;
import com.csa.official.common.util.SecurityUtils;
import com.csa.official.modules.biz.entity.CompEditor;
import com.csa.official.modules.biz.entity.Competition;
import com.csa.official.modules.biz.mapper.CompEditorMapper;
import com.csa.official.modules.biz.mapper.CompetitionMapper;
import com.csa.official.modules.sys.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("csaSec")
public class CsaSecurityService {

    @Autowired
    private CompetitionMapper competitionMapper;

    @Autowired
    private CompEditorMapper editorMapper;

    public boolean canCreateCompetition() {
        User user = getCurrentUserSafely();
        return user != null
                && user.getRoleLevel() != null
                && user.getRoleLevel() >= RoleConsts.MINISTER;
    }

    public boolean canEditCompetition(Long compId) {
        User user = getCurrentUserSafely();
        if (user == null || compId == null || user.getId() == null) {
            return false;
        }

        if (user.getRoleLevel() != null && user.getRoleLevel() >= RoleConsts.PRESIDENT) {
            return true;
        }

        Competition competition = competitionMapper.selectById(compId);
        if (competition == null) {
            return false;
        }

        if (competition.getPublisherId() != null && competition.getPublisherId().equals(user.getId())) {
            return true;
        }

        return editorMapper.exists(new LambdaQueryWrapper<CompEditor>()
                .eq(CompEditor::getCompetitionId, compId)
                .eq(CompEditor::getUserId, user.getId()));
    }

    public boolean canGrantCompetitionEditor(Long compId) {
        User user = getCurrentUserSafely();
        if (user == null || compId == null || user.getId() == null) {
            return false;
        }

        if (user.getRoleLevel() != null && user.getRoleLevel() >= RoleConsts.PRESIDENT) {
            return true;
        }

        Competition competition = competitionMapper.selectById(compId);
        return competition != null
                && competition.getPublisherId() != null
                && competition.getPublisherId().equals(user.getId());
    }

    private User getCurrentUserSafely() {
        try {
            return SecurityUtils.getCurrentUser();
        } catch (Exception e) {
            return null;
        }
    }
}
