package pl.planzy.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

@Entity
@Table(name = "artists", indexes = {
        @Index(name = "idx_artist_name", columnList = "artist_name", unique = true)
})
public class Artist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "artist_name", nullable = false)
    private String name;

    @ManyToMany(mappedBy = "artists", fetch = FetchType.LAZY)
    private Set<Event> events = new HashSet<>();

    /**
     * Two artists are considered equal if they have the same ID, or if both IDs are null
     * and they have the same name (which should be unique).
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Artist artist = (Artist) o;
        return Objects.equals(id, artist.id) ||
                (id == null && artist.id == null && Objects.equals(name, artist.name));
    }

    @Override
    public int hashCode() {
        return Objects.hash(id != null ? id : name);
    }

    @Override
    public String toString() {
        return "Artist{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}