package com.csa.official.modules.sys.service;

import com.csa.official.modules.sys.entity.ContributionLog;
import com.csa.official.modules.sys.mapper.ContributionLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 贡献记录异步落库组件。
 *
 * <p>为什么要异步：贡献记录是「旁路」信息，用户发布资源、保存竞赛时并不关心它写没写成功。
 * 原先 {@code ContributionAspect} 在 {@code @AfterReturning} 里直接同步 insert，
 * 等于给每个被切到的写接口都额外挂了一次数据库往返，拖长了接口 RT。
 *
 * <p>为什么单独成 Bean：和 {@link AsyncMailSender} 同理，{@code @Async} 走 Spring 代理，
 * 同类内部自调用不会生效，必须从外部 Bean 调进来。
 *
 * <p>注意：调用方必须在请求线程上先取好 {@code userId} 再传进来。
 * {@code SecurityContext} 默认是 ThreadLocal，异步线程里取不到当前登录用户。
 */
@Slf4j
@Component
public class ContributionLogWriter {

    private final ContributionLogMapper logMapper;

    public ContributionLogWriter(ContributionLogMapper logMapper) {
        this.logMapper = logMapper;
    }

    @Async("contributionTaskExecutor")
    public void write(Long userId, String type, String detail) {
        try {
            ContributionLog record = new ContributionLog();
            record.setUserId(userId);
            record.setType(type);
            record.setScore(BigDecimal.ONE); // 默认 +1
            record.setDetail(detail);

            logMapper.insert(record);
            log.info("自动记录贡献: User={} Type={}", userId, type);
        } catch (Exception e) {
            // 异步线程中的异常无法回传给请求方，仅记录；贡献记录失败不影响主业务。
            log.error("贡献记录失败: User={} Type={}", userId, type, e);
        }
    }
}
