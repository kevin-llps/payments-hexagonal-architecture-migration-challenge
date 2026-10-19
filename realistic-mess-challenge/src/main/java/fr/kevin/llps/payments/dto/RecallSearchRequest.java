package fr.kevin.llps.payments.dto;

public class RecallSearchRequest {

    public String recallType;
    public String originalPaymentId;
    public String originalPaymentType;
    public String reasonCode;
    public String status;
    public String initiatedFrom;
    public String initiatedTo;
    public Integer limit;
}
