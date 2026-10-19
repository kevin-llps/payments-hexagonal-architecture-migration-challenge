package fr.kevin.llps.payments.mess;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * TODO: split this class one day (ticket PAY-1337, opened 2019, still open)
 */
@Component
public class PaymentsGodService {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentsGodService.class);

    public static final Map<String, Object> QUERY_CACHE = new ConcurrentHashMap<>();
    public static final Map<String, Integer> QUERY_COUNTS = new ConcurrentHashMap<>();
    public static final List<String> RECENT_ERRORS = new ArrayList<>();

    private static final Object LOCK = new Object();
    private static final SimpleDateFormat SHARED_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final int CACHE_TTL_SECONDS = 30;
    private static final int HARD_LIMIT = 500;
    private static final int DEFAULT_LIMIT = 100;
    private static final BigDecimal FEE_FLAT = new BigDecimal("0.50");
    private static final BigDecimal FEE_PCT = new BigDecimal("0.02");
    private static final BigDecimal WARN_AMOUNT = new BigDecimal("10000");

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    // ------------------------------------------------------------------
    // SEARCH - INSTANT
    // ------------------------------------------------------------------
    public List<Map<String, Object>> searchInstantPayments(String endToEndId, String debtorIban,
            String creditorIban, String status, String currency, String executionFrom,
            String executionTo, Integer limit) {

        String cacheKey = "instant|" + endToEndId + "|" + debtorIban + "|" + creditorIban + "|"
                + status + "|" + currency + "|" + executionFrom + "|" + executionTo + "|" + limit;
        synchronized (LOCK) {
            Object cached = QUERY_CACHE.get(cacheKey);
            if (cached instanceof Object[] pair) {
                Long ts = (Long) pair[1];
                if (System.currentTimeMillis() - ts < CACHE_TTL_SECONDS * 1000L) {
                    bumpCount("searchInstantPayments");
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> hit = (List<Map<String, Object>>) pair[0];
                    return deepCopy(hit);
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("SELECT id, end_to_end_id, uetr, amount, currency, debtor_iban, debtor_name, creditor_iban, creditor_name, execution_datetime, status, scheme, created_at, updated_at FROM instant_payments WHERE 1=1");
        if (endToEndId != null && endToEndId.length() > 0 && !endToEndId.isEmpty() && !"".equals(endToEndId)) {
            sb.append(" AND end_to_end_id LIKE '%").append(sanitize(endToEndId)).append("%'");
        }
        if (debtorIban != null && debtorIban.length() > 0 && !debtorIban.isEmpty()) {
            sb.append(" AND UPPER(debtor_iban) LIKE '%").append(sanitize(debtorIban).toUpperCase()).append("%'");
        }
        if (creditorIban != null && creditorIban.length() > 0 && !creditorIban.isEmpty()) {
            sb.append(" AND UPPER(creditor_iban) LIKE '%").append(sanitize(creditorIban).toUpperCase()).append("%'");
        }
        if (status != null && status.length() > 0 && !status.isEmpty()) {
            sb.append(" AND status = '").append(sanitize(status).toUpperCase()).append("'");
        }
        if (currency != null && currency.length() > 0 && !currency.isEmpty()) {
            sb.append(" AND currency = '").append(sanitize(currency).toUpperCase()).append("'");
        }
        if (executionFrom != null && executionFrom.length() > 0 && !executionFrom.isEmpty()) {
            String from = sanitize(executionFrom).replace('T', ' ');
            if (from.endsWith("Z")) {
                from = from.substring(0, from.length() - 1);
            }
            sb.append(" AND execution_datetime >= '").append(from).append("'");
        }
        if (executionTo != null && executionTo.length() > 0 && !executionTo.isEmpty()) {
            String to = sanitize(executionTo).replace('T', ' ');
            if (to.endsWith("Z")) {
                to = to.substring(0, to.length() - 1);
            }
            sb.append(" AND execution_datetime <= '").append(to).append("'");
        }
        sb.append(" ORDER BY execution_datetime DESC NULLS LAST, created_at DESC");
        sb.append(" LIMIT ").append(effectiveLimit(limit));

        List<Map<String, Object>> rows;
        try {
            rows = jdbc.queryForList(sb.toString());
        } catch (Exception e) {
            LOG.error("searchInstantPayments failed", e);
            rememberError("searchInstantPayments", e);
            return new ArrayList<>();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> out = new HashMap<>();
            out.put("id", row.get("id"));
            out.put("endToEndId", row.get("end_to_end_id"));
            out.put("uetr", row.get("uetr"));
            out.put("amount", row.get("amount"));
            out.put("currency", row.get("currency"));
            out.put("debtorIban", row.get("debtor_iban"));
            out.put("debtorName", row.get("debtor_name"));
            out.put("creditorIban", row.get("creditor_iban"));
            out.put("creditorName", row.get("creditor_name"));
            Object dt = row.get("execution_datetime");
            if (dt != null) {
                out.put("executionDatetime", SHARED_FORMAT.format((Timestamp) dt));
            } else {
                out.put("executionDatetime", null);
            }
            out.put("status", row.get("status"));
            out.put("scheme", row.get("scheme"));
            BigDecimal amount = toBigDecimal(row.get("amount"));
            String st = row.get("status") == null ? "" : row.get("status").toString();
            if (amount != null && amount.compareTo(WARN_AMOUNT) > 0) {
                if ("PENDING".equals(st)) {
                    out.put("warning", "HIGH_AMOUNT_PENDING");
                } else {
                    out.put("warning", "HIGH_AMOUNT");
                }
            } else {
                out.put("warning", null);
            }
            if ("COMPLETED".equals(st)) {
                out.put("actionable", Boolean.FALSE);
            } else if ("REJECTED".equals(st)) {
                out.put("actionable", Boolean.FALSE);
            } else {
                out.put("actionable", Boolean.TRUE);
            }
            result.add(out);
        }

        synchronized (LOCK) {
            QUERY_CACHE.put(cacheKey, new Object[]{result, System.currentTimeMillis()});
            if (QUERY_CACHE.size() > 200) {
                QUERY_CACHE.clear();
            }
        }
        bumpCount("searchInstantPayments");
        return result;
    }

    // ------------------------------------------------------------------
    // SEARCH - SUSPENDED
    // ------------------------------------------------------------------
    public List<Map<String, Object>> searchSuspendedPayments(String debtorIban, String holdReason,
            String reviewStatus, String heldFrom, String heldTo, Integer limit) {

        String cacheKey = "suspended|" + debtorIban + "|" + holdReason + "|" + reviewStatus + "|"
                + heldFrom + "|" + heldTo + "|" + limit;
        synchronized (LOCK) {
            Object cached = QUERY_CACHE.get(cacheKey);
            if (cached instanceof Object[] pair) {
                Long ts = (Long) pair[1];
                if (System.currentTimeMillis() - ts < CACHE_TTL_SECONDS * 1000L) {
                    bumpCount("searchSuspendedPayments");
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> hit = (List<Map<String, Object>>) pair[0];
                    return deepCopy(hit);
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("SELECT id, amount, currency, debtor_iban, debtor_name, creditor_iban, creditor_name, hold_reason, compliance_case_ref, review_status, held_at, released_at, created_at, updated_at FROM suspended_payments WHERE 1=1");
        if (debtorIban != null && debtorIban.length() > 0 && !debtorIban.isEmpty()) {
            sb.append(" AND debtor_iban = '").append(sanitize(debtorIban)).append("'");
        }
        if (holdReason != null && holdReason.length() > 0 && !holdReason.isEmpty()) {
            sb.append(" AND hold_reason = '").append(sanitize(holdReason).toUpperCase()).append("'");
        }
        if (reviewStatus != null && reviewStatus.length() > 0 && !reviewStatus.isEmpty()) {
            sb.append(" AND review_status = '").append(sanitize(reviewStatus).toUpperCase()).append("'");
        }
        if (heldFrom != null && heldFrom.length() > 0 && !heldFrom.isEmpty()) {
            String from = sanitize(heldFrom).replace('T', ' ');
            if (from.endsWith("Z")) {
                from = from.substring(0, from.length() - 1);
            }
            sb.append(" AND held_at >= '").append(from).append("'");
        }
        if (heldTo != null && heldTo.length() > 0 && !heldTo.isEmpty()) {
            String to = sanitize(heldTo).replace('T', ' ');
            if (to.endsWith("Z")) {
                to = to.substring(0, to.length() - 1);
            }
            sb.append(" AND held_at <= '").append(to).append("'");
        }
        sb.append(" ORDER BY held_at DESC NULLS LAST");
        sb.append(" LIMIT ").append(effectiveLimit(limit));

        List<Map<String, Object>> rows;
        try {
            rows = jdbc.queryForList(sb.toString());
        } catch (Exception e) {
            LOG.error("searchSuspendedPayments failed", e);
            rememberError("searchSuspendedPayments", e);
            return new ArrayList<>();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> out = new HashMap<>();
            out.put("id", row.get("id"));
            out.put("amount", row.get("amount"));
            out.put("currency", row.get("currency"));
            out.put("debtorIban", row.get("debtor_iban"));
            out.put("debtorName", row.get("debtor_name"));
            out.put("creditorIban", row.get("creditor_iban"));
            out.put("creditorName", row.get("creditor_name"));
            out.put("holdReason", row.get("hold_reason"));
            out.put("complianceCaseRef", row.get("compliance_case_ref"));
            out.put("reviewStatus", row.get("review_status"));
            Object held = row.get("held_at");
            if (held != null) {
                out.put("heldAt", SHARED_FORMAT.format((Timestamp) held));
            } else {
                out.put("heldAt", null);
            }
            Object released = row.get("released_at");
            if (released != null) {
                out.put("releasedAt", SHARED_FORMAT.format((Timestamp) released));
            } else {
                out.put("releasedAt", null);
            }
            String review = row.get("review_status") == null ? "" : row.get("review_status").toString();
            if ("PENDING".equals(review)) {
                if (held != null) {
                    long ageHours = (System.currentTimeMillis() - ((Timestamp) held).getTime()) / 3600000L;
                    out.put("ageHours", ageHours);
                    if (ageHours > 48) {
                        out.put("slaBreached", Boolean.TRUE);
                    } else {
                        out.put("slaBreached", Boolean.FALSE);
                    }
                } else {
                    out.put("ageHours", null);
                    out.put("slaBreached", Boolean.FALSE);
                }
                out.put("actionable", Boolean.TRUE);
            } else {
                out.put("ageHours", null);
                out.put("slaBreached", Boolean.FALSE);
                out.put("actionable", Boolean.FALSE);
            }
            result.add(out);
        }

        synchronized (LOCK) {
            QUERY_CACHE.put(cacheKey, new Object[]{result, System.currentTimeMillis()});
            if (QUERY_CACHE.size() > 200) {
                QUERY_CACHE.clear();
            }
        }
        bumpCount("searchSuspendedPayments");
        return result;
    }

    // ------------------------------------------------------------------
    // SEARCH - INBOUND
    // ------------------------------------------------------------------
    public List<Map<String, Object>> searchInboundPayments(String creditorIban, String originBic,
            String status, String currency, String valueDateFrom, String valueDateTo, Integer limit) {

        String cacheKey = "inbound|" + creditorIban + "|" + originBic + "|" + status + "|"
                + currency + "|" + valueDateFrom + "|" + valueDateTo + "|" + limit;
        synchronized (LOCK) {
            Object cached = QUERY_CACHE.get(cacheKey);
            if (cached instanceof Object[] pair) {
                Long ts = (Long) pair[1];
                if (System.currentTimeMillis() - ts < CACHE_TTL_SECONDS * 1000L) {
                    bumpCount("searchInboundPayments");
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> hit = (List<Map<String, Object>>) pair[0];
                    return deepCopy(hit);
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("SELECT id, credit_transfer_ref, amount, currency, creditor_iban, creditor_name, origin_bic, value_date, settlement_date, status, created_at, updated_at FROM inbound_payments WHERE 1=1");
        if (creditorIban != null && creditorIban.length() > 0 && !creditorIban.isEmpty()) {
            sb.append(" AND creditor_iban = '").append(sanitize(creditorIban)).append("'");
        }
        if (originBic != null && originBic.length() > 0 && !originBic.isEmpty()) {
            sb.append(" AND UPPER(origin_bic) LIKE '%").append(sanitize(originBic).toUpperCase()).append("%'");
        }
        if (status != null && status.length() > 0 && !status.isEmpty()) {
            sb.append(" AND status = '").append(sanitize(status).toUpperCase()).append("'");
        }
        if (currency != null && currency.length() > 0 && !currency.isEmpty()) {
            sb.append(" AND currency = '").append(sanitize(currency).toUpperCase()).append("'");
        }
        if (valueDateFrom != null && valueDateFrom.length() > 0 && !valueDateFrom.isEmpty()) {
            sb.append(" AND value_date >= '").append(sanitize(valueDateFrom).substring(0, Math.min(10, sanitize(valueDateFrom).length()))).append("'");
        }
        if (valueDateTo != null && valueDateTo.length() > 0 && !valueDateTo.isEmpty()) {
            sb.append(" AND value_date <= '").append(sanitize(valueDateTo).substring(0, Math.min(10, sanitize(valueDateTo).length()))).append("'");
        }
        sb.append(" ORDER BY value_date DESC NULLS LAST, created_at DESC");
        sb.append(" LIMIT ").append(effectiveLimit(limit));

        List<Map<String, Object>> rows;
        try {
            rows = jdbc.queryForList(sb.toString());
        } catch (Exception e) {
            LOG.error("searchInboundPayments failed", e);
            rememberError("searchInboundPayments", e);
            return new ArrayList<>();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> out = new HashMap<>();
            out.put("id", row.get("id"));
            out.put("creditTransferRef", row.get("credit_transfer_ref"));
            out.put("amount", row.get("amount"));
            out.put("currency", row.get("currency"));
            out.put("creditorIban", row.get("creditor_iban"));
            out.put("creditorName", row.get("creditor_name"));
            out.put("originBic", row.get("origin_bic"));
            Object vd = row.get("value_date");
            if (vd != null) {
                out.put("valueDate", vd.toString().substring(0, Math.min(10, vd.toString().length())));
                try {
                    java.sql.Date valueDate = java.sql.Date.valueOf(vd.toString().substring(0, 10));
                    long ageDays = (System.currentTimeMillis() - valueDate.getTime()) / 86400000L;
                    out.put("ageInDays", ageDays);
                    if (ageDays > 3 && "RECEIVED".equals(String.valueOf(row.get("status")))) {
                        out.put("stale", Boolean.TRUE);
                    } else {
                        out.put("stale", Boolean.FALSE);
                    }
                } catch (Exception parseProblem) {
                    // date could not be parsed for whatever reason, skip the computed fields
                    out.put("ageInDays", null);
                    out.put("stale", Boolean.FALSE);
                }
            } else {
                out.put("valueDate", null);
                out.put("ageInDays", null);
                out.put("stale", Boolean.FALSE);
            }
            Object sd = row.get("settlement_date");
            if (sd != null) {
                out.put("settlementDate", sd.toString().substring(0, Math.min(10, sd.toString().length())));
            } else {
                out.put("settlementDate", null);
            }
            out.put("status", row.get("status"));
            result.add(out);
        }

        synchronized (LOCK) {
            QUERY_CACHE.put(cacheKey, new Object[]{result, System.currentTimeMillis()});
            if (QUERY_CACHE.size() > 200) {
                QUERY_CACHE.clear();
            }
        }
        bumpCount("searchInboundPayments");
        return result;
    }

    // ------------------------------------------------------------------
    // SEARCH - OUTBOUND
    // ------------------------------------------------------------------
    public List<Map<String, Object>> searchOutboundPayments(String debtorIban, String destinationBic,
            String chargesType, String status, String executionFrom, String executionTo, Integer limit) {

        String cacheKey = "outbound|" + debtorIban + "|" + destinationBic + "|" + chargesType + "|"
                + status + "|" + executionFrom + "|" + executionTo + "|" + limit;
        synchronized (LOCK) {
            Object cached = QUERY_CACHE.get(cacheKey);
            if (cached instanceof Object[] pair) {
                Long ts = (Long) pair[1];
                if (System.currentTimeMillis() - ts < CACHE_TTL_SECONDS * 1000L) {
                    bumpCount("searchOutboundPayments");
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> hit = (List<Map<String, Object>>) pair[0];
                    return deepCopy(hit);
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("SELECT id, instruction_id, amount, currency, debtor_iban, debtor_name, destination_bic, requested_execution_date, charges_type, status, created_at, updated_at FROM outbound_payments WHERE 1=1");
        if (debtorIban != null && debtorIban.length() > 0 && !debtorIban.isEmpty()) {
            sb.append(" AND debtor_iban = '").append(sanitize(debtorIban)).append("'");
        }
        if (destinationBic != null && destinationBic.length() > 0 && !destinationBic.isEmpty()) {
            sb.append(" AND destination_bic LIKE '%").append(sanitize(destinationBic).toUpperCase()).append("%'");
        }
        if (chargesType != null && chargesType.length() > 0 && !chargesType.isEmpty()) {
            sb.append(" AND charges_type = '").append(sanitize(chargesType).toUpperCase()).append("'");
        }
        if (status != null && status.length() > 0 && !status.isEmpty()) {
            sb.append(" AND status = '").append(sanitize(status).toUpperCase()).append("'");
        }
        if (executionFrom != null && executionFrom.length() > 0 && !executionFrom.isEmpty()) {
            sb.append(" AND requested_execution_date >= '").append(sanitize(executionFrom).substring(0, Math.min(10, sanitize(executionFrom).length()))).append("'");
        }
        if (executionTo != null && executionTo.length() > 0 && !executionTo.isEmpty()) {
            sb.append(" AND requested_execution_date <= '").append(sanitize(executionTo).substring(0, Math.min(10, sanitize(executionTo).length()))).append("'");
        }
        sb.append(" ORDER BY requested_execution_date DESC NULLS LAST, created_at DESC");
        sb.append(" LIMIT ").append(effectiveLimit(limit));

        List<Map<String, Object>> rows;
        try {
            rows = jdbc.queryForList(sb.toString());
        } catch (Exception e) {
            LOG.error("searchOutboundPayments failed", e);
            rememberError("searchOutboundPayments", e);
            return new ArrayList<>();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> out = new HashMap<>();
            out.put("id", row.get("id"));
            out.put("instructionId", row.get("instruction_id"));
            out.put("amount", row.get("amount"));
            out.put("currency", row.get("currency"));
            out.put("debtorIban", row.get("debtor_iban"));
            out.put("debtorName", row.get("debtor_name"));
            out.put("destinationBic", row.get("destination_bic"));
            Object red = row.get("requested_execution_date");
            if (red != null) {
                out.put("requestedExecutionDate", red.toString().substring(0, Math.min(10, red.toString().length())));
            } else {
                out.put("requestedExecutionDate", null);
            }
            out.put("chargesType", row.get("charges_type"));
            out.put("status", row.get("status"));
            BigDecimal amount = toBigDecimal(row.get("amount"));
            String ct = row.get("charges_type") == null ? "" : row.get("charges_type").toString();
            BigDecimal fee;
            if (amount == null) {
                fee = BigDecimal.ZERO;
            } else if ("OUR".equals(ct)) {
                fee = amount.multiply(FEE_PCT).setScale(2, RoundingMode.HALF_UP);
            } else if ("SHA".equals(ct)) {
                fee = FEE_FLAT;
            } else {
                fee = BigDecimal.ZERO;
            }
            out.put("estimatedFee", fee);
            String st = row.get("status") == null ? "" : row.get("status").toString();
            if ("REJECTED".equals(st)) {
                out.put("actionable", Boolean.FALSE);
                out.put("action", "CHECK_REJECT_REASON");
            } else if ("ACCEPTED".equals(st)) {
                out.put("actionable", Boolean.FALSE);
                out.put("action", null);
            } else if ("SENT".equals(st)) {
                out.put("actionable", Boolean.TRUE);
                out.put("action", "MONITOR");
            } else {
                out.put("actionable", Boolean.TRUE);
                out.put("action", "SEND");
            }
            result.add(out);
        }

        synchronized (LOCK) {
            QUERY_CACHE.put(cacheKey, new Object[]{result, System.currentTimeMillis()});
            if (QUERY_CACHE.size() > 200) {
                QUERY_CACHE.clear();
            }
        }
        bumpCount("searchOutboundPayments");
        return result;
    }

    // ------------------------------------------------------------------
    // SEARCH - RECALLS
    // ------------------------------------------------------------------
    public List<Map<String, Object>> searchRecalls(String recallType, String originalPaymentId,
            String originalPaymentType, String reasonCode, String status, String initiatedFrom,
            String initiatedTo, Integer limit) {

        String cacheKey = "recall|" + recallType + "|" + originalPaymentId + "|" + originalPaymentType
                + "|" + reasonCode + "|" + status + "|" + initiatedFrom + "|" + initiatedTo + "|" + limit;
        synchronized (LOCK) {
            Object cached = QUERY_CACHE.get(cacheKey);
            if (cached instanceof Object[] pair) {
                Long ts = (Long) pair[1];
                if (System.currentTimeMillis() - ts < CACHE_TTL_SECONDS * 1000L) {
                    bumpCount("searchRecalls");
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> hit = (List<Map<String, Object>>) pair[0];
                    return deepCopy(hit);
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("SELECT id, recall_reference, recall_type, original_payment_id, original_payment_type, reason_code, amount, currency, initiated_at, deadline, status, created_at, updated_at FROM recalls WHERE 1=1");
        if (recallType != null && recallType.length() > 0 && !recallType.isEmpty()) {
            sb.append(" AND recall_type = '").append(sanitize(recallType).toUpperCase()).append("'");
        }
        if (originalPaymentId != null && originalPaymentId.length() > 0 && !originalPaymentId.isEmpty()) {
            sb.append(" AND original_payment_id = '").append(sanitize(originalPaymentId)).append("'");
        }
        if (originalPaymentType != null && originalPaymentType.length() > 0 && !originalPaymentType.isEmpty()) {
            sb.append(" AND original_payment_type = '").append(sanitize(originalPaymentType).toUpperCase()).append("'");
        }
        if (reasonCode != null && reasonCode.length() > 0 && !reasonCode.isEmpty()) {
            sb.append(" AND reason_code = '").append(sanitize(reasonCode).toUpperCase()).append("'");
        }
        if (status != null && status.length() > 0 && !status.isEmpty()) {
            sb.append(" AND status = '").append(sanitize(status).toUpperCase()).append("'");
        }
        if (initiatedFrom != null && initiatedFrom.length() > 0 && !initiatedFrom.isEmpty()) {
            String from = sanitize(initiatedFrom).replace('T', ' ');
            if (from.endsWith("Z")) {
                from = from.substring(0, from.length() - 1);
            }
            sb.append(" AND initiated_at >= '").append(from).append("'");
        }
        if (initiatedTo != null && initiatedTo.length() > 0 && !initiatedTo.isEmpty()) {
            String to = sanitize(initiatedTo).replace('T', ' ');
            if (to.endsWith("Z")) {
                to = to.substring(0, to.length() - 1);
            }
            sb.append(" AND initiated_at <= '").append(to).append("'");
        }
        sb.append(" ORDER BY initiated_at DESC NULLS LAST");
        sb.append(" LIMIT ").append(effectiveLimit(limit));

        List<Map<String, Object>> rows;
        try {
            rows = jdbc.queryForList(sb.toString());
        } catch (Exception e) {
            LOG.error("searchRecalls failed", e);
            rememberError("searchRecalls", e);
            return new ArrayList<>();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> out = new HashMap<>();
            out.put("id", row.get("id"));
            out.put("recallReference", row.get("recall_reference"));
            out.put("recallType", row.get("recall_type"));
            out.put("originalPaymentId", row.get("original_payment_id"));
            out.put("originalPaymentType", row.get("original_payment_type"));
            out.put("reasonCode", row.get("reason_code"));
            out.put("amount", row.get("amount"));
            out.put("currency", row.get("currency"));
            Object init = row.get("initiated_at");
            if (init != null) {
                out.put("initiatedAt", SHARED_FORMAT.format((Timestamp) init));
            } else {
                out.put("initiatedAt", null);
            }
            Object dl = row.get("deadline");
            if (dl != null) {
                String deadline = dl.toString().substring(0, Math.min(10, dl.toString().length()));
                out.put("deadline", deadline);
                try {
                    java.sql.Date deadlineDate = java.sql.Date.valueOf(deadline);
                    long daysLeft = (deadlineDate.getTime() - System.currentTimeMillis()) / 86400000L;
                    out.put("daysLeft", daysLeft);
                    if (daysLeft < 0 && !"COMPLETED".equals(String.valueOf(row.get("status")))) {
                        out.put("overdue", Boolean.TRUE);
                    } else {
                        out.put("overdue", Boolean.FALSE);
                    }
                } catch (Exception e) {
                    out.put("daysLeft", null);
                    out.put("overdue", Boolean.FALSE);
                }
            } else {
                out.put("deadline", null);
                out.put("daysLeft", null);
                out.put("overdue", Boolean.FALSE);
            }
            out.put("status", row.get("status"));
            result.add(out);
        }

        synchronized (LOCK) {
            QUERY_CACHE.put(cacheKey, new Object[]{result, System.currentTimeMillis()});
            if (QUERY_CACHE.size() > 200) {
                QUERY_CACHE.clear();
            }
        }
        bumpCount("searchRecalls");
        return result;
    }

    // ------------------------------------------------------------------
    // DETAILS - one giant switch, five copy-pasted branches
    // ------------------------------------------------------------------
    public Map<String, Object> getPaymentDetails(String type, String id) {
        if (type == null || id == null) {
            return null;
        }
        String normalized = type.trim().toLowerCase();
        if ("instant".equals(normalized) || "instant-payment".equals(normalized) || "sct-inst".equals(normalized)) {
            try {
                Map<String, Object> row = jdbc.queryForMap(
                        "SELECT id, end_to_end_id, uetr, amount, currency, debtor_iban, debtor_name, creditor_iban, creditor_name, execution_datetime, status, scheme, created_at, updated_at FROM instant_payments WHERE id = ?::uuid", id);
                Map<String, Object> out = new HashMap<>();
                out.put("type", "INSTANT");
                out.put("id", row.get("id"));
                out.put("endToEndId", row.get("end_to_end_id"));
                out.put("uetr", row.get("uetr"));
                out.put("amount", row.get("amount"));
                out.put("currency", row.get("currency"));
                out.put("debtorIban", row.get("debtor_iban"));
                out.put("debtorName", row.get("debtor_name"));
                out.put("creditorIban", row.get("creditor_iban"));
                out.put("creditorName", row.get("creditor_name"));
                Object dt = row.get("execution_datetime");
                out.put("executionDatetime", dt == null ? null : SHARED_FORMAT.format((Timestamp) dt));
                out.put("status", row.get("status"));
                out.put("scheme", row.get("scheme"));
                Object createdAt = row.get("created_at");
                out.put("createdAt", createdAt == null ? null : SHARED_FORMAT.format((Timestamp) createdAt));
                return out;
            } catch (EmptyResultDataAccessException e) {
                return null;
            } catch (Exception e) {
                rememberError("getPaymentDetails/instant", e);
                throw new RuntimeException("could not load instant payment", e);
            }
        } else if ("suspended".equals(normalized) || "suspended-payment".equals(normalized)) {
            try {
                Map<String, Object> row = jdbc.queryForMap(
                        "SELECT id, amount, currency, debtor_iban, debtor_name, creditor_iban, creditor_name, hold_reason, compliance_case_ref, review_status, held_at, released_at, created_at, updated_at FROM suspended_payments WHERE id = ?::uuid", id);
                Map<String, Object> out = new HashMap<>();
                out.put("type", "SUSPENDED");
                out.put("id", row.get("id"));
                out.put("amount", row.get("amount"));
                out.put("currency", row.get("currency"));
                out.put("debtorIban", row.get("debtor_iban"));
                out.put("debtorName", row.get("debtor_name"));
                out.put("creditorIban", row.get("creditor_iban"));
                out.put("creditorName", row.get("creditor_name"));
                out.put("holdReason", row.get("hold_reason"));
                out.put("complianceCaseRef", row.get("compliance_case_ref"));
                out.put("reviewStatus", row.get("review_status"));
                Object held = row.get("held_at");
                out.put("heldAt", held == null ? null : SHARED_FORMAT.format((Timestamp) held));
                Object released = row.get("released_at");
                out.put("releasedAt", released == null ? null : SHARED_FORMAT.format((Timestamp) released));
                return out;
            } catch (EmptyResultDataAccessException e) {
                return null;
            } catch (Exception e) {
                rememberError("getPaymentDetails/suspended", e);
                throw new RuntimeException("could not load suspended payment", e);
            }
        } else if ("inbound".equals(normalized) || "inbound-payment".equals(normalized)) {
            try {
                Map<String, Object> row = jdbc.queryForMap(
                        "SELECT id, credit_transfer_ref, amount, currency, creditor_iban, creditor_name, origin_bic, value_date, settlement_date, status, created_at, updated_at FROM inbound_payments WHERE id = ?::uuid", id);
                Map<String, Object> out = new HashMap<>();
                out.put("type", "INBOUND");
                out.put("id", row.get("id"));
                out.put("creditTransferRef", row.get("credit_transfer_ref"));
                out.put("amount", row.get("amount"));
                out.put("currency", row.get("currency"));
                out.put("creditorIban", row.get("creditor_iban"));
                out.put("creditorName", row.get("creditor_name"));
                out.put("originBic", row.get("origin_bic"));
                Object vd = row.get("value_date");
                out.put("valueDate", vd == null ? null : vd.toString().substring(0, Math.min(10, vd.toString().length())));
                Object sd = row.get("settlement_date");
                out.put("settlementDate", sd == null ? null : sd.toString().substring(0, Math.min(10, sd.toString().length())));
                out.put("status", row.get("status"));
                return out;
            } catch (EmptyResultDataAccessException e) {
                return null;
            } catch (Exception e) {
                rememberError("getPaymentDetails/inbound", e);
                throw new RuntimeException("could not load inbound payment", e);
            }
        } else if ("outbound".equals(normalized) || "outbound-payment".equals(normalized)) {
            try {
                Map<String, Object> row = jdbc.queryForMap(
                        "SELECT id, instruction_id, amount, currency, debtor_iban, debtor_name, destination_bic, requested_execution_date, charges_type, status, created_at, updated_at FROM outbound_payments WHERE id = ?::uuid", id);
                Map<String, Object> out = new HashMap<>();
                out.put("type", "OUTBOUND");
                out.put("id", row.get("id"));
                out.put("instructionId", row.get("instruction_id"));
                out.put("amount", row.get("amount"));
                out.put("currency", row.get("currency"));
                out.put("debtorIban", row.get("debtor_iban"));
                out.put("debtorName", row.get("debtor_name"));
                out.put("destinationBic", row.get("destination_bic"));
                Object red = row.get("requested_execution_date");
                out.put("requestedExecutionDate", red == null ? null : red.toString().substring(0, Math.min(10, red.toString().length())));
                out.put("chargesType", row.get("charges_type"));
                out.put("status", row.get("status"));
                BigDecimal amount = toBigDecimal(row.get("amount"));
                String ct = row.get("charges_type") == null ? "" : row.get("charges_type").toString();
                if (amount != null && "OUR".equals(ct)) {
                    out.put("estimatedFee", amount.multiply(FEE_PCT).setScale(2, RoundingMode.HALF_UP));
                } else if ("SHA".equals(ct)) {
                    out.put("estimatedFee", FEE_FLAT);
                } else {
                    out.put("estimatedFee", BigDecimal.ZERO);
                }
                return out;
            } catch (EmptyResultDataAccessException e) {
                return null;
            } catch (Exception e) {
                rememberError("getPaymentDetails/outbound", e);
                throw new RuntimeException("could not load outbound payment", e);
            }
        } else if ("recall".equals(normalized) || "recalls".equals(normalized)) {
            try {
                Map<String, Object> row = jdbc.queryForMap(
                        "SELECT id, recall_reference, recall_type, original_payment_id, original_payment_type, reason_code, amount, currency, initiated_at, deadline, status, created_at, updated_at FROM recalls WHERE id = ?::uuid", id);
                Map<String, Object> out = new HashMap<>();
                out.put("type", "RECALL");
                out.put("id", row.get("id"));
                out.put("recallReference", row.get("recall_reference"));
                out.put("recallType", row.get("recall_type"));
                out.put("originalPaymentId", row.get("original_payment_id"));
                out.put("originalPaymentType", row.get("original_payment_type"));
                out.put("reasonCode", row.get("reason_code"));
                out.put("amount", row.get("amount"));
                out.put("currency", row.get("currency"));
                Object init = row.get("initiated_at");
                out.put("initiatedAt", init == null ? null : SHARED_FORMAT.format((Timestamp) init));
                Object dl = row.get("deadline");
                out.put("deadline", dl == null ? null : dl.toString().substring(0, Math.min(10, dl.toString().length())));
                out.put("status", row.get("status"));
                return out;
            } catch (EmptyResultDataAccessException e) {
                return null;
            } catch (Exception e) {
                rememberError("getPaymentDetails/recall", e);
                throw new RuntimeException("could not load recall", e);
            }
        } else {
            throw new IllegalArgumentException("unknown payment type: " + type);
        }
    }

    // ------------------------------------------------------------------
    // INGESTION - one giant if/else, five copy-pasted blocks
    // ------------------------------------------------------------------
    public void ingest(String topic, String raw) {
        try {
            if ("pay.instant".equals(topic)) {
                JsonNode node = mapper.readTree(raw);
                String id = node.path("id").asText(UUID.randomUUID().toString());
                String sql = "INSERT INTO instant_payments (id, end_to_end_id, uetr, amount, currency, debtor_iban, debtor_name, creditor_iban, creditor_name, execution_datetime, status, scheme, created_at, updated_at) VALUES ('"
                        + id + "', '" + node.path("endToEndId").asText() + "', '"
                        + node.path("uetr").asText() + "', " + node.path("amount").decimalValue() + ", '"
                        + node.path("currency").asText() + "', '" + node.path("debtorIban").asText() + "', '"
                        + node.path("debtorName").asText() + "', '" + node.path("creditorIban").asText() + "', '"
                        + node.path("creditorName").asText() + "', " + ts(node.path("executionDateTime").asText()) + ", '"
                        + node.path("status").asText() + "', '" + node.path("scheme").asText("SCT INST") + "', now(), now()) "
                        + "ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status, updated_at = now()";
                jdbc.execute(sql);
            } else if ("pay.suspended".equals(topic)) {
                JsonNode node = mapper.readTree(raw);
                String id = node.path("id").asText(UUID.randomUUID().toString());
                String sql = "INSERT INTO suspended_payments (id, amount, currency, debtor_iban, debtor_name, creditor_iban, creditor_name, hold_reason, compliance_case_ref, review_status, held_at, released_at, created_at, updated_at) VALUES ('"
                        + id + "', " + node.path("amount").decimalValue() + ", '"
                        + node.path("currency").asText() + "', '" + node.path("debtorIban").asText() + "', '"
                        + node.path("debtorName").asText() + "', '" + node.path("creditorIban").asText() + "', '"
                        + node.path("creditorName").asText() + "', '" + node.path("holdReason").asText() + "', '"
                        + node.path("complianceCaseRef").asText() + "', '" + node.path("reviewStatus").asText() + "', "
                        + ts(node.path("heldAt").asText()) + ", " + ts(node.path("releasedAt").asText()) + ", now(), now()) "
                        + "ON CONFLICT (id) DO UPDATE SET review_status = EXCLUDED.review_status, released_at = EXCLUDED.released_at, updated_at = now()";
                jdbc.execute(sql);
            } else if ("pay.inbound".equals(topic)) {
                JsonNode node = mapper.readTree(raw);
                String id = node.path("id").asText(UUID.randomUUID().toString());
                String sql = "INSERT INTO inbound_payments (id, credit_transfer_ref, amount, currency, creditor_iban, creditor_name, origin_bic, value_date, settlement_date, status, created_at, updated_at) VALUES ('"
                        + id + "', '" + node.path("creditTransferRef").asText() + "', "
                        + node.path("amount").decimalValue() + ", '" + node.path("currency").asText() + "', '"
                        + node.path("creditorIban").asText() + "', '" + node.path("creditorName").asText() + "', '"
                        + node.path("originBic").asText() + "', " + dt(node.path("valueDate").asText()) + ", "
                        + dt(node.path("settlementDate").asText()) + ", '" + node.path("status").asText() + "', now(), now()) "
                        + "ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status, settlement_date = EXCLUDED.settlement_date, updated_at = now()";
                jdbc.execute(sql);
            } else if ("pay.outbound".equals(topic)) {
                JsonNode node = mapper.readTree(raw);
                String id = node.path("id").asText(UUID.randomUUID().toString());
                String sql = "INSERT INTO outbound_payments (id, instruction_id, amount, currency, debtor_iban, debtor_name, destination_bic, requested_execution_date, charges_type, status, created_at, updated_at) VALUES ('"
                        + id + "', '" + node.path("instructionId").asText() + "', "
                        + node.path("amount").decimalValue() + ", '" + node.path("currency").asText() + "', '"
                        + node.path("debtorIban").asText() + "', '" + node.path("debtorName").asText() + "', '"
                        + node.path("destinationBic").asText() + "', " + dt(node.path("requestedExecutionDate").asText()) + ", '"
                        + node.path("chargesType").asText() + "', '" + node.path("status").asText() + "', now(), now()) "
                        + "ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status, updated_at = now()";
                jdbc.execute(sql);
            } else if ("pay.recall".equals(topic)) {
                JsonNode node = mapper.readTree(raw);
                String id = node.path("id").asText(UUID.randomUUID().toString());
                String sql = "INSERT INTO recalls (id, recall_reference, recall_type, original_payment_id, original_payment_type, reason_code, amount, currency, initiated_at, deadline, status, created_at, updated_at) VALUES ('"
                        + id + "', '" + node.path("recallReference").asText() + "', '"
                        + node.path("recallType").asText() + "', '" + node.path("originalPaymentId").asText() + "', '"
                        + node.path("originalPaymentType").asText() + "', '" + node.path("reasonCode").asText() + "', "
                        + node.path("amount").decimalValue() + ", '" + node.path("currency").asText() + "', "
                        + ts(node.path("initiatedAt").asText()) + ", " + dt(node.path("deadline").asText()) + ", '"
                        + node.path("status").asText() + "', now(), now()) "
                        + "ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status, deadline = EXCLUDED.deadline, updated_at = now()";
                jdbc.execute(sql);
            } else {
                LOG.warn("unknown topic {}, dropping message", topic);
            }
        } catch (Exception e) {
            // one of the services downstream said we should never fail the consumer
            LOG.warn("ingest failed for topic {} : {}", topic, e.getMessage());
            rememberError("ingest/" + topic, e);
        }
    }

    // ------------------------------------------------------------------
    // LEGACY EXPORT - kept "just in case", nobody calls it from prod anymore
    // ------------------------------------------------------------------
    public String exportLegacyCsv(String type) {
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            connection = dataSource.getConnection();
            statement = connection.createStatement();
            String table = "instant_payments";
            if ("suspended".equalsIgnoreCase(type)) {
                table = "suspended_payments";
            } else if ("inbound".equalsIgnoreCase(type)) {
                table = "inbound_payments";
            } else if ("outbound".equalsIgnoreCase(type)) {
                table = "outbound_payments";
            } else if ("recall".equalsIgnoreCase(type)) {
                table = "recalls";
            }
            resultSet = statement.executeQuery("SELECT * FROM " + table + " LIMIT 1000");
            StringBuilder csv = new StringBuilder();
            int columns = resultSet.getMetaData().getColumnCount();
            for (int i = 1; i <= columns; i++) {
                csv.append(resultSet.getMetaData().getColumnName(i)).append(";");
            }
            csv.append("\n");
            while (resultSet.next()) {
                for (int i = 1; i <= columns; i++) {
                    try {
                        csv.append(resultSet.getString(i)).append(";");
                    } catch (Exception ignore) {
                        // some drivers choke on exotic types, skip the cell
                    }
                }
                csv.append("\n");
            }
            return csv.toString();
        } catch (Exception e) {
            rememberError("exportLegacyCsv", e);
            return "ERROR";
            // NOTE: connection/statement/resultset intentionally not closed here,
            // "the pool takes care of it" (it does not)
        }
    }

    public Map<String, Object> getInternalStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("queryCounts", new HashMap<>(QUERY_COUNTS));
        stats.put("cacheSize", QUERY_CACHE.size());
        stats.put("recentErrors", new ArrayList<>(RECENT_ERRORS));
        return stats;
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------
    private String sanitize(String value) {
        if (value == null) {
            return "";
        }
        // "security": strip single quotes so nobody can break out of the SQL literal
        return value.replace("'", "").replace(";", "").trim();
    }

    private int effectiveLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        if (limit > HARD_LIMIT) {
            return HARD_LIMIT;
        }
        return limit;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        try {
            return new BigDecimal(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private String ts(String value) {
        if (value == null || value.isBlank() || "null".equals(value)) {
            return "null";
        }
        return "'" + value.replace("T", " ").replace("Z", "") + "'";
    }

    private String dt(String value) {
        if (value == null || value.isBlank() || "null".equals(value)) {
            return "null";
        }
        return "'" + value.substring(0, Math.min(10, value.length())) + "'";
    }

    private void bumpCount(String method) {
        synchronized (LOCK) {
            QUERY_COUNTS.merge(method, 1, Integer::sum);
        }
    }

    private void rememberError(String where, Exception e) {
        synchronized (RECENT_ERRORS) {
            if (RECENT_ERRORS.size() > 50) {
                RECENT_ERRORS.remove(0);
            }
            RECENT_ERRORS.add(where + " : " + e.getClass().getSimpleName() + " : " + new Date());
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> deepCopy(List<Map<String, Object>> source) {
        // "deep" copy: new list, same maps. good enough (famous last words)
        return new ArrayList<>(source);
    }
}
