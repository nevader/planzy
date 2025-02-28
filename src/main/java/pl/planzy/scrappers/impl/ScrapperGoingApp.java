package pl.planzy.scrappers.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import pl.planzy.scrappers.mapper.EventMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@Component("scrapperGoingApp")
public class ScrapperGoingApp implements Scrapper {

    private static final Logger logger = LoggerFactory.getLogger(ScrapperGoingApp.class);
    private final ObjectMapper mapper;
    private final EventMapper eventMapper;

    @Autowired
    public ScrapperGoingApp(ObjectMapper mapper, @Qualifier("eventMapperGoingApp") EventMapper eventMapper) {
        this.mapper = mapper;
        this.eventMapper = eventMapper;
    }

    @Override
    public List<JsonNode> scrapeData() {
        List<JsonNode> scrapedData = new ArrayList<>();
        List<String> pendingRequests = new ArrayList<>();

        Object lock = new Object();
        CountDownLatch latch = new CountDownLatch(1);

        logger.info("[{}] Started fetching data ...", getClass().getSimpleName());

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
            Page page = browser.newPage();

            // Configure request tracking
            page.onRequest(request -> {
                if (request.url().contains("algolia.net/1/indexes/")) {
                    synchronized (lock) {
                        pendingRequests.add(request.url());
                    }
                }
            });

            CountDownLatch finalLatch = latch;

            // Configure response handling
            page.onResponse(response -> {
                if (response.url().contains("algolia.net/1/indexes/") && response.status() == 200) {
                    try {
                        if (response.headers().getOrDefault("content-type", "").contains("application/json")) {
                            String responseBody = response.text();
                            JsonNode jsonNode = mapper.readTree(responseBody);
                            JsonNode hits = jsonNode.path("results").path(0).path("hits");

                            if (hits.isArray()) {
                                for (JsonNode hit : hits) {
                                    scrapedData.add(hit);
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.error("[{}] Error processing JSON response from URL: [{}]", getClass().getSimpleName(), response.url(), e);
                    } finally {
                        synchronized (lock) {
                            pendingRequests.remove(response.url());
                            if (pendingRequests.isEmpty()) {
                                finalLatch.countDown();
                            }
                        }
                    }
                }
            });

            // Navigate to initial page
            String BASE_URL = "https://goingapp.pl/szukaj?refinementList%5Btype%5D%5B0%5D=rundate&refinementList%5Btype%5D%5B1%5D=activity";
            page.navigate(BASE_URL);

            // Wait for initial page load
            page.waitForTimeout(10000);

            // Get total records count
            String RECORDS_COUNT_SELECTOR = "#root > main > div.MuiBox-root.css-1kyexf6 > h6";
            String recordsText = page.textContent(RECORDS_COUNT_SELECTOR).replaceAll("\\D", "");
            int totalRecords = Integer.parseInt(recordsText);
            logger.info("[{}] Total records to scrape: [{}]", getClass().getSimpleName(), totalRecords);

            boolean hasMoreContent = true;
            int maxAttempts = 3; // Maximum number of attempts to click when errors occur

            while (hasMoreContent && scrapedData.size() < 100) {
                try {
                    synchronized (lock) {
                        if (!pendingRequests.isEmpty()) {
                            latch.await();
                        }
                    }

                    String LOAD_MORE_BUTTON_SELECTOR = ".ais-InfiniteHits-loadMore";

                    // First check if the button exists at all
                    ElementHandle loadMoreButton = page.querySelector(LOAD_MORE_BUTTON_SELECTOR);
                    if (loadMoreButton == null) {
                        logger.info("[{}] No 'Load More' button found, assuming all content is loaded.", getClass().getSimpleName());
                        hasMoreContent = false;
                        continue;
                    }

                    // Then check if the button is disabled
                    boolean isDisabled = loadMoreButton.isDisabled();
                    if (isDisabled) {
                        logger.info("[{}] 'Load More' button is disabled, assuming all content is loaded.", getClass().getSimpleName());
                        hasMoreContent = false;
                        continue;
                    }

                    // If we get here, the button exists and is not disabled
                    latch = new CountDownLatch(1);

                    // Scroll to the button to ensure it's in view
                    loadMoreButton.scrollIntoViewIfNeeded();

                    // Click with retry logic
                    boolean clickSuccess = false;
                    for (int attempt = 0; attempt < maxAttempts; attempt++) {
                        try {
                            loadMoreButton.click(new ElementHandle.ClickOptions().setTimeout(5000));
                            clickSuccess = true;
                            break;
                        } catch (TimeoutError e) {
                            // Check if the button became disabled
                            if (loadMoreButton.isDisabled()) {
                                logger.info("[{}] 'Load More' button became disabled during click attempt.", getClass().getSimpleName());
                                hasMoreContent = false;
                                break;
                            }

                            if (attempt == maxAttempts - 1) {
                                throw e; // Re-throw on last attempt
                            }

                            logger.info("[{}] Click attempt {} failed, retrying...", getClass().getSimpleName(), attempt + 1);
                            page.waitForTimeout(2000); // Wait a bit before retrying
                        }
                    }

                    if (!clickSuccess && hasMoreContent) {
                        logger.warn("[{}] Failed to click 'Load More' button after {} attempts. Moving on.",
                                getClass().getSimpleName(), maxAttempts);
                        hasMoreContent = false;
                        continue;
                    }

                    // Wait for content to load
                    page.waitForTimeout(4000);

                    logger.info("[{}] Progress: [{}/{}] records scraped ...",
                            getClass().getSimpleName(), scrapedData.size(), totalRecords);

                } catch (Exception e) {
                    if (e instanceof TimeoutError) {
                        // This is likely just the end of content
                        logger.info("[{}] Timeout reached, assuming all content has been loaded.", getClass().getSimpleName());
                        hasMoreContent = false;
                    } else {
                        // Log other errors but continue with the data we have
                        logger.warn("[{}] Error during scraping: {}", getClass().getSimpleName(), e.getMessage());
                        // Attempt to continue if we have some data
                        if (scrapedData.size() > 0) {
                            logger.info("[{}] Continuing with {} records already scraped.",
                                    getClass().getSimpleName(), scrapedData.size());
                            hasMoreContent = false;
                        } else {
                            // If we have no data, this is a fatal error
                            throw new RuntimeException("Failed to scrape any data", e);
                        }
                    }
                }
            }

            browser.close();

        } catch (Exception e) {
            logger.error("[{}] An error occurred while scraping data", getClass().getSimpleName(), e);
            // If we've collected some data, we'll return it despite the error
            if (scrapedData.isEmpty()) {
                throw new RuntimeException("Failed to scrape any data from GoingApp", e);
            }
        }

        logger.info("[{}] Finished fetching. Total events fetched: [{}]", getClass().getSimpleName(), scrapedData.size());

        return scrapedData;
    }

    @Override
    public EventMapper getMapper() {
        return eventMapper;
    }
}