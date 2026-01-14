package com.csa.official.modules.sys.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csa.official.common.constant.RoleConsts;
import com.csa.official.modules.biz.entity.CompEditor;
import com.csa.official.modules.biz.entity.Competition;
import com.csa.official.modules.biz.mapper.CompEditorMapper;
import com.csa.official.modules.biz.mapper.CompetitionMapper;
import com.csa.official.common.util.SecurityUtils;
import com.csa.official.modules.sys.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("csaSec") // 这里的名字很重要，SpEL 表达式要用
public class CsaSecurityService {

    @Autowired
    private CompetitionMapper competitionMapper;
    @Autowired
    private CompEditorMapper editorMapper;

    // 判断是否有权编辑比赛
    public boolean canEditCompetition(Long compId) {
        User user;
        try {
            user = SecurityUtils.getCurrentUser();
        } catch (Exception e) {
            return false; // 未登录或 Token 无效，直接拒绝
        }
        Long userId = user.getId();
        if (userId == null)
            return false;

        // 1. 部长特权
        if (user.getRoleLevel() >= RoleConsts.MINISTER)
            return true;

        // 2. 比赛是否存在
        Competition comp = competitionMapper.selectById(compId);
        if (comp == null)
            return false;

        // 3. 发布者本人
        if (comp.getPublisherId().equals(userId))
            return true;

        // 4. 授权名单
        return editorMapper.exists(new LambdaQueryWrapper<CompEditor>()
                .eq(CompEditor::getCompetitionId, compId)
                .eq(CompEditor::getUserId, userId));
    }
}
