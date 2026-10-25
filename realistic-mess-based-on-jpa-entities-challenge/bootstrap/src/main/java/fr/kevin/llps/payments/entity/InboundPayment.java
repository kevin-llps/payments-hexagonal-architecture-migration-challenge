package fr.kevin.llps.payments.entity;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "inbound_payments")
public class InboundPayment {

    @Id
    @Column(name = "id")
    public UUID id;

    @Column(name = "credit_transfer_ref")
    public String creditTransferRef;

    @Column(name = "amount")
    public BigDecimal amount;

    @Column(name = "currency")
    public String currency;

    @Column(name = "creditor_iban")
    public String creditorIban;

    @Column(name = "creditor_name")
    public String creditorName;

    @Column(name = "origin_bic")
    public String originBic;

    @Column(name = "value_date")
    public Date valueDate;

    @Column(name = "settlement_date")
    public Date settlementDate;

    @Column(name = "status")
    public String status;

    @Column(name = "created_at")
    public Timestamp createdAt;

    @Column(name = "updated_at")
    public Timestamp updatedAt;
}
