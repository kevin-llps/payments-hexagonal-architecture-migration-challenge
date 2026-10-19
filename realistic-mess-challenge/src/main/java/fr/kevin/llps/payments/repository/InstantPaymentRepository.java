package fr.kevin.llps.payments.repository;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import fr.kevin.llps.payments.dto.InstantPaymentResponse;
import fr.kevin.llps.payments.dto.InstantPaymentSearchRequest;
import fr.kevin.llps.payments.util.PaymentUtils;

@Repository
public class InstantPaymentRepository {

    @Autowired
    private JdbcTemplate jdbc;

    public List<InstantPaymentResponse> search(InstantPaymentSearchRequest req) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT id, end_to_end_id, uetr, amount, currency, debtor_iban, debtor_name, creditor_iban, creditor_name, execution_datetime, status, scheme FROM instant_payments WHERE 1=1");
        if (req.endToEndId != null && !req.endToEndId.isBlank()) {
            sql.append(" AND end_to_end_id = '").append(req.endToEndId.trim()).append("'");
        }
        if (req.debtorIban != null && !req.debtorIban.isBlank()) {
            sql.append(" AND UPPER(debtor_iban) LIKE '").append(PaymentUtils.likePattern(req.debtorIban)).append("'");
        }
        if (req.creditorIban != null && !req.creditorIban.isBlank()) {
            sql.append(" AND UPPER(creditor_iban) LIKE '").append(PaymentUtils.likePattern(req.creditorIban)).append("'");
        }
        if (req.status != null && !req.status.isBlank()) {
            sql.append(" AND status = '").append(req.status.trim().toUpperCase()).append("'");
        }
        if (req.currency != null && !req.currency.isBlank()) {
            sql.append(" AND currency = '").append(req.currency.trim().toUpperCase()).append("'");
        }
        if (req.executionFrom != null && !req.executionFrom.isBlank()) {
            sql.append(" AND execution_datetime >= '").append(req.executionFrom.trim()).append("'");
        }
        if (req.executionTo != null && !req.executionTo.isBlank()) {
            sql.append(" AND execution_datetime <= '").append(req.executionTo.trim()).append(" 23:59:59'");
        }
        sql.append(" ORDER BY execution_datetime DESC LIMIT ").append(PaymentUtils.limitOrDefault(req.limit));

        return jdbc.query(sql.toString(), (rs, rowNum) -> {
            InstantPaymentResponse r = new InstantPaymentResponse();
            r.id = rs.getString("id");
            r.endToEndId = rs.getString("end_to_end_id");
            r.uetr = rs.getString("uetr");
            r.amount = rs.getBigDecimal("amount");
            r.currency = rs.getString("currency");
            r.debtorIban = rs.getString("debtor_iban");
            r.debtorName = rs.getString("debtor_name");
            r.creditorIban = rs.getString("creditor_iban");
            r.creditorName = rs.getString("creditor_name");
            Timestamp ts = rs.getTimestamp("execution_datetime");
            r.executionDatetime = ts == null ? null : PaymentUtils.FMT.format(ts);
            r.status = rs.getString("status");
            r.scheme = rs.getString("scheme");
            return r;
        });
    }

    public InstantPaymentResponse findById(String id) {
        try {
            return jdbc.queryForObject(
                    "SELECT id, end_to_end_id, uetr, amount, currency, debtor_iban, debtor_name, creditor_iban, creditor_name, execution_datetime, status, scheme FROM instant_payments WHERE id = ?::uuid",
                    new Object[]{id},
                    (rs, rowNum) -> {
                        InstantPaymentResponse r = new InstantPaymentResponse();
                        r.id = rs.getString("id");
                        r.endToEndId = rs.getString("end_to_end_id");
                        r.uetr = rs.getString("uetr");
                        r.amount = rs.getBigDecimal("amount");
                        r.currency = rs.getString("currency");
                        r.debtorIban = rs.getString("debtor_iban");
                        r.debtorName = rs.getString("debtor_name");
                        r.creditorIban = rs.getString("creditor_iban");
                        r.creditorName = rs.getString("creditor_name");
                        Timestamp ts = rs.getTimestamp("execution_datetime");
                        r.executionDatetime = ts == null ? null : PaymentUtils.FMT.format(ts);
                        r.status = rs.getString("status");
                        r.scheme = rs.getString("scheme");
                        return r;
                    });
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }
}
