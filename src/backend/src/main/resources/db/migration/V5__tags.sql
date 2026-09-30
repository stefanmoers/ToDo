-- US-043 (M1c): Tags. Ein Tag gehört entweder persönlich einem Benutzer oder einem Projekt (genau eines von beiden),
-- innerhalb seines Geltungsbereichs ohne Beachtung der Großschreibung eindeutig (docs/DOMAIN_MODEL.md).
CREATE TABLE tag (
    id         uuid         PRIMARY KEY,
    owner_id   uuid         REFERENCES app_user (id) ON DELETE RESTRICT,
    project_id uuid         REFERENCES project (id) ON DELETE RESTRICT,
    name       varchar(50)  NOT NULL,
    created_at timestamptz  NOT NULL,
    CONSTRAINT ck_tag_scope CHECK ((owner_id IS NOT NULL) <> (project_id IS NOT NULL)),
    CONSTRAINT ck_tag_name CHECK (char_length(btrim(name)) BETWEEN 1 AND 50)
);

CREATE UNIQUE INDEX uq_tag_owner_name ON tag (owner_id, lower(name)) WHERE owner_id IS NOT NULL;
CREATE UNIQUE INDEX uq_tag_project_name ON tag (project_id, lower(name)) WHERE project_id IS NOT NULL;

-- Zuordnung Aufgabe/Tag (US-043). Löschen einer Aufgabe oder eines Tags entfernt nur die Zuordnung, nie die
-- jeweils andere Seite ("Entfernen einer Zuordnung löscht keine andere Aufgabe").
CREATE TABLE task_tag (
    id         uuid        PRIMARY KEY,
    task_id    uuid        NOT NULL REFERENCES task (id) ON DELETE CASCADE,
    tag_id     uuid        NOT NULL REFERENCES tag (id) ON DELETE CASCADE,
    created_at timestamptz NOT NULL,
    CONSTRAINT uq_task_tag UNIQUE (task_id, tag_id)
);

CREATE INDEX ix_task_tag_task ON task_tag (task_id);
CREATE INDEX ix_task_tag_tag ON task_tag (tag_id);
