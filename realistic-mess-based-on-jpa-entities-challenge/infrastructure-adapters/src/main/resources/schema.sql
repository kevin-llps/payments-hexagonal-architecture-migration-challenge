CREATE TABLE IF NOT EXISTS instant_payments (
    id                  UUID PRIMARY KEY,
    end_to_end_id       VARCHAR(35),
    uetr                VARCHAR(36),
    amount              NUMERIC(18, 2) NOT NULL,
    currency            VARCHAR(3) NOT NULL,
    debtor_iban         VARCHAR(34),
    debtor_name         VARCHAR(140),
    creditor_iban       VARCHAR(34),
    creditor_name       VARCHAR(140),
    execution_datetime  TIMESTAMP,
    status              VARCHAR(20),
    scheme              VARCHAR(20),
    created_at          TIMESTAMP,
    updated_at          TIMESTAMP
);

CREATE TABLE IF NOT EXISTS suspended_payments (
    id                   UUID PRIMARY KEY,
    amount               NUMERIC(18, 2) NOT NULL,
    currency             VARCHAR(3) NOT NULL,
    debtor_iban          VARCHAR(34),
    debtor_name          VARCHAR(140),
    creditor_iban        VARCHAR(34),
    creditor_name        VARCHAR(140),
    hold_reason          VARCHAR(50),
    compliance_case_ref  VARCHAR(30),
    review_status        VARCHAR(20),
    held_at              TIMESTAMP,
    released_at          TIMESTAMP,
    created_at           TIMESTAMP,
    updated_at           TIMESTAMP
);

CREATE TABLE IF NOT EXISTS inbound_payments (
    id                  UUID PRIMARY KEY,
    credit_transfer_ref VARCHAR(35),
    amount              NUMERIC(18, 2) NOT NULL,
    currency            VARCHAR(3) NOT NULL,
    creditor_iban       VARCHAR(34),
    creditor_name       VARCHAR(140),
    origin_bic          VARCHAR(11),
    value_date          DATE,
    settlement_date     DATE,
    status              VARCHAR(20),
    created_at          TIMESTAMP,
    updated_at          TIMESTAMP
);

CREATE TABLE IF NOT EXISTS outbound_payments (
    id                       UUID PRIMARY KEY,
    instruction_id           VARCHAR(35),
    amount                   NUMERIC(18, 2) NOT NULL,
    currency                 VARCHAR(3) NOT NULL,
    debtor_iban              VARCHAR(34),
    debtor_name              VARCHAR(140),
    destination_bic          VARCHAR(11),
    requested_execution_date DATE,
    charges_type             VARCHAR(3),
    status                   VARCHAR(20),
    created_at               TIMESTAMP,
    updated_at               TIMESTAMP
);

CREATE TABLE IF NOT EXISTS recalls (
    id                     UUID PRIMARY KEY,
    recall_reference       VARCHAR(35),
    recall_type            VARCHAR(10),
    original_payment_id    UUID,
    original_payment_type  VARCHAR(30),
    reason_code            VARCHAR(10),
    amount                 NUMERIC(18, 2),
    currency               VARCHAR(3),
    initiated_at           TIMESTAMP,
    deadline               DATE,
    status                 VARCHAR(20),
    created_at             TIMESTAMP,
    updated_at             TIMESTAMP
);
