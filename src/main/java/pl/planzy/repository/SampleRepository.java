package pl.planzy.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.planzy.entity.Sample;

public interface SampleRepository extends JpaRepository<Sample, Long> {

}
