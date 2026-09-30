-- US-040 bis 042 (M1c): Projekte. Vorerst ohne Mitgliedschaft; Zusammenarbeit folgt erst mit US-060 (M2).
CREATE TABLE project (
    id          uuid         PRIMARY KEY,
    owner_id    uuid         NOT NULL,
    name        varchar(100) NOT NULL,
    description varchar(10000),
    created_at  timestamptz  NOT NULL,
    updated_at  timestamptz  NOT NULL,
    version     bigint       NOT NULL DEFAULT 0,
    CONSTRAINT fk_project_owner FOREIGN KEY (owner_id) REFERENCES app_user (id) ON DELETE RESTRICT,
    CONSTRAINT ck_project_name CHECK (char_length(btrim(name)) BETWEEN 1 AND 100),
    CONSTRAINT ck_project_description CHECK (description IS NULL OR char_length(description) BETWEEN 1 AND 10000),
    CONSTRAINT ck_project_version CHECK (version >= 0)
);

CREATE INDEX ix_project_owner_created ON project (owner_id, created_at DESC, id DESC);

-- Eine Aufgabe kann optional einem Projekt zugeordnet sein (US-030/032 Erweiterung). RESTRICT ist ein
-- Sicherheitsnetz: die Anwendung löst Aufgaben beim Löschen eines Projekts transaktional ab (project_id = NULL),
-- bevor die Projektzeile selbst gelöscht wird (US-042) — Aufgaben werden dabei nie mitgelöscht.
ALTER TABLE task ADD COLUMN project_id uuid REFERENCES project (id) ON DELETE RESTRICT;

CREATE INDEX ix_task_project_created ON task (project_id, created_at DESC, id DESC) WHERE project_id IS NOT NULL;
