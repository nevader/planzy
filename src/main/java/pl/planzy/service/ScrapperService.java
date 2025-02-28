package pl.planzy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.web.mappings.MappingsEndpoint;
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

    public List<JsonNode> scrapeAndMergeData() {

        List<CompletableFuture<List<JsonNode>>> futures = new ArrayList<>();

        if (scrapers.isEmpty()) {
            logger.warn("[{}] No scrapers available for execution. Exiting scrapeAndMergeData", getClass().getSimpleName());
            return null;
        }

        logger.info("[{}] Starting scraping process with [{}] scrapers.", getClass().getSimpleName(), scrapers.size());

        for (Scrapper scraper : scrapers) {

            CompletableFuture<List<JsonNode>> future = CompletableFuture.supplyAsync(() -> {

                try {
                    logger.info("[{}] Starting scraper: [{}]", getClass().getSimpleName(), scraper.getClass().getSimpleName());
                    var scrapedData = scraper.scrapeData();
                    EventMapper mapper = scraper.getMapper();
                    var mappedData = mapper.mapEvents(scrapedData);
                    logger.info("[{}] Finished scraping with [{}]. Total events scraped: [{}]. Total events mapped: [{}]", getClass().getSimpleName(), scraper.getClass().getSimpleName(), scrapedData.size(), mappedData.size());
                    return mappedData;

                } catch (Exception e) {
                    logger.error("[{}] Error occurred while scraping with [{}]: [{}]", getClass().getSimpleName(), scraper.getClass().getSimpleName(), e.getMessage());
                    return new ArrayList<>();

                }
            }, taskExecutor);

            futures.add(future);
        }

        List<JsonNode> mergedResults = new ArrayList<>();
        try {
            mergedResults = CompletableFuture
                    .allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> futures.stream()
                            .map(CompletableFuture::join)
                            .flatMap(List::stream)
                            .toList()
                    )
                    .get();


        } catch (InterruptedException | ExecutionException e) {
            logger.error("[{}] An error occurred while waiting for scrapers to complete.", getClass().getSimpleName(), e);
        }

        logger.info("[{}] Scraping process completed.", getClass().getSimpleName());

        return mergedResults;
    }


    private void saveResultsToFile(List<JsonNode> mergedResults) {
        File outputFile = new File("scraped_data.json");
        try {
            objectMapper.writeValue(outputFile, mergedResults);
            logger.info("[{}] Scraped data saved to file: [{}]", getClass().getSimpleName(), outputFile.getAbsolutePath());
        } catch (IOException e) {
            logger.error("[{}] Failed to save scraped data to file.", getClass().getSimpleName(), e);
        }
    }
}
