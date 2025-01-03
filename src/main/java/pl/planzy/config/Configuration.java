package pl.planzy.config;

import com.microsoft.playwright.Playwright;
import org.springframework.context.annotation.Bean;

@org.springframework.context.annotation.Configuration

public class Configuration {

    @Bean
    public Playwright getPlaywright() {
        return Playwright.create();
    }
}
