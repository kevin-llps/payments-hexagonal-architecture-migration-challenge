package fr.kevin.llps.payments.compliancehold.adapter.out.persistence.entity;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "suspended_payments")
public class SuspendedPayment {

    @Id
    @Column(name = "id")
    public UUID id;

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

    @Column(name = "hold_reason")
    public String holdReason;

    @Column(name = "compliance_case_ref")
    public String complianceCaseRef;

    @Column(name = "review_status")
    public String reviewStatus;

    @Column(name = "held_at")
    public Timestamp heldAt;

    @Column(name = "released_at")
    public Timestamp releasedAt;

    @Column(name = "created_at")
    public Timestamp createdAt;

    @Column(name = "updated_at")
    public Timestamp updatedAt;
}
