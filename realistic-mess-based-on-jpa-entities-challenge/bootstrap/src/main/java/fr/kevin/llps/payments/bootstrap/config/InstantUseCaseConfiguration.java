package fr.kevin.llps.payments.bootstrap.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.kevin.llps.payments.domain.instant.GetInstantPaymentDetailsService;
import fr.kevin.llps.payments.domain.instant.PersistInstantPaymentService;
import fr.kevin.llps.payments.domain.instant.SearchInstantPaymentsService;
import fr.kevin.llps.payments.domain.instant.port.in.GetInstantPaymentDetails;
import fr.kevin.llps.payments.domain.instant.port.in.PersistInstantPayment;
import fr.kevin.llps.payments.domain.instant.port.in.SearchInstantPayments;
import fr.kevin.llps.payments.domain.instant.port.out.LoadInstantPayments;
import fr.kevin.llps.payments.domain.instant.port.out.SaveInstantPayment;

@Configuration
public class InstantUseCaseConfiguration {

    @Bean
    public SearchInstantPayments searchInstantPayments(LoadInstantPayments loadInstantPayments) {
        return new SearchInstantPaymentsService(loadInstantPayments);
    }

    @Bean
    public GetInstantPaymentDetails getInstantPaymentDetails(LoadInstantPayments loadInstantPayments) {
        return new GetInstantPaymentDetailsService(loadInstantPayments);
    }

    @Bean
    public PersistInstantPayment persistInstantPayment(SaveInstantPayment saveInstantPayment, Clock clock) {
        return new PersistInstantPaymentService(saveInstantPayment, clock);
    }
}
