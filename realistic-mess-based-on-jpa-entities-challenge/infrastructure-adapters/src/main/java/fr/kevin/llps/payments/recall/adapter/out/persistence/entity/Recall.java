package fr.kevin.llps.payments.recall.adapter.out.persistence.entity;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "recalls")
public class Recall {

    @Id
    @Column(name = "id")
    public UUID id;

    @Column(name = "recall_reference")
    public String recallReference;

    @Column(name = "recall_type")
    public String recallType;

    @Column(name = "original_payment_id")
    public UUID originalPaymentId;

    @Column(name = "original_payment_type")
    public String originalPaymentType;

    @Column(name = "reason_code")
    public String reasonCode;

    @Column(name = "amount")
    public BigDecimal amount;

    @Column(name = "currency")
    public String currency;

    @Column(name = "initiated_at")
    public Timestamp initiatedAt;

    @Column(name = "deadline")
    public Date deadline;

    @Column(name = "status")
    public String status;

    @Column(name = "created_at")
    public Timestamp createdAt;

    @Column(name = "updated_at")
    public Timestamp updatedAt;
}
