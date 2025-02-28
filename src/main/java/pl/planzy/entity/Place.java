package pl.planzy.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

@Entity
@Table(name = "places")

public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "place_name", nullable = false)
    private String place_name;

    // Bidirectional relationship with Event
    @OneToMany(mappedBy = "place", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Event> events = new ArrayList<>();

    // Helper methods to maintain bidirectional relationship
    public void addEvent(Event event) {
        events.add(event);
        event.setPlace(this);
    }

    public void removeEvent(Event event) {
        events.remove(event);
        event.setPlace(null);
    }


}
