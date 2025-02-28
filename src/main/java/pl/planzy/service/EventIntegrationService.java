package pl.planzy.service;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pl.planzy.entity.Artist;
import pl.planzy.entity.Event;
import pl.planzy.entity.Place;
import pl.planzy.entity.Tag;
import pl.planzy.repository.ArtistRepository;
import pl.planzy.repository.EventRepository;
import pl.planzy.repository.PlaceRepository;
import pl.planzy.repository.TagRepository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class EventIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(EventIntegrationService.class);
    private static final int BATCH_SIZE = 20;
    private static final int FLUSH_THRESHOLD = 50;

    @PersistenceContext
    private EntityManager entityManager;

    private final EventRepository eventRepository;
    private final ArtistRepository artistRepository;
    private final PlaceRepository placeRepository;
    private final TagRepository tagRepository;
    private final JdbcTemplate jdbcTemplate;

    // Cache maps to avoid repetitive database lookups
    private final Map<String, Place> placeCache = new ConcurrentHashMap<>();
    private final Map<String, Artist> artistCache = new ConcurrentHashMap<>();
    private final Map<String, Tag> tagCache = new ConcurrentHashMap<>();
    private final Set<String> processedUrls = new HashSet<>();

    @Autowired
    public EventIntegrationService(EventRepository eventRepository,
                                   ArtistRepository artistRepository,
                                   PlaceRepository placeRepository,
                                   TagRepository tagRepository,
                                   JdbcTemplate jdbcTemplate) {
        this.eventRepository = eventRepository;
        this.artistRepository = artistRepository;
        this.placeRepository = placeRepository;
        this.tagRepository = tagRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Pre-loads frequently accessed data into cache to minimize database queries.
     */
    private void preloadCaches() {
        logger.info("Preloading entity caches to improve performance...");

        // Load all places into cache
        placeRepository.findAll().forEach(place -> placeCache.put(place.getName(), place));
        logger.info("Loaded {} places into cache", placeCache.size());

        // Load all artists into cache
        artistRepository.findAll().forEach(artist -> artistCache.put(artist.getName(), artist));
        logger.info("Loaded {} artists into cache", artistCache.size());

        // Load all tags into cache
        tagRepository.findAll().forEach(tag -> tagCache.put(tag.getName(), tag));
        logger.info("Loaded {} tags into cache", tagCache.size());

        // Load all event URLs to avoid duplicates
        jdbcTemplate.query("SELECT url FROM events", rs -> {
            processedUrls.add(rs.getString(1));
        });
        logger.info("Loaded {} event URLs for duplicate prevention", processedUrls.size());
    }

    /**
     * Main method to process scraped events with optimized database access.
     */
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void processScrapedEvents(List<JsonNode> events) {
        logger.info("Saving scraped data to database - [{}] events ...", events.size());

        // Preload caches for better performance
        preloadCaches();

        int totalProcessed = 0;
        int successCount = 0;
        int skipCount = 0;
        int errorCount = 0;
        int flushCounter = 0;

        // Process in batches for better performance
        for (int i = 0; i < events.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, events.size());
            List<JsonNode> batch = events.subList(i, endIndex);

            logger.info("Processing batch {}-{} of {} events", i, endIndex-1, events.size());

            // Process each event in the batch
            for (JsonNode eventNode : batch) {
                try {
                    String url = getEventUrl(eventNode);

                    // Skip already processed events (using cache)
                    if (url == null || processedUrls.contains(url)) {
                        skipCount++;
                        continue;
                    }

                    // Process the event
                    Event savedEvent = processEvent(eventNode);
                    if (savedEvent != null) {
                        processedUrls.add(url);
                        successCount++;
                        flushCounter++;
                    } else {
                        skipCount++;
                    }

                    // Periodically flush and clear to prevent memory issues
                    if (flushCounter >= FLUSH_THRESHOLD) {
                        entityManager.flush();
                        entityManager.clear();
                        flushCounter = 0;
                    }
                } catch (Exception e) {
                    logger.error("Error processing event: {}", e.getMessage());
                    errorCount++;
                }

                totalProcessed++;
                if (totalProcessed % 50 == 0) {
                    logger.info("Progress: {}/{} events processed. Success: {}, Skipped: {}, Errors: {}",
                            totalProcessed, events.size(), successCount, skipCount, errorCount);
                }
            }

            // Flush changes after each batch
            entityManager.flush();
            entityManager.clear();
        }

        logger.info("Finished processing events. Total: {}, Success: {}, Skipped: {}, Errors: {}",
                events.size(), successCount, skipCount, errorCount);
    }

    /**
     * Processes a single event with optimized entity creation.
     */
    private Event processEvent(JsonNode eventNode) {
        String eventUrl = getEventUrl(eventNode);
        if (eventUrl == null) {
            logger.warn("Skipping event without URL");
            return null;
        }

        // Check for existing URL using our cache first
        if (processedUrls.contains(eventUrl)) {
            logger.debug("Event with URL {} already exists, skipping", eventUrl);
            return null;
        }

        // Create new event with necessary data
        Event newEvent = createEventFromNode(eventNode);

        // Save the event first to generate ID
        Event savedEvent = eventRepository.saveAndFlush(newEvent);

        // Process place, artists and tags efficiently
        assignPlace(savedEvent, eventNode);
        processRelationships(savedEvent, eventNode);

        return savedEvent;
    }

    /**
     * Creates an Event entity from JsonNode data.
     */
    private Event createEventFromNode(JsonNode eventNode) {
        Event newEvent = new Event();

        JsonNode nameNode = eventNode.get("event_name");
        newEvent.setEvent_name(nameNode != null ? nameNode.asText() : "Unknown Event");

        // Handle dates with default values
        JsonNode startDateNode = eventNode.get("start_date");
        LocalDateTime startDate = startDateNode != null ? parseTimestamp(startDateNode.asText()) : null;
        newEvent.setStart_date(startDate != null ? startDate : LocalDateTime.now());

        JsonNode endDateNode = eventNode.get("end_date");
        LocalDateTime endDate = endDateNode != null ? parseTimestamp(endDateNode.asText()) : null;
        newEvent.setEnd_date(endDate != null ? endDate : LocalDateTime.now().plusHours(1));

        JsonNode thumbnailNode = eventNode.get("thumbnail");
        newEvent.setThumbnail(thumbnailNode != null ? thumbnailNode.asText() : "No thumbnail");

        newEvent.setUrl(eventNode.get("url").asText());

        JsonNode locationNode = eventNode.get("location");
        newEvent.setLocation(locationNode != null ? locationNode.asText() : "Unknown Location");

        JsonNode categoryNode = eventNode.get("category");
        newEvent.setCategory(categoryNode != null ? categoryNode.asText() : "Unknown Category");

        JsonNode descriptionNode = eventNode.get("description");
        newEvent.setDescription(descriptionNode != null ? descriptionNode.asText() : "No description available");

        JsonNode sourceNode = eventNode.get("source");
        newEvent.setSource(sourceNode != null ? sourceNode.asText() : "Unknown Source");

        return newEvent;
    }

    /**
     * Gets the event URL from JsonNode.
     */
    private String getEventUrl(JsonNode eventNode) {
        JsonNode urlNode = eventNode.get("url");
        return urlNode != null ? urlNode.asText() : null;
    }

    /**
     * Optimized place assignment using cache.
     */
    private void assignPlace(Event event, JsonNode eventNode) {
        JsonNode placeNode = eventNode.get("place");
        if (placeNode == null || placeNode.asText().isEmpty()) {
            return;
        }

        String placeName = placeNode.asText().trim();

        // Check cache first
        Place place = placeCache.computeIfAbsent(placeName, name -> {
            // Check database if not in cache
            Optional<Place> existingPlace = placeRepository.findByName(name);
            if (existingPlace.isPresent()) {
                return existingPlace.get();
            }

            // Create new place if not found
            Place newPlace = new Place();
            newPlace.setName(name);
            return placeRepository.save(newPlace);
        });

        // Set place without bidirectional relationship management
        event.setPlace(place);
    }

    /**
     * Process artists and tags with batch optimization.
     */
    private void processRelationships(Event event, JsonNode eventNode) {
        // Process artists
        JsonNode artistsNode = eventNode.get("artists");
        if (artistsNode != null && !artistsNode.asText().isEmpty()) {
            Set<String> artistNames = parseNames(artistsNode.asText());
            Set<Artist> artistsToAdd = getOrCreateArtists(artistNames);

            // Add all artists at once, avoiding duplicates
            for (Artist artist : artistsToAdd) {
                // Skip if relationship already exists
                if (!hasArtistRelationship(event.getId(), artist.getId())) {
                    event.getArtists().add(artist);
                }
            }
        }

        // Process tags
        JsonNode tagsNode = eventNode.get("tags");
        if (tagsNode != null && !tagsNode.asText().isEmpty()) {
            Set<String> tagNames = parseNames(tagsNode.asText());
            Set<Tag> tagsToAdd = getOrCreateTags(tagNames);

            // Add all tags at once, avoiding duplicates
            for (Tag tag : tagsToAdd) {
                // Skip if relationship already exists
                if (!hasTagRelationship(event.getId(), tag.getId())) {
                    event.getTags().add(tag);
                }
            }
        }

        // Save the event with its relationships
        eventRepository.save(event);
    }

    /**
     * Check if an event-artist relationship already exists.
     */
    private boolean hasArtistRelationship(Long eventId, Long artistId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM event_artists WHERE event_id = ? AND artist_id = ?",
                Integer.class, eventId, artistId);
        return count != null && count > 0;
    }

    /**
     * Check if an event-tag relationship already exists.
     */
    private boolean hasTagRelationship(Long eventId, Long tagId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM event_tags WHERE event_id = ? AND tag_id = ?",
                Integer.class, eventId, tagId);
        return count != null && count > 0;
    }

    /**
     * Parse comma-separated names into a set of unique, trimmed names.
     */
    private Set<String> parseNames(String namesString) {
        return Arrays.stream(namesString.split(","))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * Get or create artists in batch, using cache for performance.
     */
    private Set<Artist> getOrCreateArtists(Set<String> artistNames) {
        Set<Artist> artists = new HashSet<>();
        Set<String> namesToCreate = new HashSet<>();

        // First check cache
        for (String name : artistNames) {
            Artist cachedArtist = artistCache.get(name);
            if (cachedArtist != null) {
                artists.add(cachedArtist);
            } else {
                namesToCreate.add(name);
            }
        }

        // Batch create missing artists
        if (!namesToCreate.isEmpty()) {
            for (String name : namesToCreate) {
                Artist newArtist = artistCache.computeIfAbsent(name, artistName -> {
                    Optional<Artist> existingArtist = artistRepository.findByName(artistName);
                    if (existingArtist.isPresent()) {
                        return existingArtist.get();
                    }

                    Artist artist = new Artist();
                    artist.setName(artistName);
                    return artistRepository.save(artist);
                });
                artists.add(newArtist);
            }
        }

        return artists;
    }

    /**
     * Get or create tags in batch, using cache for performance.
     */
    private Set<Tag> getOrCreateTags(Set<String> tagNames) {
        Set<Tag> tags = new HashSet<>();
        Set<String> namesToCreate = new HashSet<>();

        // First check cache
        for (String name : tagNames) {
            Tag cachedTag = tagCache.get(name);
            if (cachedTag != null) {
                tags.add(cachedTag);
            } else {
                namesToCreate.add(name);
            }
        }

        // Batch create missing tags
        if (!namesToCreate.isEmpty()) {
            for (String name : namesToCreate) {
                Tag newTag = tagCache.computeIfAbsent(name, tagName -> {
                    Optional<Tag> existingTag = tagRepository.findByName(tagName);
                    if (existingTag.isPresent()) {
                        return existingTag.get();
                    }

                    Tag tag = new Tag();
                    tag.setName(tagName);
                    return tagRepository.save(tag);
                });
                tags.add(newTag);
            }
        }

        return tags;
    }

    /**
     * Parse timestamp string to LocalDateTime.
     */
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