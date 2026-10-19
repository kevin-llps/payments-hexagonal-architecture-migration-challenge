package fr.kevin.llps.payments.dto;

import java.math.BigDecimal;

public class RecallResponse {

    public String id;
    public String recallReference;
    public String recallType;
    public String originalPaymentId;
    public String originalPaymentType;
    public String reasonCode;
    public BigDecimal amount;
    public String currency;
    public String initiatedAt;
    public String deadline;
    public String status;
}
