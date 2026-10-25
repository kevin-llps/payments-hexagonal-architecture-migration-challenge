package fr.kevin.llps.payments.recall.config;

import fr.kevin.llps.payments.recall.RecallQueryService;
import fr.kevin.llps.payments.recall.RecordRecallService;
import fr.kevin.llps.payments.recall.port.in.RecallQueries;
import fr.kevin.llps.payments.recall.port.in.RecordRecallUseCase;
import fr.kevin.llps.payments.recall.port.out.RecallRecordRepository;
import fr.kevin.llps.payments.recall.port.out.RecallSearchRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class RecallConfiguration {

    @Bean
    public RecallQueries recallQueries(RecallSearchRepository repository) {
        return new RecallQueryService(repository);
    }

    @Bean
    public RecordRecallUseCase recordRecallUseCase(RecallRecordRepository repository, Clock clock) {
        return new RecordRecallService(repository, clock);
    }
}
