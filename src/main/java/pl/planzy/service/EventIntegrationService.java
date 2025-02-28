package pl.planzy.service;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.planzy.entity.Event;
import pl.planzy.entity.Place;
import pl.planzy.repository.ArtistRepository;
import pl.planzy.repository.EventRepository;
import pl.planzy.repository.PlaceRepository;
import pl.planzy.repository.TagRepository;
import pl.planzy.scrappers.impl.ScrapperGoingApp;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
@Transactional

public class EventIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(EventIntegrationService.class);

    private final EventRepository eventRepository;
    private final ArtistRepository artistRepository;
    private final PlaceRepository placeRepository;
    private final TagRepository tagRepository;

    @Autowired
    public EventIntegrationService(EventRepository eventRepository, ArtistRepository artistRepository, PlaceRepository placeRepository, TagRepository tagRepository) {
        this.eventRepository = eventRepository;
        this.artistRepository = artistRepository;
        this.placeRepository = placeRepository;
        this.tagRepository = tagRepository;
    }


    public void processScrapedEvents(List<JsonNode> events) {

        logger.info("Processing [{}] events ...", events.size());

        for (JsonNode event : events) {
            processEvents(event);
        }
    }

    public void processEvents(JsonNode event) {

        String eventUrl = event.get("event_url").asText();

        Optional<Event> eventOptional = eventRepository.findByUrl(eventUrl);

        if (eventOptional.isPresent()) {
            return;
        }

        Event newEvent = new Event();
        newEvent.setEvent_name(event.get("event_name").asText());
        newEvent.setStart_date(parseTimestamp(event.get("start_date").asText()));
        newEvent.setEnd_date(parseTimestamp(event.get("end_date").asText()));
        newEvent.setThumbnail(event.get("thumbnail").asText());
        newEvent.setUrl(eventUrl);
        newEvent.setLocation(event.get("location").asText());
        newEvent.setCategory(event.get("category").asText());
        newEvent.setDescription(event.get("description").asText());
        newEvent.setSource(event.get("source").asText());



    }

    private void processArtist(Event event, JsonNode eventNode) {

        String artists = eventNode.get("artists").asText();

        if (artists == null || artists.isEmpty()) {
            artists = "Unknown Artist";
        }
    }

    private LocalDateTime parseTimestamp(String timestamp) {
        if (timestamp == null || timestamp.equals("null")) {
            return null;
        }

        try {
            long epochSeconds = Long.parseLong(timestamp);
            return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneId.systemDefault());
        } catch (NumberFormatException e) {
            logger.warn("Invalid timestamp format: {}", timestamp);
            return null;
        }
    }

}

