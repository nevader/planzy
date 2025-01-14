package pl.planzy.config;

import com.microsoft.playwright.Playwright;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@org.springframework.context.annotation.Configuration

public class Configuration {

    @Bean
    public Playwright getPlaywright() {
        return Playwright.create();
    }

    @Bean(name = "customTaskExecutor")
    public TaskExecutor customTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("Scraper-");
        executor.initialize();
        return executor;
    }
}
