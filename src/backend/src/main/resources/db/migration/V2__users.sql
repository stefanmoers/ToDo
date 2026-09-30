-- US-010/020: Benutzerkonten. E-Mail wird kleingeschrieben gespeichert und ist eindeutig.
CREATE TABLE app_user (
    id            uuid PRIMARY KEY,
    email         varchar(254)  NOT NULL,
    display_name  varchar(100)  NOT NULL,
    password_hash varchar(255)  NOT NULL,
    status        varchar(16)   NOT NULL DEFAULT 'ACTIVE',
    role          varchar(16)   NOT NULL DEFAULT 'USER',
    time_zone     varchar(64)   NOT NULL,
    -- Wird bei Passwortänderung/-reset erhöht; Sitzungen mit älterem Wert sind ungültig (US-012).
    auth_epoch    integer       NOT NULL DEFAULT 0,
    created_at    timestamptz   NOT NULL,
    updated_at    timestamptz   NOT NULL,
    version       bigint        NOT NULL DEFAULT 0,
    CONSTRAINT uq_app_user_email UNIQUE (email),
    CONSTRAINT ck_app_user_email_lower CHECK (email = lower(email)),
    CONSTRAINT ck_app_user_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_app_user_role CHECK (role IN ('USER', 'ADMIN')),
    CONSTRAINT ck_app_user_display_name CHECK (char_length(btrim(display_name)) BETWEEN 1 AND 100)
);
