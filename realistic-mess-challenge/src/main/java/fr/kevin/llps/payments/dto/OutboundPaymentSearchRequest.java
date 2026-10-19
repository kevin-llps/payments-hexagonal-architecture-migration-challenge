package fr.kevin.llps.payments.dto;

public class OutboundPaymentSearchRequest {

    public String debtorIban;
    public String destinationBic;
    public String chargesType;
    public String status;
    public String executionFrom;
    public String executionTo;
    public Integer limit;
}
