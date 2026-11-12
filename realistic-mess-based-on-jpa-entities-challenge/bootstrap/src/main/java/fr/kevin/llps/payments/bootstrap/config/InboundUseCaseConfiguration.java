package fr.kevin.llps.payments.bootstrap.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.kevin.llps.payments.domain.inbound.GetInboundPaymentDetailsService;
import fr.kevin.llps.payments.domain.inbound.PersistInboundPaymentService;
import fr.kevin.llps.payments.domain.inbound.SearchInboundPaymentsService;
import fr.kevin.llps.payments.domain.inbound.port.in.GetInboundPaymentDetails;
import fr.kevin.llps.payments.domain.inbound.port.in.PersistInboundPayment;
import fr.kevin.llps.payments.domain.inbound.port.in.SearchInboundPayments;
import fr.kevin.llps.payments.domain.inbound.port.out.LoadInboundPayments;
import fr.kevin.llps.payments.domain.inbound.port.out.SaveInboundPayment;

@Configuration
public class InboundUseCaseConfiguration {

    @Bean
    public SearchInboundPayments searchInboundPayments(LoadInboundPayments loadInboundPayments, Clock clock) {
        return new SearchInboundPaymentsService(loadInboundPayments, clock);
    }

    @Bean
    public GetInboundPaymentDetails getInboundPaymentDetails(LoadInboundPayments loadInboundPayments, Clock clock) {
        return new GetInboundPaymentDetailsService(loadInboundPayments, clock);
    }

    @Bean
    public PersistInboundPayment persistInboundPayment(SaveInboundPayment saveInboundPayment, Clock clock) {
        return new PersistInboundPaymentService(saveInboundPayment, clock);
    }
}
