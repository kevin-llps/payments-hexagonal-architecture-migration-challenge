package fr.kevin.llps.payments.processing.outbound.adapter.in.rest.dto;

import java.math.BigDecimal;

public class OutboundPaymentResponse {

    public String id;
    public String instructionId;
    public BigDecimal amount;
    public String currency;
    public String debtorIban;
    public String debtorName;
    public String destinationBic;
    public String requestedExecutionDate;
    public String chargesType;
    public String status;
    public BigDecimal estimatedFee;
}
