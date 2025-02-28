package pl.planzy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import pl.planzy.service.EventIntegrationService;
import pl.planzy.service.ScrapperService;

@SpringBootApplication
public class PlanzyApplication implements CommandLineRunner {

    private final ScrapperService scrapperService;
    private EventIntegrationService eventIntegrationService;

    @Autowired
    public PlanzyApplication(ScrapperService scrapperService, EventIntegrationService eventIntegrationService) {
        this.scrapperService = scrapperService;
        this.eventIntegrationService = eventIntegrationService;
    }

    public static void main(String[] args) {
        SpringApplication.run(PlanzyApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        eventIntegrationService.processScrapedEvents(scrapperService.scrapeAndMergeData());
    }
}
