package fr.kevin.llps.payments.compliancehold.port.in;

/**
 * Suspended payment search filters as received from the caller.
 */
public record SuspendedPaymentSearchQuery(String debtorIban,
                                          String holdReason,
                                          String reviewStatus,
                                          String heldFrom,
                                          String heldTo,
                                          Integer limit) {
}
