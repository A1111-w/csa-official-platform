package com.csa.official.modules.sys.controller;

import com.csa.official.common.result.R;
import com.csa.official.modules.sys.entity.ContributionLog;
import com.csa.official.modules.sys.entity.User;
import com.csa.official.modules.sys.enums.ContributionType;
import com.csa.official.modules.sys.mapper.ContributionLogMapper;
import com.csa.official.modules.sys.mapper.UserMapper;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ContributionController {

    @Autowired
    private ContributionLogMapper logMapper;
    @Autowired
    private UserMapper userMapper;

    // 1. 获取贡献墙数据 (聚合统计)
    @GetMapping("/public/contribution/wall")
    public R<List<WallVo>> getWall() {
        // 1. 查出所有贡献记录
        List<ContributionLog> allLogs = logMapper.selectList(null);

        // 2. 按用户分组
        Map<Long, List<ContributionLog>> userLogMap = allLogs.stream()
                .collect(Collectors.groupingBy(ContributionLog::getUserId));

        if (userLogMap.isEmpty())
            return R.ok(new ArrayList<>());

        // 3. 查出涉及到的用户信息 (批量查，性能好)
        List<User> users = userMapper.selectBatchIds(userLogMap.keySet());
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 4. 组装 VO
        List<WallVo> result = new ArrayList<>();

        for (Map.Entry<Long, List<ContributionLog>> entry : userLogMap.entrySet()) {
            Long userId = entry.getKey();
            List<ContributionLog> logs = entry.getValue();
            User user = userMap.get(userId);
            if (user == null)
                continue;

            WallVo vo = new WallVo();
            vo.setUserId(userId);
            vo.setRealName(user.getRealName());
            vo.setAvatar(user.getAvatar());
            // 简单处理部门名，如果需要准确部门名，后续可以再查 Dept 表
            vo.setDeptName("");

            // --- 核心：分类统计 ---

            // 1. 官网建设 (DEV): 累加分数 (百分比)
            BigDecimal dev = logs.stream()
                    .filter(l -> ContributionType.DEV.name().equals(l.getType()))
                    .map(ContributionLog::getScore)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            vo.setDevScore(dev);

            // 2. 资源 (RES): 累加次数 (条数)
            // 关键点：这里用了 count()，忽略数据库里的 score 值，只数有多少条记录
            long res = logs.stream()
                    .filter(l -> ContributionType.RES.name().equals(l.getType()))
                    .count();
            vo.setResCount((int) res);

            // 3. 比赛 (COMP): 累加次数
            long comp = logs.stream()
                    .filter(l -> ContributionType.COMP.name().equals(l.getType()))
                    .count();
            vo.setCompCount((int) comp);

            // 4. 运维 (OPS): 累加次数 (包括改轮播图、改简介)
            long ops = logs.stream()
                    .filter(l -> ContributionType.OPS.name().equals(l.getType()))
                    .count();
            vo.setOpsCount((int) ops);

            // 计算一个排序总分 (例如：开发分 + 其他条数总和)
            // 你可以调整权重，比如 vo.setTotalSortScore(dev.add(new BigDecimal(res * 2 + comp * 3
            // ...)));
            vo.setTotalSortScore(dev.add(new BigDecimal(res + comp + ops)));

            result.add(vo);
        }

        // 5. 按总贡献度排序 (从高到低)
        result.sort((a, b) -> b.getTotalSortScore().compareTo(a.getTotalSortScore()));

        return R.ok(result);
    }

    // 2. 获取简单排行 (保留接口，暂未实现具体逻辑)
    @GetMapping("/public/contribution/rank")
    public R<List<RankVo>> getRank() {
        return R.ok(new ArrayList<>());
    }

    // 3. 手动授予贡献 (仅限会长/Root) -> 用于“官网建设”打分
    @PreAuthorize("hasRole('LEVEL_4')")
    @PostMapping("/sys/contribution/award")
    public R<String> award(@RequestBody AwardDto dto) {
        ContributionLog log = new ContributionLog();
        log.setUserId(dto.getUserId());
        log.setType(dto.getType()); // 传入 "DEV"
        log.setScore(dto.getScore()); // 传入 20 (代表20%)
        log.setDetail(dto.getReason());
        logMapper.insert(log);
        return R.ok("授予成功");
    }

    // ================= DTO 定义 (放在类内部) =================

    @Data
    static class WallVo {
        private Long userId;
        private String realName;
        private String avatar;
        private String deptName;

        // 分类统计
        private BigDecimal devScore; // 官网建设 (%)
        private Integer resCount; // 资源贡献 (条)
        private Integer compCount; // 比赛贡献 (条)
        private Integer opsCount; // 首页维护 (次)

        private BigDecimal totalSortScore; // 用于排序的总权重分
    }

    @Data
    static class AwardDto {
        private Long userId;
        private String type; // DEV, RES...
        private BigDecimal score;
        private String reason;
    }

    @Data
    static class RankVo {
        private String username;
        private BigDecimal score;
    }
}