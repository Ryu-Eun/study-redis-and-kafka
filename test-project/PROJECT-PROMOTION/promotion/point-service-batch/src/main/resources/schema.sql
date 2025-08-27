DROP TABLE IF EXISTS points;
DROP TABLE IF EXISTS point_balances;
DROP TABLE IF EXISTS daily_point_reports;

CREATE TABLE point_balances
(
    id         BIGINT AUTO_INCREMENT,
    user_id    BIGINT NOT NULL,
    balance    BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    version    BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_point_balance_user_id UNIQUE (user_id)
);

CREATE TABLE points
(
    id               BIGINT AUTO_INCREMENT,
    user_id          BIGINT      NOT NULL,
    amount           BIGINT      NOT NULL,
    type             VARCHAR(20) NOT NULL,
    description      VARCHAR(255),
    balance_snapshot BIGINT      NOT NULL,
    created_at       TIMESTAMP,
    updated_at       TIMESTAMP,

    point_balance_id BIGINT,
    version          BIGINT NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
    CONSTRAINT fk_points_point_balance
        FOREIGN KEY (point_balance_id) REFERENCES point_balances(id)
);

CREATE TABLE daily_point_reports
(
    id            BIGINT AUTO_INCREMENT,
    user_id       BIGINT NOT NULL,
    report_date   DATE   NOT NULL,
    earn_amount   BIGINT NOT NULL DEFAULT 0,
    use_amount    BIGINT NOT NULL DEFAULT 0,
    cancel_amount BIGINT NOT NULL DEFAULT 0,
    net_amount    BIGINT NOT NULL DEFAULT 0,
    created_at    TIMESTAMP,
    updated_at    TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_daily_point_report UNIQUE (user_id, report_date)
);
