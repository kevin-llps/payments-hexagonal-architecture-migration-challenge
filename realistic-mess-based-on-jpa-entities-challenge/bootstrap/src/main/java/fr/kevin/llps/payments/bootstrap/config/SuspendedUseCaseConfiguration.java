package fr.kevin.llps.payments.bootstrap.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.kevin.llps.payments.domain.suspended.GetSuspendedPaymentDetailsService;
import fr.kevin.llps.payments.domain.suspended.PersistSuspendedPaymentService;
import fr.kevin.llps.payments.domain.suspended.SearchSuspendedPaymentsService;
import fr.kevin.llps.payments.domain.suspended.port.in.GetSuspendedPaymentDetails;
import fr.kevin.llps.payments.domain.suspended.port.in.PersistSuspendedPayment;
import fr.kevin.llps.payments.domain.suspended.port.in.SearchSuspendedPayments;
import fr.kevin.llps.payments.domain.suspended.port.out.LoadSuspendedPayments;
import fr.kevin.llps.payments.domain.suspended.port.out.SaveSuspendedPayment;

@Configuration
public class SuspendedUseCaseConfiguration {

    @Bean
    public SearchSuspendedPayments searchSuspendedPayments(LoadSuspendedPayments loadSuspendedPayments) {
        return new SearchSuspendedPaymentsService(loadSuspendedPayments);
    }

    @Bean
    public GetSuspendedPaymentDetails getSuspendedPaymentDetails(LoadSuspendedPayments loadSuspendedPayments) {
        return new GetSuspendedPaymentDetailsService(loadSuspendedPayments);
    }

    @Bean
    public PersistSuspendedPayment persistSuspendedPayment(SaveSuspendedPayment saveSuspendedPayment, Clock clock) {
        return new PersistSuspendedPaymentService(saveSuspendedPayment, clock);
    }
}
