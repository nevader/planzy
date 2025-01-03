package pl.planzy.scrappers.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.planzy.scrappers.Scrapper;

import java.io.File;
import java.io.IOException;

public class ScrapperGoingApp implements Scrapper {

    private static final Logger logger = LoggerFactory.getLogger(ScrapperGoingApp.class);
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void scrapeData() {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
            Page page = browser.newPage();

            // Add a response listener to capture the desired XHR response
            page.onResponse(response -> {
                String url = response.url();
                if (url.contains("algolia.net/1/indexes/*/queries")) {
                    logger.info("Captured URL: {}", url);
                    try {
                        if (response.status() == 200 && response.headers().getOrDefault("content-type", "").contains("application/json")) {
                            String responseBody = response.text();
                            logger.info("Captured Response Body: {}", responseBody);

                            // Parse JSON and save it to a file
                            JsonNode jsonNode = mapper.readTree(responseBody);
                            saveDataToFile(jsonNode, "algolia_response.json");
                        } else {
                            logger.warn("Response not JSON or unsuccessful: {}", url);
                        }
                    } catch (Exception e) {
                        logger.error("Error processing response from URL: {}", url, e);
                    }
                }
            });

            // Navigate to the main page to trigger the desired XHR request
            String mainUrl = "https://goingapp.pl";
            logger.info("Navigating to URL: {}", mainUrl);
            page.navigate(mainUrl);
            page.waitForLoadState(LoadState.NETWORKIDLE);

            browser.close();
        } catch (Exception e) {
            logger.error("An error occurred while scraping data", e);
        }
    }

    private void saveDataToFile(JsonNode data, String fileName) {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(fileName), data);
            logger.info("Data successfully saved to {}", fileName);
        } catch (IOException e) {
            logger.error("Failed to save data to file: {}", fileName, e);
        }
    }
}
