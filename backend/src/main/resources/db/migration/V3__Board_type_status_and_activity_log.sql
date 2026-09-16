-- =============================================================================
-- V3: tipo y estado de los tableros, fecha de completado de las notas
--     e historial de actividad para el dashboard
-- =============================================================================

-- Tipo y estado del tablero
ALTER TABLE boards ADD COLUMN board_type VARCHAR(30) NOT NULL DEFAULT 'PROJECT';
ALTER TABLE boards ADD COLUMN board_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE boards
    ADD CONSTRAINT chk_boards_type CHECK (board_type IN ('PROJECT', 'SPRINT', 'SUPPORT', 'MEETING', 'OTHER'));
ALTER TABLE boards
    ADD CONSTRAINT chk_boards_status CHECK (board_status IN ('ACTIVE', 'PAUSED', 'FINISHED'));

-- Cuándo se completó cada nota (para medir el avance en el tiempo)
ALTER TABLE notes ADD COLUMN completed_at TIMESTAMP;

UPDATE notes
SET completed_at = COALESCE(updated_at, created_at)
WHERE status = 'COMPLETED';

-- Historial de actividad. Guarda copia del nombre del tablero y del título de la nota
-- para que el historial se siga leyendo aunque se eliminen.
CREATE TABLE activity_log (
    id            BIGSERIAL PRIMARY KEY,
    action        VARCHAR(40)  NOT NULL,
    user_id       BIGINT       NOT NULL REFERENCES users (id),
    board_id      BIGINT,
    board_name    VARCHAR(100),
    note_id       BIGINT,
    note_title    VARCHAR(255),
    note_owner_id BIGINT REFERENCES users (id),
    from_value    VARCHAR(30),
    to_value      VARCHAR(30),
    details       VARCHAR(500),
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_activity_log_created_at ON activity_log (created_at DESC);
CREATE INDEX idx_activity_log_user_id ON activity_log (user_id);
CREATE INDEX idx_activity_log_board_id ON activity_log (board_id);

-- Historial inicial con lo que ya se sabe: quién creó cada tablero y cada nota, y cuándo
INSERT INTO activity_log (action, user_id, board_id, board_name, created_at)
SELECT 'BOARD_CREATED', b.created_by, b.id, b.name, b.created_at
FROM boards b
WHERE b.created_by IS NOT NULL;

INSERT INTO activity_log (action, user_id, board_id, board_name, note_id, note_title, note_owner_id, to_value, created_at)
SELECT 'NOTE_CREATED', n.created_by, b.id, b.name, n.id, n.title, n.created_by, n.status, COALESCE(n.created_at, CURRENT_TIMESTAMP)
FROM notes n
JOIN boards b ON b.id = n.board_id
WHERE n.created_by IS NOT NULL;
