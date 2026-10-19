package fr.kevin.llps.payments.model;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

public class RecallRow {

    public String id;
    public String recallReference;
    public String recallType;
    public String originalPaymentId;
    public String originalPaymentType;
    public String reasonCode;
    public BigDecimal amount;
    public String currency;
    public Timestamp initiatedAt;
    public Date deadline;
    public String status;
    public Timestamp createdAt;
    public Timestamp updatedAt;
}
