package pl.planzy.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.planzy.entity.Place;

public interface PlaceRepository extends JpaRepository<Place, Long> {
}
