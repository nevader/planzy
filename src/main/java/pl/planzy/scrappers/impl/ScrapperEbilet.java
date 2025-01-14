package pl.planzy.scrappers.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import pl.planzy.scrappers.mapper.EventMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

@Component("scrapperEbilet")
public class ScrapperEbilet implements Scrapper {

    private final String baseUrl = "https://www.ebilet.pl/api/TitleListing/Search";
    private final List<JsonNode> allData = new ArrayList<>();

    private static final Logger logger = LoggerFactory.getLogger(ScrapperEbilet.class);

    private final EventMapper eventMapper;

    @Autowired
    public ScrapperEbilet(@Qualifier("eventMapperEbilet") EventMapper eventMapper) {
        this.eventMapper = eventMapper;
    }

    @Override
    public void scrapeData() {
        int size = 20;
        int top = 0;
        boolean hasNext = true;


        try {
            HttpClient client = HttpClient.newHttpClient();
            ObjectMapper mapper = new ObjectMapper();

            while (hasNext) {
                String url = String.format("%s?currentTab=2&sort=1&top=%d&size=%d", baseUrl, top, size);
                logger.info("Fetching data from URL: {}", url);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonNode jsonNode = mapper.readTree(response.body());
                    JsonNode data = jsonNode.get("titles");

                    if (data != null && !data.isEmpty()) {
                        data.forEach(allData::add);
                        top += size;
                    } else {
                        hasNext = false;
                    }
                } else {
                    logger.error("Failed to fetch data. HTTP status code: {}", response.statusCode());
                    hasNext = false;
                }
            }

            eventMapper.mapEvents(allData);

        } catch (Exception e) {
            logger.error("An error occurred while scraping data", e);
        }
    }

    @Override
    public List<JsonNode> getResults() {
        return allData;
    }

    @Override
    public EventMapper getMapper() {
        return eventMapper;
    }
}