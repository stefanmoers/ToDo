-- US-080 (M2): Kommentare. Gleiche Leseberechtigung wie die Aufgabe; Inhalt wird als reiner Text behandelt
-- (kein HTML-Rendering). Bearbeiten/Löschen von Kommentaren ist zunächst nicht erforderlich.
CREATE TABLE comment (
    id         uuid          PRIMARY KEY,
    task_id    uuid          NOT NULL REFERENCES task (id) ON DELETE CASCADE,
    author_id  uuid          NOT NULL REFERENCES app_user (id) ON DELETE RESTRICT,
    content    varchar(5000) NOT NULL,
    created_at timestamptz   NOT NULL,
    CONSTRAINT ck_comment_content CHECK (char_length(btrim(content)) BETWEEN 1 AND 5000)
);

CREATE INDEX ix_comment_task_created ON comment (task_id, created_at ASC, id ASC);

-- US-081 (M2): Aktivitätshistorie. Zusammen mit der jeweiligen Fachänderung gespeichert (nie nachträglich
-- durch Benutzer editierbar). from_value/to_value sind bereits anzeigefertiger, geheimnisfreier Text
-- (z. B. Titel, Status-/Prioritätsname, Bearbeiter-/Projektname zum Zeitpunkt der Änderung) statt roher IDs,
-- damit die Historie auch nach einer späteren Umbenennung/Entfernung des referenzierten Datensatzes stabil bleibt.
CREATE TABLE activity (
    id         uuid        PRIMARY KEY,
    task_id    uuid        NOT NULL REFERENCES task (id) ON DELETE CASCADE,
    actor_id   uuid        NOT NULL REFERENCES app_user (id) ON DELETE RESTRICT,
    type       varchar(32) NOT NULL,
    from_value varchar(200),
    to_value   varchar(200),
    created_at timestamptz NOT NULL,
    CONSTRAINT ck_activity_type CHECK (type IN (
        'CREATED', 'TITLE_CHANGED', 'STATUS_CHANGED', 'PRIORITY_CHANGED', 'ASSIGNEE_CHANGED',
        'DUE_CHANGED', 'PROJECT_CHANGED', 'COMPLETED', 'REOPENED', 'COMMENTED'
    ))
);

CREATE INDEX ix_activity_task_created ON activity (task_id, created_at ASC, id ASC);
