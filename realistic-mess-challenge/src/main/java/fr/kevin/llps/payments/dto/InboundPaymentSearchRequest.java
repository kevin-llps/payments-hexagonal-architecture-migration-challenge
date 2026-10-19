package fr.kevin.llps.payments.dto;

public class InboundPaymentSearchRequest {

    public String creditorIban;
    public String originBic;
    public String status;
    public String currency;
    public String valueDateFrom;
    public String valueDateTo;
    public Integer limit;
}
