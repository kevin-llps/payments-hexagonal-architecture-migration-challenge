package fr.kevin.llps.payments.processing.config;

import fr.kevin.llps.payments.processing.inbound.InboundPaymentQueryService;
import fr.kevin.llps.payments.processing.inbound.RecordInboundPaymentService;
import fr.kevin.llps.payments.processing.inbound.port.in.InboundPaymentQueries;
import fr.kevin.llps.payments.processing.inbound.port.in.RecordInboundPaymentUseCase;
import fr.kevin.llps.payments.processing.inbound.port.out.InboundPaymentRecordRepository;
import fr.kevin.llps.payments.processing.inbound.port.out.InboundPaymentSearchRepository;
import fr.kevin.llps.payments.processing.instant.InstantPaymentQueryService;
import fr.kevin.llps.payments.processing.instant.RecordInstantPaymentService;
import fr.kevin.llps.payments.processing.instant.port.in.InstantPaymentQueries;
import fr.kevin.llps.payments.processing.instant.port.in.RecordInstantPaymentUseCase;
import fr.kevin.llps.payments.processing.instant.port.out.InstantPaymentRecordRepository;
import fr.kevin.llps.payments.processing.instant.port.out.InstantPaymentSearchRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ProcessingConfiguration {

    @Bean
    public InstantPaymentQueries instantPaymentQueries(InstantPaymentSearchRepository repository) {
        return new InstantPaymentQueryService(repository);
    }

    @Bean
    public RecordInstantPaymentUseCase recordInstantPaymentUseCase(InstantPaymentRecordRepository repository,
                                                                   Clock clock) {
        return new RecordInstantPaymentService(repository, clock);
    }

    @Bean
    public InboundPaymentQueries inboundPaymentQueries(InboundPaymentSearchRepository repository, Clock clock) {
        return new InboundPaymentQueryService(repository, clock);
    }

    @Bean
    public RecordInboundPaymentUseCase recordInboundPaymentUseCase(InboundPaymentRecordRepository repository,
                                                                   Clock clock) {
        return new RecordInboundPaymentService(repository, clock);
    }
}
