package fr.kevin.llps.payments.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import fr.kevin.llps.payments.dto.InboundPaymentResponse;
import fr.kevin.llps.payments.dto.InboundPaymentSearchRequest;

@Repository
public class InboundPaymentRepository {

    @Autowired
    private JdbcTemplate jdbc;

    public List<InboundPaymentResponse> search(InboundPaymentSearchRequest req) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT id, credit_transfer_ref, amount, currency, creditor_iban, creditor_name, origin_bic, value_date, settlement_date, status FROM inbound_payments WHERE 1=1");
        if (req.creditorIban != null && !req.creditorIban.isBlank()) {
            sql.append(" AND creditor_iban = '").append(req.creditorIban.trim()).append("'");
        }
        if (req.originBic != null && !req.originBic.isBlank()) {
            sql.append(" AND UPPER(origin_bic) LIKE '%").append(req.originBic.trim().toUpperCase()).append("%'");
        }
        if (req.status != null && !req.status.isBlank()) {
            sql.append(" AND status = '").append(req.status.trim().toUpperCase()).append("'");
        }
        if (req.currency != null && !req.currency.isBlank()) {
            sql.append(" AND currency = '").append(req.currency.trim().toUpperCase()).append("'");
        }
        if (req.valueDateFrom != null && !req.valueDateFrom.isBlank()) {
            sql.append(" AND value_date >= '").append(req.valueDateFrom.trim()).append("'");
        }
        if (req.valueDateTo != null && !req.valueDateTo.isBlank()) {
            sql.append(" AND value_date <= '").append(req.valueDateTo.trim()).append("'");
        }
        sql.append(" ORDER BY value_date DESC, created_at DESC LIMIT ").append(req.limit == null ? 50 : req.limit);
        return jdbc.query(sql.toString(), (rs, rowNum) -> mapRow(rs));
    }

    public InboundPaymentResponse findById(String id) {
        try {
            return jdbc.queryForObject(
                    "SELECT id, credit_transfer_ref, amount, currency, creditor_iban, creditor_name, origin_bic, value_date, settlement_date, status FROM inbound_payments WHERE id = ?::uuid",
                    new Object[]{id}, (rs, rowNum) -> mapRow(rs));
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private InboundPaymentResponse mapRow(ResultSet rs) throws SQLException {
        InboundPaymentResponse r = new InboundPaymentResponse();
        r.id = rs.getString("id");
        r.creditTransferRef = rs.getString("credit_transfer_ref");
        r.amount = rs.getBigDecimal("amount");
        r.currency = rs.getString("currency");
        r.creditorIban = rs.getString("creditor_iban");
        r.creditorName = rs.getString("creditor_name");
        r.originBic = rs.getString("origin_bic");
        java.sql.Date valueDate = rs.getDate("value_date");
        r.valueDate = valueDate == null ? null : valueDate.toString();
        java.sql.Date settlementDate = rs.getDate("settlement_date");
        r.settlementDate = settlementDate == null ? null : settlementDate.toString();
        r.status = rs.getString("status");
        return r;
    }
}
