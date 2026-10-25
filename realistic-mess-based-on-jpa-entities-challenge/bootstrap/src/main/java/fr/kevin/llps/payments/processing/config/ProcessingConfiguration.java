package fr.kevin.llps.payments.processing.config;

import fr.kevin.llps.payments.processing.inbound.InboundPaymentQueryService;
import fr.kevin.llps.payments.processing.inbound.RecordInboundPaymentService;
import fr.kevin.llps.payments.processing.inbound.port.in.InboundPaymentQueries;
import fr.kevin.llps.payments.processing.inbound.port.in.RecordInboundPaymentUseCase;
import fr.kevin.llps.payments.processing.inbound.port.out.InboundPaymentRecordRepository;
import fr.kevin.llps.payments.processing.inbound.port.out.InboundPaymentSearchRepository;
import fr.kevin.llps.payments.processing.instant.InstantPaymentQueryService;
import fr.kevin.llps.payments.processing.outbound.DetailFeeEstimate;
import fr.kevin.llps.payments.processing.outbound.OutboundPaymentQueryService;
import fr.kevin.llps.payments.processing.outbound.RecordOutboundPaymentService;
import fr.kevin.llps.payments.processing.outbound.SearchFeeEstimate;
import fr.kevin.llps.payments.processing.outbound.port.in.OutboundPaymentQueries;
import fr.kevin.llps.payments.processing.outbound.port.in.RecordOutboundPaymentUseCase;
import fr.kevin.llps.payments.processing.outbound.port.out.OutboundPaymentRecordRepository;
import fr.kevin.llps.payments.processing.outbound.port.out.OutboundPaymentSearchRepository;
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

    /**
     * Search and details deliberately price with different tariffs (C-01).
     */
    @Bean
    public OutboundPaymentQueries outboundPaymentQueries(OutboundPaymentSearchRepository repository) {
        return new OutboundPaymentQueryService(repository, new SearchFeeEstimate(), new DetailFeeEstimate());
    }

    @Bean
    public RecordOutboundPaymentUseCase recordOutboundPaymentUseCase(OutboundPaymentRecordRepository repository,
                                                                     Clock clock) {
        return new RecordOutboundPaymentService(repository, clock);
    }
}
