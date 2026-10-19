package fr.kevin.llps.payments.model;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

public class OutboundPaymentRow {

    public String id;
    public String instructionId;
    public BigDecimal amount;
    public String currency;
    public String debtorIban;
    public String debtorName;
    public String destinationBic;
    public Date requestedExecutionDate;
    public String chargesType;
    public String status;
    public Timestamp createdAt;
    public Timestamp updatedAt;
}
