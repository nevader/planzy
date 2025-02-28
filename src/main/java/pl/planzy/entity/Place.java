package pl.planzy.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

@Entity
@Table(name = "places", indexes = {
        @Index(name = "idx_place_name", columnList = "place_name", unique = true)
})
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "place_name", nullable = false)
    private String name;

    @OneToMany(mappedBy = "place", fetch = FetchType.LAZY)
    private List<Event> events = new ArrayList<>();

    /**
     * Two places are considered equal if they have the same ID, or if both IDs are null
     * and they have the same name (which should be unique).
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Place place = (Place) o;
        return Objects.equals(id, place.id) ||
                (id == null && place.id == null && Objects.equals(name, place.name));
    }

    @Override
    public int hashCode() {
        return Objects.hash(id != null ? id : name);
    }

    @Override
    public String toString() {
        return "Place{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}