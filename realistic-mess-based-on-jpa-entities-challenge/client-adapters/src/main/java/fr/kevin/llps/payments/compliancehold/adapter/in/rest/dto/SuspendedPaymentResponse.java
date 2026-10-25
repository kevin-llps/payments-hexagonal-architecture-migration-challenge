package fr.kevin.llps.payments.compliancehold.adapter.in.rest.dto;

import java.math.BigDecimal;

public class SuspendedPaymentResponse {

    public String id;
    public BigDecimal amount;
    public String currency;
    public String debtorIban;
    public String debtorName;
    public String creditorIban;
    public String creditorName;
    public String holdReason;
    public String complianceCaseRef;
    public String reviewStatus;
    public String heldAt;
    public String releasedAt;
}
