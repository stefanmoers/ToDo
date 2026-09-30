-- US-060/061 (M2): Mitgliedschaft eines Benutzers in einem Projekt. Der Eigentümer ist davon unabhängig
-- immer zugriffsberechtigt und braucht keine eigene Zeile; wiederholte Aufnahme dupliziert nichts (UNIQUE).
CREATE TABLE membership (
    id         uuid        PRIMARY KEY,
    project_id uuid        NOT NULL REFERENCES project (id) ON DELETE CASCADE,
    user_id    uuid        NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    created_at timestamptz NOT NULL,
    CONSTRAINT uq_membership UNIQUE (project_id, user_id)
);

CREATE INDEX ix_membership_user ON membership (user_id);

-- US-062 (M2): Zuständigkeit für eine Aufgabe. Wer zugewiesen werden darf (aktives Projektmitglied oder
-- Eigentümer) wird auf Anwendungsebene geprüft, nicht durch eine Datenbank-Prüfung allein.
ALTER TABLE task ADD COLUMN assignee_id uuid REFERENCES app_user (id) ON DELETE RESTRICT;

CREATE INDEX ix_task_assignee ON task (assignee_id) WHERE assignee_id IS NOT NULL;
