-- =============================================================================
-- V2: roles ADMIN / LEADER / USER, tableros y dueño de las notas
-- =============================================================================

-- Roles válidos
ALTER TABLE users
    ADD CONSTRAINT chk_users_role CHECK (role IN ('ADMIN', 'LEADER', 'USER'));

-- Tableros
CREATE TABLE boards (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    created_by  BIGINT REFERENCES users (id),
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP
);

-- Tablero inicial: recibe las notas que existían antes de esta migración
INSERT INTO boards (name, description)
VALUES ('General', 'Tablero creado automáticamente con las notas existentes');

-- Cada nota pertenece a un tablero y guarda quién la creó
ALTER TABLE notes ADD COLUMN board_id BIGINT;
ALTER TABLE notes ADD COLUMN created_by BIGINT;
ALTER TABLE notes ADD COLUMN updated_at TIMESTAMP;

UPDATE notes
SET board_id = (SELECT id FROM boards WHERE name = 'General' ORDER BY id LIMIT 1);

ALTER TABLE notes ALTER COLUMN board_id SET NOT NULL;

ALTER TABLE notes
    ADD CONSTRAINT fk_notes_board FOREIGN KEY (board_id) REFERENCES boards (id) ON DELETE CASCADE;

ALTER TABLE notes
    ADD CONSTRAINT fk_notes_created_by FOREIGN KEY (created_by) REFERENCES users (id);

CREATE INDEX idx_notes_board_id ON notes (board_id);
CREATE INDEX idx_notes_created_by ON notes (created_by);
