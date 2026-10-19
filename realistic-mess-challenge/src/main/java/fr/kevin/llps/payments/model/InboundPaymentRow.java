package fr.kevin.llps.payments.model;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

public class InboundPaymentRow {

    public String id;
    public String creditTransferRef;
    public BigDecimal amount;
    public String currency;
    public String creditorIban;
    public String creditorName;
    public String originBic;
    public Date valueDate;
    public Date settlementDate;
    public String status;
    public Timestamp createdAt;
    public Timestamp updatedAt;
}
