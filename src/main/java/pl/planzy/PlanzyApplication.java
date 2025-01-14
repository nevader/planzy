package pl.planzy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import pl.planzy.service.ScrapperService;

@SpringBootApplication
public class PlanzyApplication implements CommandLineRunner {

    @Autowired
    private ScrapperService scrapperService;

    public static void main(String[] args) {
        SpringApplication.run(PlanzyApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        scrapperService.scrapeAndMergeData();
    }
}
