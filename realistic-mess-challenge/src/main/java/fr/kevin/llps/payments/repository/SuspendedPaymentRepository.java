package fr.kevin.llps.payments.repository;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import fr.kevin.llps.payments.dto.SuspendedPaymentSearchRequest;
import fr.kevin.llps.payments.model.SuspendedPaymentRow;
import fr.kevin.llps.payments.util.PaymentUtils;

@Repository
public class SuspendedPaymentRepository {

    private static final String COLUMNS = "id, amount, currency, debtor_iban, debtor_name, creditor_iban, creditor_name, hold_reason, compliance_case_ref, review_status, held_at, released_at, created_at, updated_at";

    @Autowired
    private JdbcTemplate jdbc;

    public List<SuspendedPaymentRow> search(SuspendedPaymentSearchRequest req) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ").append(COLUMNS).append(" FROM suspended_payments WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (req.debtorIban != null && !req.debtorIban.isBlank()) {
            sql.append(" AND debtor_iban = ?");
            args.add(req.debtorIban.trim());
        }
        if (req.holdReason != null && !req.holdReason.isBlank()) {
            sql.append(" AND hold_reason = ?");
            args.add(req.holdReason.trim().toUpperCase());
        }
        if (req.reviewStatus != null && !req.reviewStatus.isBlank()) {
            sql.append(" AND review_status = ?");
            args.add(req.reviewStatus.trim().toUpperCase());
        }
        if (req.heldFrom != null && !req.heldFrom.isBlank()) {
            sql.append(" AND held_at >= ?");
            args.add(PaymentUtils.parseTimestamp(req.heldFrom));
        }
        if (req.heldTo != null && !req.heldTo.isBlank()) {
            sql.append(" AND held_at <= ?");
            args.add(PaymentUtils.parseTimestamp(req.heldTo));
        }
        sql.append(" ORDER BY held_at DESC LIMIT ").append(PaymentUtils.limitOrDefault(req.limit));
        return jdbc.query(sql.toString(), args.toArray(), (rs, rowNum) -> mapRow(rs));
    }

    public SuspendedPaymentRow findById(String id) {
        try {
            return jdbc.queryForObject("SELECT " + COLUMNS + " FROM suspended_payments WHERE id = ?::uuid",
                    new Object[]{id}, (rs, rowNum) -> mapRow(rs));
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private SuspendedPaymentRow mapRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        SuspendedPaymentRow row = new SuspendedPaymentRow();
        row.id = rs.getString("id");
        row.amount = rs.getBigDecimal("amount");
        row.currency = rs.getString("currency");
        row.debtorIban = rs.getString("debtor_iban");
        row.debtorName = rs.getString("debtor_name");
        row.creditorIban = rs.getString("creditor_iban");
        row.creditorName = rs.getString("creditor_name");
        row.holdReason = rs.getString("hold_reason");
        row.complianceCaseRef = rs.getString("compliance_case_ref");
        row.reviewStatus = rs.getString("review_status");
        row.heldAt = rs.getTimestamp("held_at");
        row.releasedAt = rs.getTimestamp("released_at");
        row.createdAt = rs.getTimestamp("created_at");
        row.updatedAt = rs.getTimestamp("updated_at");
        return row;
    }
}
