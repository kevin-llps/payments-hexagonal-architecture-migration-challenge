package fr.kevin.llps.payments.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class InstantPaymentRow {

    public String id;
    public String endToEndId;
    public String uetr;
    public BigDecimal amount;
    public String currency;
    public String debtorIban;
    public String debtorName;
    public String creditorIban;
    public String creditorName;
    public Timestamp executionDatetime;
    public String status;
    public String scheme;
    public Timestamp createdAt;
    public Timestamp updatedAt;
}
