package fr.kevin.llps.payments.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.kevin.llps.payments.dto.InboundPaymentResponse;
import fr.kevin.llps.payments.dto.InboundPaymentSearchRequest;
import fr.kevin.llps.payments.repository.InboundPaymentRepository;

@Service
public class InboundPaymentService {

    @Autowired
    private InboundPaymentRepository repository;

    public List<InboundPaymentResponse> search(InboundPaymentSearchRequest request) {
        return repository.search(request);
    }

    public InboundPaymentResponse getById(String id) {
        InboundPaymentResponse response = repository.findById(id);
        if (response != null && response.valueDate != null) {
            LocalDate valueDate = LocalDate.parse(response.valueDate);
            response.ageInDays = ChronoUnit.DAYS.between(valueDate, LocalDate.now());
        }
        return response;
    }
}
