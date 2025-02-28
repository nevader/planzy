package pl.planzy.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.planzy.entity.Artist;
import pl.planzy.entity.Event;
import pl.planzy.entity.Tag;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

/**
 * Utility class for managing entity relationships while avoiding duplicates.
 * Provides methods to safely add entities to collections without causing constraint violations.
 */
public class EntityRelationshipHelper {
    private static final Logger logger = LoggerFactory.getLogger(EntityRelationshipHelper.class);

    /**
     * Safely adds entities to a collection, avoiding duplicates.
     *
     * @param existingEntities The collection to add to
     * @param newEntities Entities to add
     * @param idExtractor Function to extract the ID from an entity
     * @param <T> Entity type
     * @param <ID> ID type
     * @return Number of entities actually added
     */
    public static <T, ID> int addEntitiesWithoutDuplicates(
            Collection<T> existingEntities,
            Collection<T> newEntities,
            Function<T, ID> idExtractor) {

        if (newEntities == null || newEntities.isEmpty()) {
            return 0;
        }

        // Create a set of existing IDs for quick lookup
        Set<ID> existingIds = new HashSet<>();
        for (T entity : existingEntities) {
            ID id = idExtractor.apply(entity);
            if (id != null) {
                existingIds.add(id);
            }
        }

        // Add only entities that don't have IDs already in the set
        int addedCount = 0;
        for (T entity : newEntities) {
            ID id = idExtractor.apply(entity);
            if (id == null || !existingIds.contains(id)) {
                existingEntities.add(entity);
                if (id != null) {
                    existingIds.add(id);
                }
                addedCount++;
            }
        }

        return addedCount;
    }

    /**
     * Safely adds artists to an event, avoiding duplicates.
     *
     * @param event The event to add artists to
     * @param artists The artists to add
     * @return Number of artists actually added
     */
    public static int addArtistsToEvent(Event event, Collection<Artist> artists) {
        return addEntitiesWithoutDuplicates(
                event.getArtists(),
                artists,
                Artist::getId
        );
    }

    /**
     * Safely adds tags to an event, avoiding duplicates.
     *
     * @param event The event to add tags to
     * @param tags The tags to add
     * @return Number of tags actually added
     */
    public static int addTagsToEvent(Event event, Collection<Tag> tags) {
        return addEntitiesWithoutDuplicates(
                event.getTags(),
                tags,
                Tag::getId
        );
    }
}