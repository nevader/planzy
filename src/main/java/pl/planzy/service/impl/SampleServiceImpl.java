package pl.planzy.service.impl;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.planzy.dto.SampleDto;
import pl.planzy.entity.Sample;
import pl.planzy.repository.SampleRepository;
import pl.planzy.service.SampleService;

@Service
@RequiredArgsConstructor

public class SampleServiceImpl implements SampleService {

    private final SampleRepository sampleRepository;

    @Override
    public void addSample(Sample sample) {

        sampleRepository.save(sample);
    }

}
