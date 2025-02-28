package pl.planzy.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor

@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_name", nullable = false, length = 500)
    private String event_name;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime start_date;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime end_date;

    @Column(name = "thumbnail", nullable = false, length = 500)
    private String thumbnail;

    @Column(name = "url", nullable = false, length = 500, unique = true)
    private String url;

    @Column(name = "location", nullable = false, length = 500)
    private String location;

    @Column(name = "category", nullable = false, length = 500)
    private String category;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "source", nullable = false, length = 500)
    private String source;

    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "event_artists",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "artist_id")
    )
    private Set<Artist> artists = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "event_tags",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id")
    private Place place;

    // Simplified method that avoids bidirectional relationship issues
    public void setPlace(Place place) {
        this.place = place;
    }

    /**
     * Two events are considered equal if they have the same ID, or if both IDs are null
     * and they have the same URL (which should be unique).
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return Objects.equals(id, event.id) ||
                (id == null && event.id == null && Objects.equals(url, event.url));
    }

    @Override
    public int hashCode() {
        return Objects.hash(id != null ? id : url);
    }

    @Override
    public String toString() {
        return "Event{" +
                "id=" + id +
                ", event_name='" + event_name + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}