-- US-030 bis 037, US-120: Persönliche Aufgaben (M1b). Projekt (M1c), Bearbeiter (M2), Tags (M1c) folgen als eigene Migrationen.
-- Eine Aufgabe ohne Projekt gehört ausschließlich ihrem Ersteller (docs/DOMAIN_MODEL.md).
CREATE TABLE task (
    id                 uuid           PRIMARY KEY,
    creator_id         uuid           NOT NULL,
    title              varchar(200)   NOT NULL,
    description        varchar(10000),
    status             varchar(16)    NOT NULL DEFAULT 'OPEN',
    priority           varchar(16)    NOT NULL DEFAULT 'NORMAL',
    -- Fälligkeit (US-037): lokale Angaben + IANA-Zone sind maßgeblich; due_deadline ist daraus abgeleitet.
    due_date           date,
    due_time           time(0),
    due_time_zone      varchar(64),
    -- Gewählter UTC-Offset in Sekunden; nötig, um doppelte Uhrzeiten bei Zeitumstellung eindeutig zu machen.
    due_offset_seconds integer,
    -- Erster Zeitpunkt, ab dem die Aufgabe überfällig ist (mit Uhrzeit: der Zeitpunkt selbst; ohne: Beginn des Folgetags in der Zone).
    due_deadline       timestamptz,
    completed_at       timestamptz,
    created_at         timestamptz    NOT NULL,
    updated_at         timestamptz    NOT NULL,
    version            bigint         NOT NULL DEFAULT 0,
    CONSTRAINT fk_task_creator FOREIGN KEY (creator_id) REFERENCES app_user (id) ON DELETE RESTRICT,
    CONSTRAINT ck_task_title CHECK (char_length(btrim(title)) BETWEEN 1 AND 200),
    CONSTRAINT ck_task_description CHECK (description IS NULL OR char_length(description) BETWEEN 1 AND 10000),
    CONSTRAINT ck_task_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'DONE')),
    CONSTRAINT ck_task_priority CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT')),
    CONSTRAINT ck_task_completed_at CHECK ((status = 'DONE') = (completed_at IS NOT NULL)),
    CONSTRAINT ck_task_due_time_needs_date CHECK (due_time IS NULL OR due_date IS NOT NULL),
    CONSTRAINT ck_task_due_zone CHECK ((due_date IS NULL) = (due_time_zone IS NULL)),
    CONSTRAINT ck_task_due_offset CHECK ((due_time IS NULL) = (due_offset_seconds IS NULL)),
    CONSTRAINT ck_task_due_offset_range CHECK (due_offset_seconds IS NULL OR due_offset_seconds BETWEEN -64800 AND 64800),
    CONSTRAINT ck_task_due_deadline CHECK ((due_date IS NULL) = (due_deadline IS NULL)),
    CONSTRAINT ck_task_timestamps CHECK (updated_at >= created_at),
    CONSTRAINT ck_task_version CHECK (version >= 0)
);

-- Liste "eigene Aufgaben" mit stabiler Sortierung (created_at DESC, id DESC); deckt zugleich den FK creator_id ab.
CREATE INDEX ix_task_creator_created ON task (creator_id, created_at DESC, id DESC);
