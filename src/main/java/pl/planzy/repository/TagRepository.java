package pl.planzy.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.planzy.entity.Tag;

public interface TagRepository extends JpaRepository<Tag, Long> {
}
