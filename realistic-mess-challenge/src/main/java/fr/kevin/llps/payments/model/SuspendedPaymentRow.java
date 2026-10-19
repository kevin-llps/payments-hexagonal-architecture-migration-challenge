package fr.kevin.llps.payments.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class SuspendedPaymentRow {

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
    public Timestamp heldAt;
    public Timestamp releasedAt;
    public Timestamp createdAt;
    public Timestamp updatedAt;
}
