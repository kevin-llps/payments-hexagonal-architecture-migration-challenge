package fr.kevin.llps.payments.compliancehold.config;

import fr.kevin.llps.payments.compliancehold.RecordSuspendedPaymentService;
import fr.kevin.llps.payments.compliancehold.SuspendedPaymentQueryService;
import fr.kevin.llps.payments.compliancehold.port.in.RecordSuspendedPaymentUseCase;
import fr.kevin.llps.payments.compliancehold.port.in.SuspendedPaymentQueries;
import fr.kevin.llps.payments.compliancehold.port.out.SuspendedPaymentRecordRepository;
import fr.kevin.llps.payments.compliancehold.port.out.SuspendedPaymentSearchRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ComplianceHoldConfiguration {

    @Bean
    public SuspendedPaymentQueries suspendedPaymentQueries(SuspendedPaymentSearchRepository repository) {
        return new SuspendedPaymentQueryService(repository);
    }

    @Bean
    public RecordSuspendedPaymentUseCase recordSuspendedPaymentUseCase(SuspendedPaymentRecordRepository repository,
                                                                       Clock clock) {
        return new RecordSuspendedPaymentService(repository, clock);
    }
}
