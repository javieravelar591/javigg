package dev.javis.javigg.config;

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Dedicated, bounded pool for parallel Riot calls. Riot calls block (HTTP I/O plus rate-limit
 * waits), so they must not run on the shared ForkJoin common pool.
 */
@Configuration
public class RiotExecutorConfig {

    public static final String RIOT_EXECUTOR = "riotExecutor";

    @Bean(name = RIOT_EXECUTOR)
    public ThreadPoolTaskExecutor riotExecutor(@Value("${riot.executor.threads:16}") int threads) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threads);
        executor.setMaxPoolSize(threads);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("riot-");
        // Under overload, slow the submitting thread down instead of dropping work.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }
}
