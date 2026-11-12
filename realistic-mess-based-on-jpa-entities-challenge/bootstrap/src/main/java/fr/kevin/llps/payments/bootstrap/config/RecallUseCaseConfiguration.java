package fr.kevin.llps.payments.bootstrap.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.kevin.llps.payments.domain.recall.GetRecallDetailsService;
import fr.kevin.llps.payments.domain.recall.PersistRecallService;
import fr.kevin.llps.payments.domain.recall.SearchRecallsService;
import fr.kevin.llps.payments.domain.recall.port.in.GetRecallDetails;
import fr.kevin.llps.payments.domain.recall.port.in.PersistRecall;
import fr.kevin.llps.payments.domain.recall.port.in.SearchRecalls;
import fr.kevin.llps.payments.domain.recall.port.out.LoadRecalls;
import fr.kevin.llps.payments.domain.recall.port.out.SaveRecall;

@Configuration
public class RecallUseCaseConfiguration {

    @Bean
    public SearchRecalls searchRecalls(LoadRecalls loadRecalls) {
        return new SearchRecallsService(loadRecalls);
    }

    @Bean
    public GetRecallDetails getRecallDetails(LoadRecalls loadRecalls) {
        return new GetRecallDetailsService(loadRecalls);
    }

    @Bean
    public PersistRecall persistRecall(SaveRecall saveRecall, Clock clock) {
        return new PersistRecallService(saveRecall, clock);
    }
}
