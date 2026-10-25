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
@Table(name = "outbound_payments")
public class OutboundPayment {

    @Id
    @Column(name = "id")
    public UUID id;

    @Column(name = "instruction_id")
    public String instructionId;

    @Column(name = "amount")
    public BigDecimal amount;

    @Column(name = "currency")
    public String currency;

    @Column(name = "debtor_iban")
    public String debtorIban;

    @Column(name = "debtor_name")
    public String debtorName;

    @Column(name = "destination_bic")
    public String destinationBic;

    @Column(name = "requested_execution_date")
    public Date requestedExecutionDate;

    @Column(name = "charges_type")
    public String chargesType;

    @Column(name = "status")
    public String status;

    @Column(name = "created_at")
    public Timestamp createdAt;

    @Column(name = "updated_at")
    public Timestamp updatedAt;
}
