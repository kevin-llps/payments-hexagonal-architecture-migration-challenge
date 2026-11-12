package fr.kevin.llps.payments.dto;

public class InstantPaymentSearchRequest {

    public String endToEndId;
    public String debtorIban;
    public String creditorIban;
    public String status;
    public String currency;
    public String executionFrom;
    public String executionTo;
    public Integer limit;
}
