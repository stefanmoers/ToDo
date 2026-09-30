-- US-070/071 (M2): Unteraufgaben. Eine einfache Checkliste innerhalb einer Aufgabe, keine weitere vollständige
-- Aufgabe; genau eine Ebene, keine rekursiven Unteraufgaben (docs/DOMAIN_MODEL.md). Rechte richten sich nach der
-- Hauptaufgabe (task); Löschen der Aufgabe entfernt ihre Unteraufgaben transaktional per CASCADE.
CREATE TABLE subtask (
    id         uuid         PRIMARY KEY,
    task_id    uuid         NOT NULL REFERENCES task (id) ON DELETE CASCADE,
    title      varchar(200) NOT NULL,
    done       boolean      NOT NULL DEFAULT false,
    sort_order integer      NOT NULL,
    created_at timestamptz  NOT NULL,
    updated_at timestamptz  NOT NULL,
    CONSTRAINT ck_subtask_title CHECK (char_length(btrim(title)) BETWEEN 1 AND 200),
    CONSTRAINT ck_subtask_timestamps CHECK (updated_at >= created_at)
);

-- Liste der Unteraufgaben einer Aufgabe in stabiler Reihenfolge (US-070); sort_order ist bewusst nicht eindeutig
-- erzwungen (id löst Gleichstände auf), damit ein Vertauschen zweier Werte ohne Deferred-Constraint möglich ist.
CREATE INDEX ix_subtask_task_order ON subtask (task_id, sort_order, id);
