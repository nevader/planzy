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

        List<JsonNode> resultsList = new ArrayList<>();

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
                                for (JsonNode hit : hits) {
                                    resultsList.add(hit);
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.error("Error processing JSON response from URL: {}", response.url(), e);
                    }
                }
            });

            // Navigate to the base URL
            String BASE_URL = "https://queue.goingapp.pl/szukaj";
            logger.info("Navigating to URL: {}", BASE_URL);
            page.navigate(BASE_URL);
            page.waitForTimeout(5000);

            // Iteratively click the "Load More" button and process new data
            while (true) {
                try {
                    String LOAD_MORE_BUTTON_SELECTOR = ".ais-InfiniteHits-loadMore";
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


        } catch (Exception e) {
            logger.error("An error occurred while scraping data", e);
        }

        return resultsList;
    }


    @Override
    public EventMapper getMapper() {
        return eventMapper;
    }

}
