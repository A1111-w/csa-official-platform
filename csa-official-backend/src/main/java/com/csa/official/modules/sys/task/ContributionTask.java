package com.csa.official.modules.sys.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csa.official.modules.sys.entity.ContributionLog;
import com.csa.official.modules.sys.entity.SysConfig;
import com.csa.official.modules.sys.enums.ContributionType;
import com.csa.official.modules.sys.mapper.ContributionLogMapper;
import com.csa.official.modules.sys.mapper.SysConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Component
public class ContributionTask {

    @Autowired
    private SysConfigMapper configMapper;
    @Autowired
    private ContributionLogMapper logMapper;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // Redis Key: 记录上一次发奖的“修改时间”
    private static final String LAST_REWARD_TIME_KEY = "csa:contribution:intro:last_reward_time";

    /**
     * 每天凌晨 4 点执行一次检查
     * cron = "秒 分 时 日 月 周"
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void settleIntroContribution() {
        log.info("开始结算【协会介绍】贡献度...");

        // 1. 查出当前的介绍配置
        SysConfig config = configMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, "CSA_INTRO"));

        if (config == null || config.getUpdateBy() == null) {
            log.info("未找到协会介绍配置或无人修改，跳过。");
            return;
        }

        // 2. 检查时间是否超过 7 天
        // 如果 (修改时间 + 7天) 依然在 (当前时间) 之前，说明已经稳了
        LocalDateTime stableLine = config.getUpdateTime().plusDays(7);
        if (LocalDateTime.now().isBefore(stableLine)) {
            log.info("当前版本修改于 [{}], 尚未满 7 天，暂不结算。", config.getUpdateTime());
            return;
        }

        // 3. 检查是否已经发过奖了 (幂等性检查)
        // 我们对比 Redis 里存的“上次发奖版本的修改时间”
        String lastRewardTimeStr = (String) redisTemplate.opsForValue().get(LAST_REWARD_TIME_KEY);
        String currentUpdateTimeStr = config.getUpdateTime().toString();

        if (currentUpdateTimeStr.equals(lastRewardTimeStr)) {
            log.info("当前版本 [{}] 的贡献已发放，跳过。", currentUpdateTimeStr);
            return;
        }

        // 4. 发放奖励 (记录到贡献表)
        ContributionLog logEntry = new ContributionLog();
        logEntry.setUserId(config.getUpdateBy());
        logEntry.setType(ContributionType.OPS.name());
        logEntry.setScore(new BigDecimal("1.00")); // 假设改一次且存活7天，给 5 分
        logEntry.setDetail("维护协会介绍 (版本存活超7天)");
        logMapper.insert(logEntry);

        // 5. 更新 Redis 标记，防止明天重复发
        redisTemplate.opsForValue().set(LAST_REWARD_TIME_KEY, currentUpdateTimeStr);

        log.info("结算完成！用户ID [{}] 获得贡献分。", config.getUpdateBy());
    }
}
