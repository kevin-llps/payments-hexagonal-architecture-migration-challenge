package fr.kevin.llps.payments.processing.instant.adapter.in.rest.dto;

import java.math.BigDecimal;

public class InstantPaymentResponse {

    public String id;
    public String endToEndId;
    public String uetr;
    public BigDecimal amount;
    public String currency;
    public String debtorIban;
    public String debtorName;
    public String creditorIban;
    public String creditorName;
    public String executionDatetime;
    public String status;
    public String scheme;
}
