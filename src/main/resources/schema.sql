-- =============================================================
-- IssueFlow – Full Database Schema
-- All tables use CREATE TABLE IF NOT EXISTS for idempotency.
-- =============================================================

DROP TABLE IF EXISTS task;

-- ----- USERS -----------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id            BIGSERIAL    PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL,
    email         VARCHAR(255) NOT NULL,
    full_name     VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL CHECK (role IN ('ADMIN', 'DEVELOPER')),
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email    UNIQUE (email)
);

-- ----- PROJECTS --------------------------------------------
CREATE TABLE IF NOT EXISTS projects (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    owner_id    BIGINT       NOT NULL REFERENCES users(id),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMPTZ
);

-- ----- TICKETS ---------------------------------------------
CREATE TABLE IF NOT EXISTS tickets (
    id          BIGSERIAL   PRIMARY KEY,
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    status      VARCHAR(20)  NOT NULL DEFAULT 'TODO'
                    CHECK (status IN ('TODO','IN_PROGRESS','IN_REVIEW','DONE')),
    priority    VARCHAR(20)  NOT NULL
                    CHECK (priority IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    type        VARCHAR(20)  NOT NULL
                    CHECK (type IN ('BUG','FEATURE','TECHNICAL')),
    project_id  BIGINT       NOT NULL REFERENCES projects(id),
    assignee_id BIGINT       REFERENCES users(id),
    due_date    TIMESTAMPTZ,
    is_overdue  BOOLEAN      NOT NULL DEFAULT FALSE,
    version     BIGINT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMPTZ
);

-- ----- COMMENTS --------------------------------------------
CREATE TABLE IF NOT EXISTS comments (
    id         BIGSERIAL   PRIMARY KEY,
    ticket_id  BIGINT      NOT NULL REFERENCES tickets(id),
    author_id  BIGINT      NOT NULL REFERENCES users(id),
    content    TEXT        NOT NULL,
    version    BIGINT      NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ----- COMMENT MENTIONS ------------------------------------
CREATE TABLE IF NOT EXISTS comment_mentions (
    id                BIGSERIAL   PRIMARY KEY,
    comment_id        BIGINT      NOT NULL REFERENCES comments(id) ON DELETE CASCADE,
    mentioned_user_id BIGINT      NOT NULL REFERENCES users(id),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_comment_mention UNIQUE (comment_id, mentioned_user_id)
);

-- ----- TICKET DEPENDENCIES ---------------------------------
CREATE TABLE IF NOT EXISTS ticket_dependencies (
    id         BIGSERIAL   PRIMARY KEY,
    ticket_id  BIGINT      NOT NULL REFERENCES tickets(id),
    blocker_id BIGINT      NOT NULL REFERENCES tickets(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_ticket_dependency   UNIQUE (ticket_id, blocker_id),
    CONSTRAINT chk_no_self_dependency CHECK  (ticket_id <> blocker_id)
);

-- ----- ATTACHMENTS -----------------------------------------
CREATE TABLE IF NOT EXISTS attachments (
    id           BIGSERIAL    PRIMARY KEY,
    ticket_id    BIGINT       NOT NULL REFERENCES tickets(id),
    uploaded_by  BIGINT       NOT NULL REFERENCES users(id),
    filename     VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size    BIGINT       NOT NULL,
    file_data    BYTEA        NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ----- AUDIT LOGS ------------------------------------------
CREATE TABLE IF NOT EXISTS audit_logs (
    id          BIGSERIAL    PRIMARY KEY,
    actor       VARCHAR(50),
    action      VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50)  NOT NULL,
    entity_id   BIGINT,
    old_value   JSONB,
    new_value   JSONB,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ----- TOKEN BLOCKLIST (JWT logout / deny-list) ------------
CREATE TABLE IF NOT EXISTS token_blocklist (
    id         BIGSERIAL   PRIMARY KEY,
    jti        VARCHAR(36) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_token_jti UNIQUE (jti)
);

-- ----- INDEXES ---------------------------------------------
CREATE INDEX IF NOT EXISTS idx_tickets_project_id   ON tickets(project_id);
CREATE INDEX IF NOT EXISTS idx_tickets_assignee_id  ON tickets(assignee_id);
CREATE INDEX IF NOT EXISTS idx_tickets_deleted_at   ON tickets(deleted_at);
CREATE INDEX IF NOT EXISTS idx_projects_deleted_at  ON projects(deleted_at);
CREATE INDEX IF NOT EXISTS idx_comments_ticket_id   ON comments(ticket_id);
CREATE INDEX IF NOT EXISTS idx_comment_mentions_user ON comment_mentions(mentioned_user_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_entity    ON audit_logs(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_actor     ON audit_logs(actor);
CREATE INDEX IF NOT EXISTS idx_token_blocklist_jti  ON token_blocklist(jti);
CREATE INDEX IF NOT EXISTS idx_token_blocklist_exp  ON token_blocklist(expires_at);
