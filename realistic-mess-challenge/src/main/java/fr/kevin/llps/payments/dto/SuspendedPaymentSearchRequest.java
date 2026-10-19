package fr.kevin.llps.payments.dto;

public class SuspendedPaymentSearchRequest {

    public String debtorIban;
    public String holdReason;
    public String reviewStatus;
    public String heldFrom;
    public String heldTo;
    public Integer limit;
}
