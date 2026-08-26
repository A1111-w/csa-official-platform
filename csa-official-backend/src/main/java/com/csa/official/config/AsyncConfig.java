package com.csa.official.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.beans.factory.annotation.Value;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 邮件发送等阻塞型任务的专用线程池，避免占用 HTTP 请求线程。
     */
    @Bean("mailTaskExecutor")
    public Executor mailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("mail-async-");
        // 队列满时由调用线程执行，保证任务不丢失（退化为同步，起背压作用）
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * 贡献记录落库的专用线程池。
     *
     * <p>和邮件池分开，是为了让两类任务互不影响：邮件积压时不应该拖慢贡献记录，
     * 反之亦然。同样用 CallerRunsPolicy，队列满时退化为同步写，宁可慢也不丢记录。
     */
    @Bean("contributionTaskExecutor")
    public Executor contributionTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(3);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("contribution-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * Git synchronization is blocking I/O and must not consume request, mail or contribution threads.
     */
    @Bean("gitSyncTaskExecutor")
    public Executor gitSyncTaskExecutor(
            @Value("${csa.git.executor-max-size:2}") int configuredMaxSize,
            @Value("${csa.git.executor-queue-capacity:20}") int configuredQueueCapacity) {
        int maxSize = Math.max(1, Math.min(configuredMaxSize, 4));
        int queueCapacity = Math.max(1, Math.min(configuredQueueCapacity, 100));

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(maxSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("git-sync-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
