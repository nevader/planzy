package pl.planzy.scrappers.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.microsoft.playwright.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import pl.planzy.scrappers.mapper.EventMapper;
import pl.planzy.scrappers.mapper.impl.EventMapperGoingApp;

import java.util.ArrayList;
import java.util.List;


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
        Object lock = new Object();
        List<String> pendingRequests = new ArrayList<>();

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
                        logger.error("[{}] Error processing JSON response from URL: {}", getClass().getSimpleName(), response.url(), e);
                    } finally {
                        synchronized (lock) {
                            pendingRequests.remove(response.url());
                            lock.notifyAll();
                        }
                    }
                }
            });

            String BASE_URL = "https://queue.goingapp.pl/szukaj";
            page.navigate(BASE_URL);
            page.waitForTimeout(5000);

            while (true) {
                try {
                    synchronized (lock) {
                        while (!pendingRequests.isEmpty()) {
                            lock.wait(1000);
                        }
                    }

                    String currentUrl = page.url();

                    if (currentUrl.contains("page=")) {
                        String[] urlParts = currentUrl.split("page=");
                        if (urlParts.length > 1) {
                            int currentPage = Integer.parseInt(urlParts[1].split("&")[0]); // Extract page number

                            if (currentPage % 5 == 0) {
                                logger.info("[{}] Pages scraped [{}/50] ...", getClass().getSimpleName(), currentPage);
                            }
                            if (currentPage >= 50) {
                                logger.info("[{}] Last page reached (page=[{}]), ending iteration.", getClass().getSimpleName(), currentPage);
                                break;
                            }
                        }
                    }

                    String LOAD_MORE_BUTTON_SELECTOR = ".ais-InfiniteHits-loadMore";
                    if (page.isVisible(LOAD_MORE_BUTTON_SELECTOR)) {
                        page.click(LOAD_MORE_BUTTON_SELECTOR);
                        page.waitForTimeout(2000);
                    }

                } catch (Exception e) {
                    logger.warn("[{}] Error during 'Load More' button interaction or URL processing: [{}]", getClass().getSimpleName(), e.getMessage());
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