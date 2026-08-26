package com.csa.official.common.aspect;

import com.csa.official.common.annotation.LogContribution;
import com.csa.official.common.util.SecurityUtils;
import com.csa.official.modules.sys.service.ContributionLogWriter;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class ContributionAspect {

    @Autowired
    private ContributionLogWriter contributionLogWriter;

    // 拦截加了 @LogContribution 的方法，且执行成功后触发
    @AfterReturning(pointcut = "@annotation(contLog)", returning = "result")
    public void doAfterReturning(JoinPoint point, LogContribution contLog, Object result) {
        try {
            // userId 必须在请求线程上取：SecurityContext 是 ThreadLocal，
            // 异步线程里拿不到当前登录用户。
            Long userId = SecurityUtils.getUserId();
            String detail = contLog.detail().isEmpty() ? point.getSignature().getName() : contLog.detail();

            // 实际写库交给异步线程，不占用 HTTP 请求线程。
            contributionLogWriter.write(userId, contLog.type().name(), detail);
        } catch (Exception e) {
            log.error("贡献记录提交失败", e); // 记录失败不影响主业务
        }
    }
}
