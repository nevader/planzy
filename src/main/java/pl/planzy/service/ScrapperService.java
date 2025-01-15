package pl.planzy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import pl.planzy.scrappers.impl.Scrapper;

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

    /**
     * Constructor for ScrapperService
     *
     * @param objectMapper ObjectMapper for JSON handling
     * @param taskExecutor Custom TaskExecutor for running scrapers concurrently
     * @param scrapers     List of scrapers available in the system
     */
    public ScrapperService(ObjectMapper objectMapper, @Qualifier("customTaskExecutor") TaskExecutor taskExecutor, List<Scrapper> scrapers) {
        this.objectMapper = objectMapper;
        this.taskExecutor = taskExecutor;
        this.scrapers = scrapers;
    }

    /**
     * Main method to scrape and merge data from all scrapers concurrently.
     */
    public void scrapeAndMergeData() {
        if (scrapers.isEmpty()) {
            logger.warn("No scrapers available for execution. Exiting scrapeAndMergeData.");
            return;
        }

        logger.info("Starting scraping process with {} scrapers.", scrapers.size());

        // List to hold all CompletableFuture tasks
        List<CompletableFuture<List<JsonNode>>> futures = new ArrayList<>();

        // Submit tasks for each scraper
        for (Scrapper scraper : scrapers) {
            CompletableFuture<List<JsonNode>> future = CompletableFuture.supplyAsync(() -> {
                try {
                    logger.info("Starting scraper: {}", scraper.getClass().getSimpleName());
                    return scraper.scrapeData(); // Directly fetch results from scrapeData
                } catch (Exception e) {
                    logger.error("Error occurred while scraping with {}: {}", scraper.getClass().getSimpleName(), e.getMessage());
                    return new ArrayList<>(); // Return an empty list on failure
                }
            }, taskExecutor);

            futures.add(future);
        }

        // Wait for all scrapers to complete
        try {
            List<JsonNode> mergedResults = CompletableFuture
                    .allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> futures.stream()
                            .map(CompletableFuture::join)
                            .flatMap(List::stream) // Flatten the results from all scrapers
                            .toList()
                    )
                    .get(); // Wait for the completion of all tasks

            // Save merged results to a file
            saveResultsToFile(mergedResults);

        } catch (InterruptedException | ExecutionException e) {
            logger.error("An error occurred while waiting for scrapers to complete.", e);
        }

        logger.info("Scraping process completed.");
    }

    /**
     * Save the merged results to a file.
     *
     * @param mergedResults List of JSON results to save
     */
    private void saveResultsToFile(List<JsonNode> mergedResults) {
        File outputFile = new File("scraped_data.json");
        try {
            objectMapper.writeValue(outputFile, mergedResults);
            logger.info("Scraped data saved to file: {}", outputFile.getAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to save scraped data to file.", e);
        }
    }
}
