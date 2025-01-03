package pl.planzy.scrappers.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.planzy.scrappers.Scrapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ScrapperGoingApp implements Scrapper {

    private static final Logger logger = LoggerFactory.getLogger(ScrapperGoingApp.class);
    private static final String BASE_URL = "https://queue.goingapp.pl/szukaj?page=";
    private static final int TOTAL_PAGES = 4; // Adjust as needed

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void scrapeData() {
        List<JsonNode> allData = new ArrayList<>();

        try (Playwright playwright = Playwright.create()) {
            Browser browser = createBrowser(playwright);
            Page page = browser.newPage();

            for (int i = 1; i <= TOTAL_PAGES; i++) {
                processPage(page, allData, i);
            }

            browser.close();
            saveDataToFile(allData, "captured_data.json");
        } catch (Exception e) {
            logger.error("An error occurred while scraping data", e);
        }
    }

    private Browser createBrowser(Playwright playwright) {
        return playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
    }

    private void processPage(Page page, List<JsonNode> allData, int pageIndex) {
        String url = BASE_URL + pageIndex;
        logger.info("Navigating to URL: {}", url);

        List<JsonNode> pageData = scrapePage(page, url);
        allData.addAll(pageData);
    }

    private List<JsonNode> scrapePage(Page page, String url) {
        List<JsonNode> pageData = new ArrayList<>();

        page.onResponse(response -> handleResponse(response, pageData));
        page.navigate(url);
        page.waitForLoadState(LoadState.NETWORKIDLE);

        return pageData;
    }

    private void handleResponse(Response response, List<JsonNode> pageData) {
        try {
            logger.info("Processing URL: {}", response.url());

            if (isRelevantResponse(response)) {
                processJsonResponse(response, pageData);
            } else {
                logger.warn("Non-JSON response from URL: {}", response.url());
            }
        } catch (Exception e) {
            logger.error("Error processing response from URL: {}", response.url(), e);
        }
    }

    private boolean isRelevantResponse(Response response) {
        return response.url().contains("szukaj?page=") && response.status() == 200 && isJsonResponse(response);
    }

    private void processJsonResponse(Response response, List<JsonNode> pageData) throws IOException {
        String responseBody = response.text();
        logger.info("Captured Response Body: {}", responseBody);
        JsonNode jsonNode = mapper.readTree(responseBody);
        pageData.add(jsonNode);
    }

    private boolean isJsonResponse(Response response) {
        String contentType = response.headers().getOrDefault("content-type", "").toLowerCase();
        return contentType.contains("application/json");
    }

    private void saveDataToFile(List<JsonNode> data, String fileName) {
        try {
            ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
            writer.writeValue(new File(fileName), data);
            logger.info("Data successfully saved to {}", fileName);
        } catch (IOException e) {
            logger.error("Failed to save data to file: {}", fileName, e);
        }
    }
}
