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

        List<JsonNode> scrappedData = new ArrayList<>();
        List<String> pendingRequests = new ArrayList<>();

        Object lock = new Object();
        CountDownLatch latch = new CountDownLatch(1);

        logger.info("[{}] Started fetching data ...", getClass().getSimpleName());

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
            Page page = browser.newPage();

            page.onRequest(request -> {
                if (request.url().contains("algolia.net/1/indexes/")) {
                    synchronized (lock) {
                        pendingRequests.add(request.url());
                    }
                }
            });

            CountDownLatch finalLatch = latch;

            page.onResponse(response -> {
                if (response.url().contains("algolia.net/1/indexes/") && response.status() == 200) {
                    try {
                        if (response.headers().getOrDefault("content-type", "").contains("application/json")) {
                            String responseBody = response.text();
                            JsonNode jsonNode = mapper.readTree(responseBody);
                            JsonNode hits = jsonNode.path("results").path(0).path("hits");

                            if (hits.isArray()) {
                                for (JsonNode hit : hits) {
                                    scrappedData.add(hit);
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

            String BASE_URL = "https://goingapp.pl/szukaj?refinementList%5Btype%5D%5B0%5D=rundate&refinementList%5Btype%5D%5B1%5D=activity";
            page.navigate(BASE_URL);
            page.waitForTimeout(10000);

            String RECORDS_COUNT_SELECTOR = "#root > main > div.MuiBox-root.css-1kyexf6 > h6";
            String recordsText = page.textContent(RECORDS_COUNT_SELECTOR).replaceAll("\\D", "");
            int totalRecords = Integer.parseInt(recordsText);
            logger.info("[{}] Total records to scrape: [{}]", getClass().getSimpleName(), totalRecords);

            while (scrappedData.size() < totalRecords) {
                try {
                    synchronized (lock) {
                        if (!pendingRequests.isEmpty()) {
                            latch.await();
                        }
                    }

                    String LOAD_MORE_BUTTON_SELECTOR = ".ais-InfiniteHits-loadMore";
                    if (page.isVisible(LOAD_MORE_BUTTON_SELECTOR)) {
                        latch = new CountDownLatch(1);
                        page.click(LOAD_MORE_BUTTON_SELECTOR);
                        page.waitForTimeout(4000);
                    } else {
                        logger.info("[{}] No more 'Load More' button found, or all records loaded.", getClass().getSimpleName());
                        break;
                    }

                    logger.info("[{}] Progress: [{}/{}] records scraped ...", getClass().getSimpleName(), scrappedData.size(), totalRecords);

                } catch (Exception e) {
                    logger.warn("[{}] Error during 'Load More' button interaction or scraping: [{}]", getClass().getSimpleName(), e.getMessage());
                    break;
                }
            }

            browser.close();

        } catch (Exception e) {
            logger.error("[{}] An error occurred while scraping data", getClass().getSimpleName(), e);
        }

        logger.info("[{}] Finished fetching. Total events fetched: [{}]", getClass().getSimpleName(), scrappedData.size());

        return scrappedData;
    }

    @Override
    public EventMapper getMapper() {
        return eventMapper;
    }
}
