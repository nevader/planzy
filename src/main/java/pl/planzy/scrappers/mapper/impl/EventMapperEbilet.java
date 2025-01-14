package pl.planzy.scrappers.mapper.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pl.planzy.scrappers.mapper.EventMapper;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


@Component("eventMapperEbilet")
public class EventMapperEbilet implements EventMapper {

    private static final Logger logger = LoggerFactory.getLogger(EventMapperEbilet.class);
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public List<JsonNode> mapEvents(List<JsonNode> data) {
        logger.info("Starting to map events. Total events to map: {}", data.size());

        List<JsonNode> mappedEvents = new ArrayList<>();

        for (JsonNode event : data) {
            try {
                mappedEvents.add(mapEbiletEvent(event));
            } catch (Exception e) {
                logger.error("Error mapping event: {}", event, e);
            }
        }

        try {
            mapper.writeValue(new File("mapped_events.json"), mappedEvents);
            logger.info("Mapped events saved to mapped_events.json");
        } catch (IOException e) {
            logger.error("Error writing mapped events to file", e);
        }

        logger.info("Finished mapping events. Total mapped events: {}", mappedEvents.size());

        return mappedEvents;
    }

    private JsonNode mapEbiletEvent(JsonNode event) {

        String startDateTimestamp = "null";
        String endDateTimeStamp = "null";

        if (event.has("dateFrom") && !event.get("dateFrom").isNull() && !event.get("dateFrom").asText().isEmpty() && !event.get("dateFrom").asText().equalsIgnoreCase("null")) {
            try {
                startDateTimestamp = convertToTimestamp(event.get("dateFrom").asText());
            } catch (DateTimeParseException e) {
                logger.warn("Invalid date format for dateFrom: {}", event.get("dateFrom").asText(), e);
            }
        } else {
            logger.info("dateFrom is null or empty for event: {}", event);
        }

        if (event.has("dateTo") && !event.get("dateTo").isNull() && !event.get("dateTo").asText().isEmpty() && !event.get("dateTo").asText().equalsIgnoreCase("null")) {
            try {
                endDateTimeStamp = convertToTimestamp(event.get("dateTo").asText());
            } catch (DateTimeParseException e) {
                logger.warn("Invalid date format for dateTo: {}", event.get("dateTo").asText(), e);
            }
        } else {
            logger.info("dateTo is null or empty for event: {}", event);
        }

        String artistsNames = event.has("artists") && event.get("artists").isArray()
                ? String.join(", ", mapper.convertValue(event.get("artists"), List.class))
                : "Unknown Artist";

        String customName = event.has("nextEventPlace") && event.get("nextEventPlace").has("customName")
                ? event.get("nextEventPlace").get("customName").asText()
                : "Unknown Place";

        String location = event.has("nextEventPlace") && event.get("nextEventPlace").has("city")
                ? event.get("nextEventPlace").get("city").asText()
                : "Unknown City";

        String url = "";

        if (event.has("linkTo") && event.get("linkTo").asText().equalsIgnoreCase("null")) {
            String category = event.get("category").asText();
            String subcategory = event.get("subcategory").asText().replace("\"", "");
            String slug = event.get("slug").asText();
            url = "https://www.ebilet.pl/" + category + "/" + subcategory + "/" + slug;
        } else {
            url = event.get("linkTo").asText();
        }

        StringBuilder cat = new StringBuilder();

        if (event.has("subcategoryName") && !event.get("subcategoryName").isNull()
                && !event.get("subcategoryName").asText().isBlank()
                && !event.get("subcategoryName").asText().equalsIgnoreCase("null")) {

            cat.append(event.get("subcategoryName").asText()).append(", ");
        }

        if (event.has("category") && !event.get("category").isNull()
                && !event.get("category").asText().isBlank()
                && !event.get("category").asText().equalsIgnoreCase("null")) {

            cat.append(event.get("category").asText()).append(", ");
        }

        if (event.has("subcategory") && !event.get("category").isNull()
                && !event.get("subcategory").asText().isBlank()
                && !event.get("subcategory").asText().equalsIgnoreCase("null")) {

            cat.append(event.get("subcategory").asText());
        }

        return mapper.createObjectNode()
                .put("event_name", event.get("title").asText())
                .put("artists", artistsNames)
                .put("start_date", startDateTimestamp)
                .put("end_date", endDateTimeStamp)
                .put("thumbnail", "https://www.ebilet.pl/media" + event.get("imageLandscape").asText())
                .put("url", url)
                .put("location", location)
                .put("place", customName)
                .put("category", event.get("categoryName").asText())
                .put("tags", cat.toString())
                .put("description", event.get("metaDescription").asText())
                .put("source", "eBilet");
    }

    private String convertToTimestamp(String dateString) {
        try {
            LocalDateTime localDateTime = LocalDateTime.parse(dateString);
            return String.valueOf(localDateTime.toEpochSecond(ZoneOffset.UTC));
        } catch (Exception e) {
            logger.error("Error parsing date: {}", dateString, e);
            throw new RuntimeException("Error parsing date: " + dateString, e);
        }
    }
}