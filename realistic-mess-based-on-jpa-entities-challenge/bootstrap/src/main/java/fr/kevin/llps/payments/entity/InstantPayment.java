package fr.kevin.llps.payments.entity;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "instant_payments")
public class InstantPayment {

    @Id
    @Column(name = "id")
    public UUID id;

    @Column(name = "end_to_end_id")
    public String endToEndId;

    @Column(name = "uetr")
    public String uetr;

    @Column(name = "amount")
    public BigDecimal amount;

    @Column(name = "currency")
    public String currency;

    @Column(name = "debtor_iban")
    public String debtorIban;

    @Column(name = "debtor_name")
    public String debtorName;

    @Column(name = "creditor_iban")
    public String creditorIban;

    @Column(name = "creditor_name")
    public String creditorName;

    @Column(name = "execution_datetime")
    public Timestamp executionDatetime;

    @Column(name = "status")
    public String status;

    @Column(name = "scheme")
    public String scheme;

    @Column(name = "created_at")
    public Timestamp createdAt;

    @Column(name = "updated_at")
    public Timestamp updatedAt;
}
