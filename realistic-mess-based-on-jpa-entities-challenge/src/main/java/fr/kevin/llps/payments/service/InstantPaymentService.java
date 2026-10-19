package fr.kevin.llps.payments.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.kevin.llps.payments.dto.InstantPaymentResponse;
import fr.kevin.llps.payments.dto.InstantPaymentSearchRequest;
import fr.kevin.llps.payments.repository.InstantPaymentRepository;
import fr.kevin.llps.payments.util.PaymentUtils;

@Service
public class InstantPaymentService {

    @Autowired
    private InstantPaymentRepository repository;

    public List<InstantPaymentResponse> search(InstantPaymentSearchRequest request) {
        if (request.debtorIban != null && !request.debtorIban.isBlank() && !PaymentUtils.isValidIban(request.debtorIban)) {
            throw new IllegalArgumentException("Invalid debtor IBAN");
        }
        if (request.creditorIban != null && !request.creditorIban.isBlank() && !PaymentUtils.isValidIban(request.creditorIban)) {
            throw new IllegalArgumentException("Invalid creditor IBAN");
        }
        return repository.search(request);
    }

    public InstantPaymentResponse getById(String id) {
        return repository.findById(id);
    }
}
