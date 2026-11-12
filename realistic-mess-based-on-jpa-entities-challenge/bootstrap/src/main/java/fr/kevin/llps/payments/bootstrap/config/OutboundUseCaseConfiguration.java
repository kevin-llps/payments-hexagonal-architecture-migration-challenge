package fr.kevin.llps.payments.bootstrap.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.kevin.llps.payments.domain.outbound.DetailsFeePolicy;
import fr.kevin.llps.payments.domain.outbound.GetOutboundPaymentDetailsService;
import fr.kevin.llps.payments.domain.outbound.PersistOutboundPaymentService;
import fr.kevin.llps.payments.domain.outbound.SearchFeePolicy;
import fr.kevin.llps.payments.domain.outbound.SearchOutboundPaymentsService;
import fr.kevin.llps.payments.domain.outbound.port.in.GetOutboundPaymentDetails;
import fr.kevin.llps.payments.domain.outbound.port.in.PersistOutboundPayment;
import fr.kevin.llps.payments.domain.outbound.port.in.SearchOutboundPayments;
import fr.kevin.llps.payments.domain.outbound.port.out.LoadOutboundPayments;
import fr.kevin.llps.payments.domain.outbound.port.out.SaveOutboundPayment;

@Configuration
public class OutboundUseCaseConfiguration {

    @Bean
    public SearchFeePolicy searchFeePolicy() {
        return new SearchFeePolicy();
    }

    @Bean
    public DetailsFeePolicy detailsFeePolicy() {
        return new DetailsFeePolicy();
    }

    @Bean
    public SearchOutboundPayments searchOutboundPayments(LoadOutboundPayments loadOutboundPayments,
            SearchFeePolicy searchFeePolicy) {
        return new SearchOutboundPaymentsService(loadOutboundPayments, searchFeePolicy);
    }

    @Bean
    public GetOutboundPaymentDetails getOutboundPaymentDetails(LoadOutboundPayments loadOutboundPayments,
            DetailsFeePolicy detailsFeePolicy) {
        return new GetOutboundPaymentDetailsService(loadOutboundPayments, detailsFeePolicy);
    }

    @Bean
    public PersistOutboundPayment persistOutboundPayment(SaveOutboundPayment saveOutboundPayment, Clock clock) {
        return new PersistOutboundPaymentService(saveOutboundPayment, clock);
    }
}
