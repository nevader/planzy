package pl.planzy.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.planzy.dto.SampleDto;
import pl.planzy.entity.Sample;
import pl.planzy.repository.SampleRepository;



public interface SampleService {

    void addSample(Sample sample);


}
