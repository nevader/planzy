package pl.planzy.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

@Entity
@Table(name = "tags")

public class Tag {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "tag_name", nullable = false)
        private String tag_name;

        @ManyToMany(mappedBy = "tags")
        private Set<Event> events = new HashSet<>();

        // Helper methods to maintain bidirectional relationship
        public void addEvent(Event event) {
                this.events.add(event);
                event.getTags().add(this);
        }

        public void removeEvent(Event event) {
                this.events.remove(event);
                event.getTags().remove(this);
        }
}
