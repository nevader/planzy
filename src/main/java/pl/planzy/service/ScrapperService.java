package pl.planzy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import pl.planzy.scrappers.impl.Scrapper;
import pl.planzy.scrappers.mapper.EventMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class ScrapperService {

    private static final Logger logger = LoggerFactory.getLogger(ScrapperService.class);

    private final ObjectMapper objectMapper;
    private final TaskExecutor taskExecutor;
    private final List<Scrapper> scrapers;

    public ScrapperService(ObjectMapper objectMapper, @Qualifier("customTaskExecutor") TaskExecutor taskExecutor, List<Scrapper> scrapers) {
        this.objectMapper = objectMapper;
        this.taskExecutor = taskExecutor;
        this.scrapers = scrapers;
    }

    public void scrapeAndMergeData() {
        List<CompletableFuture<List<JsonNode>>> futures = new ArrayList<>();

        for (Scrapper scraper : scrapers) {
            CompletableFuture<List<JsonNode>> future = CompletableFuture.supplyAsync(() -> {
                try {
                    scraper.scrapeData();
                    return scraper.getResults();
                } catch (Exception e) {
                    logger.error("Error during scraping with scraper: {}", scraper.getClass().getSimpleName(), e);
                    return new ArrayList<>();
                }
            }, taskExecutor);
            futures.add(future);
        }

        List<JsonNode> allData = new ArrayList<>();
        for (CompletableFuture<List<JsonNode>> future : futures) {
            try {
                allData.addAll(future.get());
            } catch (InterruptedException | ExecutionException e) {
                logger.error("Error collecting scraping results", e);
            }
        }

        List<JsonNode> mappedData = new ArrayList<>();
        for (Scrapper scraper : scrapers) {
            EventMapper mapper = scraper.getMapper();
            List<JsonNode> scraperData = scraper.getResults();
            try {
                mappedData.addAll(mapper.mapEvents(scraperData));
            } catch (Exception e) {
                logger.error("Error mapping data for scraper: {}", scraper.getClass().getSimpleName(), e);
            }
        }

        saveToFile(mappedData, "merged_data.json");
    }

    private void saveToFile(List<JsonNode> data, String fileName) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(fileName), data);
            logger.info("Data successfully saved to {}", fileName);
        } catch (IOException e) {
            logger.error("Error saving merged data to file", e);
        }
    }
}
