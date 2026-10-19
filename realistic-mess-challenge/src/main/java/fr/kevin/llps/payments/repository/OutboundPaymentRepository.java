package fr.kevin.llps.payments.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import fr.kevin.llps.payments.dto.OutboundPaymentSearchRequest;
import fr.kevin.llps.payments.model.OutboundPaymentRow;
import fr.kevin.llps.payments.util.PaymentUtils;

@Repository
public class OutboundPaymentRepository {

    @Autowired
    private JdbcTemplate jdbc;

    public List<OutboundPaymentRow> search(OutboundPaymentSearchRequest req) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT id, instruction_id, amount, currency, debtor_iban, debtor_name, destination_bic, requested_execution_date, charges_type, status, created_at, updated_at FROM outbound_payments WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (req.debtorIban != null && !req.debtorIban.isBlank()) {
            sql.append(" AND debtor_iban = ?");
            args.add(req.debtorIban.trim());
        }
        if (req.destinationBic != null && !req.destinationBic.isBlank()) {
            // sic: BIC filter behaves like a "contains" search, decided during a hotfix in 2021
            sql.append(" AND destination_bic LIKE ?");
            args.add("%" + req.destinationBic.trim().toUpperCase() + "%");
        }
        if (req.chargesType != null && !req.chargesType.isBlank()) {
            sql.append(" AND charges_type = ?");
            args.add(req.chargesType.trim().toUpperCase());
        }
        if (req.status != null && !req.status.isBlank()) {
            sql.append(" AND status = ?");
            args.add(req.status.trim().toUpperCase());
        }
        if (req.executionFrom != null && !req.executionFrom.isBlank()) {
            sql.append(" AND requested_execution_date >= ?");
            args.add(java.sql.Date.valueOf(req.executionFrom.trim()));
        }
        if (req.executionTo != null && !req.executionTo.isBlank()) {
            sql.append(" AND requested_execution_date <= ?");
            args.add(java.sql.Date.valueOf(req.executionTo.trim()));
        }
        sql.append(" ORDER BY requested_execution_date DESC LIMIT ").append(PaymentUtils.limitOrDefault(req.limit));
        return jdbc.query(sql.toString(), args.toArray(), (rs, rowNum) -> mapRow(rs));
    }

    public OutboundPaymentRow findById(String id) {
        try {
            return jdbc.queryForObject(
                    "SELECT id, instruction_id, amount, currency, debtor_iban, debtor_name, destination_bic, requested_execution_date, charges_type, status, created_at, updated_at FROM outbound_payments WHERE id = ?::uuid",
                    new Object[]{id}, (rs, rowNum) -> mapRow(rs));
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private OutboundPaymentRow mapRow(ResultSet rs) throws SQLException {
        OutboundPaymentRow row = new OutboundPaymentRow();
        row.id = rs.getString("id");
        row.instructionId = rs.getString("instruction_id");
        row.amount = rs.getBigDecimal("amount");
        row.currency = rs.getString("currency");
        row.debtorIban = rs.getString("debtor_iban");
        row.debtorName = rs.getString("debtor_name");
        row.destinationBic = rs.getString("destination_bic");
        row.requestedExecutionDate = rs.getDate("requested_execution_date");
        row.chargesType = rs.getString("charges_type");
        row.status = rs.getString("status");
        row.createdAt = rs.getTimestamp("created_at");
        row.updatedAt = rs.getTimestamp("updated_at");
        return row;
    }
}
