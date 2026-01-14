package com.csa.official.common.aspect;

import com.csa.official.common.annotation.LogContribution;
import com.csa.official.common.util.SecurityUtils;
import com.csa.official.modules.sys.entity.ContributionLog;
import com.csa.official.modules.sys.mapper.ContributionLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Aspect
@Component
public class ContributionAspect {

    @Autowired
    private ContributionLogMapper logMapper;

    // 拦截加了 @LogContribution 的方法，且执行成功后触发
    @AfterReturning(pointcut = "@annotation(contLog)", returning = "result")
    public void doAfterReturning(JoinPoint point, LogContribution contLog, Object result) {
        try {
            Long userId = SecurityUtils.getUserId();

            ContributionLog record = new ContributionLog();
            record.setUserId(userId);
            record.setType(contLog.type().name());
            record.setScore(BigDecimal.ONE); // 默认 +1
            record.setDetail(contLog.detail().isEmpty() ? point.getSignature().getName() : contLog.detail());

            logMapper.insert(record);
            log.info("自动记录贡献: User={} Type={}", userId, contLog.type());
        } catch (Exception e) {
            log.error("贡献记录失败", e); // 记录失败不影响主业务
        }
    }
}
