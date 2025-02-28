package pl.planzy.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.planzy.entity.Tag;

import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByName(String name);
}
