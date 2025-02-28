package pl.planzy.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.planzy.entity.Artist;

public interface ArtistRepository extends JpaRepository<Artist, Long> {
}
