package fr.kevin.llps.payments.processing.inbound.adapter.in.rest.dto;

import java.math.BigDecimal;

public class InboundPaymentResponse {

    public String id;
    public String creditTransferRef;
    public BigDecimal amount;
    public String currency;
    public String creditorIban;
    public String creditorName;
    public String originBic;
    public String valueDate;
    public String settlementDate;
    public String status;
    public Long ageInDays;
}
