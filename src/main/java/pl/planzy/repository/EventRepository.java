package pl.planzy.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.planzy.entity.Event;

import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    Optional<Event> findByUrl(String url);
}
