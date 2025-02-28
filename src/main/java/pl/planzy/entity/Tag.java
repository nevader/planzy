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
@Table(name = "tags", indexes = {
        @Index(name = "idx_tag_name", columnList = "tag_name", unique = true)
})
public class Tag {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "tag_name", nullable = false)
        private String name;

        @ManyToMany(mappedBy = "tags", fetch = FetchType.LAZY)
        private Set<Event> events = new HashSet<>();

        /**
         * Two tags are considered equal if they have the same ID, or if both IDs are null
         * and they have the same name (which should be unique).
         */
        @Override
        public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                Tag tag = (Tag) o;
                return Objects.equals(id, tag.id) ||
                        (id == null && tag.id == null && Objects.equals(name, tag.name));
        }

        @Override
        public int hashCode() {
                return Objects.hash(id != null ? id : name);
        }

        @Override
        public String toString() {
                return "Tag{" +
                        "id=" + id +
                        ", name='" + name + '\'' +
                        '}';
        }
}