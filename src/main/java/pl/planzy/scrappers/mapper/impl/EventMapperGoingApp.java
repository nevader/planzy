package pl.planzy.scrappers.mapper.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pl.planzy.scrappers.mapper.EventMapper;

import java.util.ArrayList;
import java.util.List;

@Component("eventMapperGoingApp")
public class EventMapperGoingApp implements EventMapper {

    private static final Logger logger = LoggerFactory.getLogger(EventMapperGoingApp.class);
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public List<JsonNode> mapEvents(List<JsonNode> data) {

        logger.info("[{}] Starting to map events. Total events to map: [{}]", getClass().getSimpleName(), data.size());

        List<JsonNode> mappedEvents = new ArrayList<>();

        for (JsonNode event : data) {
            try {
                mappedEvents.add(mapGoingAppEvent(event));
            } catch (Exception e) {
                logger.error("[{}] Error mapping event: [{}]", getClass().getSimpleName(), event, e);
            }
        }

        logger.info("[{}] Finished mapping events. Total mapped events: [{}]", getClass().getSimpleName(), mappedEvents.size());

        return mappedEvents;

    }

    private JsonNode mapGoingAppEvent(JsonNode event) {

        String startDateTimestamp = event.has("start_date_timestamp") && !event.get("start_date_timestamp").isNull()
                ? event.get("start_date_timestamp").asText()
                : "null";

        String endDateTimestamp = event.has("end_date_timestamp") && !event.get("end_date_timestamp").isNull()
                ? event.get("end_date_timestamp").asText()
                : "null";

        String artistsNames = event.has("artists_names") && event.get("artists_names").isArray()
                ? String.join(", ", mapper.convertValue(event.get("artists_names"), List.class))
                : "Unknown Artist";

        String location = event.has("locations_names") && event.get("locations_names").isArray() && event.get("locations_names").size() > 0
                ? event.get("locations_names").get(0).asText()
                : "Unknown Location";

        String url = event.has("slug") && event.has("rundate_slug")
                ? "https://queue.goingapp.pl/wydarzenie/" + event.get("slug").asText() + "/" + event.get("rundate_slug").asText()
                : "Unknown URL";

        String tags = event.has("tags_names") && event.get("tags_names").isArray()
                ? String.join(", ", mapper.convertValue(event.get("tags_names"), List.class))
                : "";

        String thumbnail = event.has("thumbnail") && !event.get("thumbnail").isNull()
                ? "https://res.cloudinary.com/dr89d8ldb/image/upload/c_fill,h_350,w_405/f_webp/q_auto:eco/v1/rundate/" + event.get("thumbnail").asText().replace(" ", "%20")
                : "Unknown Thumbnail";

        return mapper.createObjectNode()
                .put("event_name", event.has("name_pl") && !event.get("name_pl").isNull() ? event.get("name_pl").asText() : "Unknown Event")
                .put("artists", artistsNames)
                .put("start_date", startDateTimestamp)
                .put("end_date", endDateTimestamp)
                .put("thumbnail", thumbnail)
                .put("url", url)
                .put("location", location)
                .put("place", event.has("place_name") && !event.get("place_name").isNull() ? event.get("place_name").asText() : "Unknown Place")
                .put("category", event.has("category_name") && !event.get("category_name").isNull() ? event.get("category_name").asText() : "Unknown Category")
                .put("tags", tags)
                .put("description", event.has("description_pl") && !event.get("description_pl").isNull() ? event.get("description_pl").asText() : "No Description")
                .put("source", "GoingApp");
    }
}