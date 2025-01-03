package pl.planzy;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import pl.planzy.entity.Sample;
import pl.planzy.service.SampleService;
import pl.planzy.service.WebScraperService;

@SpringBootTest
@ActiveProfiles("test")
public class SampleServiceTest {

    @Autowired
    private SampleService sampleService;

    @Autowired
    private WebScraperService webScraperService;

    @Test
    public void testAddSample() {
        // given
        Sample sample = new Sample();

        sample.setFirstName("firstName");
        sample.setLastName("lastName");
        sample.setUsername("username");
        sample.setEmail("email");

        // when
        sampleService.addSample(sample);

        // then
        // verify
    }

    @Test
    public void testWeb() {

        webScraperService.scrapeData();
    }
}
