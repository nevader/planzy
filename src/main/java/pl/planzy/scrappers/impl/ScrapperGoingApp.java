package pl.planzy.scrappers.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.microsoft.playwright.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.planzy.scrappers.Scrapper;

import java.io.File;
import java.io.IOException;

public class ScrapperGoingApp implements Scrapper {

    private static final Logger logger = LoggerFactory.getLogger(ScrapperGoingApp.class);
    private static final String BASE_URL = "https://queue.goingapp.pl/szukaj";
    private static final String LOAD_MORE_BUTTON_SELECTOR = ".ais-InfiniteHits-loadMore";
    private static final String OUTPUT_FILE = "captured_data.json";
    private final ObjectMapper mapper = new ObjectMapper();
    private final ArrayNode aggregatedResults = mapper.createArrayNode();

    @Override
    public void scrapeData() {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();

            page.onResponse(response -> {
                if (response.url().contains("algolia.net/1/indexes/") && response.status() == 200) {
                    try {
                        if (response.headers().getOrDefault("content-type", "").contains("application/json")) {
                            String responseBody = response.text();
                            JsonNode jsonNode = mapper.readTree(responseBody);
                            JsonNode hits = jsonNode.path("results").path(0).path("hits");

                            if (hits.isArray()) {
                                aggregatedResults.addAll((ArrayNode) hits);
                                logger.info("Appended {} results", hits.size());
                            }
                        }
                    } catch (Exception e) {
                        logger.error("Error processing JSON response from URL: {}", response.url(), e);
                    }
                }
            });

            // Navigate to the base URL
            logger.info("Navigating to URL: {}", BASE_URL);
            page.navigate(BASE_URL);
            page.waitForTimeout(5000);

            // Iteratively click the "Load More" button and process new data
            while (true) {
                try {
                    if (page.isVisible(LOAD_MORE_BUTTON_SELECTOR)) {
                        logger.info("Clicking 'Load More' button...");
                        page.click(LOAD_MORE_BUTTON_SELECTOR);
                        page.waitForTimeout(2000); // Wait for new content to load
                    } else {
                        logger.info("No more 'Load More' button found, ending iteration.");
                        break;
                    }
                } catch (Exception e) {
                    logger.warn("Error during 'Load More' button interaction or no button found: {}", e.getMessage());
                    break;
                }
            }

            browser.close();

            // Save all captured data to a JSON file
            saveDataToFile(aggregatedResults, OUTPUT_FILE);

        } catch (Exception e) {
            logger.error("An error occurred while scraping data", e);
        }
    }

    private void saveDataToFile(ArrayNode data, String fileName) {
        try {
            ObjectNode root = mapper.createObjectNode();
            root.set("results", data);
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(fileName), root);
            logger.info("Data successfully saved to {}", fileName);
        } catch (IOException e) {
            logger.error("Failed to save data to file: {}", fileName, e);
        }
    }
}
