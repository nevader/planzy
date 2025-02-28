package pl.planzy.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.planzy.entity.Place;

import java.util.Optional;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    Optional<Place> findByName(String name);
}
