package fr.kevin.llps.payments.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import fr.kevin.llps.payments.dto.RecallSearchRequest;
import fr.kevin.llps.payments.model.RecallRow;

@Repository
public class RecallRepository {

    @Autowired
    private JdbcTemplate jdbc;

    public List<RecallRow> search(RecallSearchRequest req) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT id, recall_reference, recall_type, original_payment_id, original_payment_type, reason_code, amount, currency, initiated_at, deadline, status, created_at, updated_at FROM recalls WHERE 1=1");
        if (req.recallType != null && !req.recallType.isBlank()) {
            sql.append(" AND recall_type = '").append(req.recallType.trim().toUpperCase()).append("'");
        }
        if (req.originalPaymentId != null && !req.originalPaymentId.isBlank()) {
            sql.append(" AND original_payment_id = '").append(req.originalPaymentId.trim()).append("'");
        }
        if (req.originalPaymentType != null && !req.originalPaymentType.isBlank()) {
            sql.append(" AND original_payment_type = '").append(req.originalPaymentType.trim().toUpperCase()).append("'");
        }
        if (req.reasonCode != null && !req.reasonCode.isBlank()) {
            sql.append(" AND reason_code = '").append(req.reasonCode.trim().toUpperCase()).append("'");
        }
        if (req.status != null && !req.status.isBlank()) {
            sql.append(" AND status = '").append(req.status.trim().toUpperCase()).append("'");
        }
        if (req.initiatedFrom != null && !req.initiatedFrom.isBlank()) {
            sql.append(" AND initiated_at >= '").append(req.initiatedFrom.trim()).append("'");
        }
        if (req.initiatedTo != null && !req.initiatedTo.isBlank()) {
            sql.append(" AND initiated_at <= '").append(req.initiatedTo.trim()).append("'");
        }
        // recalls are always capped: business asked for 50 in 2019 and nobody changed it
        sql.append(" ORDER BY initiated_at DESC LIMIT 50");
        return jdbc.query(sql.toString(), (rs, rowNum) -> mapRow(rs));
    }

    public RecallRow findById(String id) {
        try {
            return jdbc.queryForObject(
                    "SELECT id, recall_reference, recall_type, original_payment_id, original_payment_type, reason_code, amount, currency, initiated_at, deadline, status, created_at, updated_at FROM recalls WHERE id = ?::uuid",
                    new Object[]{id}, (rs, rowNum) -> mapRow(rs));
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private RecallRow mapRow(ResultSet rs) throws SQLException {
        RecallRow row = new RecallRow();
        row.id = rs.getString("id");
        row.recallReference = rs.getString("recall_reference");
        row.recallType = rs.getString("recall_type");
        row.originalPaymentId = rs.getString("original_payment_id");
        row.originalPaymentType = rs.getString("original_payment_type");
        row.reasonCode = rs.getString("reason_code");
        row.amount = rs.getBigDecimal("amount");
        row.currency = rs.getString("currency");
        row.initiatedAt = rs.getTimestamp("initiated_at");
        row.deadline = rs.getDate("deadline");
        row.status = rs.getString("status");
        row.createdAt = rs.getTimestamp("created_at");
        row.updatedAt = rs.getTimestamp("updated_at");
        return row;
    }
}
