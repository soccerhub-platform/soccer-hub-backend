-- ============================================
-- USERS & ROLES
-- ============================================

CREATE TABLE IF NOT EXISTS app_user
(
    id            UUID PRIMARY KEY,
    email         VARCHAR(100) NOT NULL UNIQUE,
    password_hash TEXT         NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP             DEFAULT NOW(),
    updated_at    TIMESTAMP             DEFAULT NOW(),
    created_by    VARCHAR,
    modified_by   VARCHAR
);

CREATE TABLE IF NOT EXISTS app_role
(
    code        VARCHAR(50) PRIMARY KEY,
    description VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS app_user_role
(
    user_id   UUID        NOT NULL,
    role_code VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role_code),
    CONSTRAINT fk_app_user_role_user FOREIGN KEY (user_id)
        REFERENCES app_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_app_user_role_role FOREIGN KEY (role_code)
        REFERENCES app_role (code)
);

-- ============================================
-- REFRESH TOKENS
-- ============================================

CREATE TABLE IF NOT EXISTS refresh_token
(
    id              UUID PRIMARY KEY,
    user_id         UUID      NOT NULL,
    jti             UUID      NOT NULL,
    token_hash      TEXT      NOT NULL,
    issued_at       TIMESTAMP          DEFAULT NOW() NOT NULL,
    expires_at      TIMESTAMP NOT NULL,
    revoked         BOOLEAN   NOT NULL DEFAULT FALSE,
    replaced_by_jty UUID,
    user_agent      VARCHAR,
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id)
        REFERENCES app_user (id) ON DELETE CASCADE
);

-- ============================================
-- CLUBS
-- ============================================

CREATE TABLE IF NOT EXISTS clubs
(
    id          UUID PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    slug        VARCHAR(50)  NOT NULL UNIQUE,
    email       VARCHAR(100),
    phone       VARCHAR(20),
    website     VARCHAR(255),
    logo_url    TEXT,
    address     TEXT,
    timezone    VARCHAR(50),
    is_active   BOOLEAN   DEFAULT TRUE,
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW(),
    created_by  VARCHAR,
    modified_by VARCHAR
);

-- ============================================
-- DISPATCHER EXTENSIONS
-- ============================================

-- Профиль диспетчера
CREATE TABLE IF NOT EXISTS dispatcher_profile
(
    id          UUID PRIMARY KEY,
    first_name  VARCHAR(100) NOT NULL,
    last_name   VARCHAR(100),
    phone       VARCHAR(20) UNIQUE,
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW(),
    created_by  VARCHAR,
    modified_by VARCHAR,
    CONSTRAINT fk_dispatcher_user FOREIGN KEY (id)
        REFERENCES app_user (id) ON DELETE CASCADE
);

-- Связь диспетчер → клуб
-- Диспетчер может управлять несколькими клубами
CREATE TABLE IF NOT EXISTS dispatcher_club
(
    dispatcher_id UUID NOT NULL,
    club_id       UUID NOT NULL,
    PRIMARY KEY (dispatcher_id, club_id),
    CONSTRAINT fk_dispatcher_club_dispatcher FOREIGN KEY (dispatcher_id)
        REFERENCES dispatcher_profile (id) ON DELETE CASCADE,
    CONSTRAINT fk_dispatcher_club_club FOREIGN KEY (club_id)
        REFERENCES clubs (id) ON DELETE CASCADE
);

-- ============================================
-- BRANCHES
-- ============================================

CREATE TABLE IF NOT EXISTS branches
(
    id          UUID PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    address     TEXT,
    active      BOOLEAN   DEFAULT FALSE,
    club_id     UUID         NOT NULL,
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW(),
    created_by  VARCHAR,
    modified_by VARCHAR,
    CONSTRAINT fk_branch_club FOREIGN KEY (club_id)
        REFERENCES clubs (id) ON DELETE CASCADE
);

-- ============================================
-- 5) ADMIN PROFILES (OPTIONAL FK → branches)
-- ============================================

CREATE TABLE admin_profiles
(
    id          UUID PRIMARY KEY,
    first_name  VARCHAR(100) NOT NULL,
    last_name   VARCHAR(100),
    email       VARCHAR(100) UNIQUE,
    phone       VARCHAR(20) UNIQUE,
    active      BOOLEAN   DEFAULT TRUE,
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW(),
    created_by  VARCHAR,
    modified_by VARCHAR,

    CONSTRAINT fk_admin_profiles_user FOREIGN KEY (id)
        REFERENCES app_user (id) ON DELETE CASCADE
);

CREATE TABLE admin_branches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    admin_id  UUID NOT NULL REFERENCES admin_profiles(id) ON DELETE CASCADE,
    branch_id UUID NOT NULL REFERENCES branches(id) ON DELETE CASCADE,

    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by  VARCHAR(255),
    modified_by VARCHAR(255),

    -- Каждый админ может быть привязан к филиалу только один раз
    CONSTRAINT uq_admin_branch UNIQUE (admin_id, branch_id)
);

CREATE INDEX idx_admin_branches_admin ON admin_branches(admin_id);
CREATE INDEX idx_admin_branches_branch ON admin_branches(branch_id);

-- ============================================
-- CLIENT_PROFILES
-- ============================================

CREATE TABLE IF NOT EXISTS client_profiles
(
    id          UUID PRIMARY KEY,
    first_name  VARCHAR(100) NOT NULL,
    last_name   VARCHAR(100),
    phone       VARCHAR(20) UNIQUE,
    branch_id   UUID,
    source      VARCHAR,
    comments    VARCHAR,
    status      VARCHAR,
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW(),
    created_by  VARCHAR,
    modified_by VARCHAR,
    CONSTRAINT fk_client_profiles_user FOREIGN KEY (id)
        REFERENCES app_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_client_profiles_branch FOREIGN KEY (branch_id)
        REFERENCES branches (id)
);

-- ============================================
-- PLAYERS
-- ============================================

CREATE TABLE IF NOT EXISTS players
(
    id          UUID PRIMARY KEY,
    first_name  VARCHAR NOT NULL,
    last_name   VARCHAR NOT NULL,
    birth_date  DATE    NOT NULL,
    position    VARCHAR,
    parent_id   UUID    NOT NULL,
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW(),
    created_by  VARCHAR,
    modified_by VARCHAR,
    CONSTRAINT fk_players_client FOREIGN KEY (parent_id)
        REFERENCES client_profiles (id) ON DELETE CASCADE
);

-- ============================================
-- GROUPS
-- ============================================

CREATE TABLE IF NOT EXISTS groups
(
    id          UUID PRIMARY KEY,
    branch_id   UUID         NOT NULL,
    name        VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW(),
    created_by  VARCHAR,
    modified_by VARCHAR,
    CONSTRAINT fk_groups_branch FOREIGN KEY (branch_id)
        REFERENCES branches (id) ON DELETE CASCADE
);

-- ============================================
-- SECTIONS
-- ============================================

CREATE TABLE IF NOT EXISTS sections
(
    id          UUID PRIMARY KEY,
    group_id    UUID         NOT NULL,
    name        VARCHAR(255) NOT NULL,
    schedule    TEXT,
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW(),
    created_by  VARCHAR,
    modified_by VARCHAR,
    CONSTRAINT fk_sections_group FOREIGN KEY (group_id)
        REFERENCES groups (id) ON DELETE CASCADE
);

-- ============================================
-- TRIALS
-- ============================================

CREATE TABLE IF NOT EXISTS trials
(
    id          UUID PRIMARY KEY,
    player_id   UUID NOT NULL,
    section_id  UUID NOT NULL,
    trial_date  DATE NOT NULL,
    result      VARCHAR(50),
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW(),
    created_by  VARCHAR,
    modified_by VARCHAR,
    CONSTRAINT fk_trials_player FOREIGN KEY (player_id)
        REFERENCES players (id) ON DELETE CASCADE,
    CONSTRAINT fk_trials_section FOREIGN KEY (section_id)
        REFERENCES sections (id) ON DELETE CASCADE
);

-- ============================================
-- CONTRACTS
-- ============================================

CREATE TABLE IF NOT EXISTS contracts
(
    id          UUID PRIMARY KEY,
    player_id   UUID NOT NULL,
    section_id  UUID NOT NULL,
    start_date  DATE NOT NULL,
    end_date    DATE,
    amount      DECIMAL(10, 2),
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW(),
    created_by  VARCHAR,
    modified_by VARCHAR,
    CONSTRAINT fk_contracts_player FOREIGN KEY (player_id)
        REFERENCES players (id),
    CONSTRAINT fk_contracts_section FOREIGN KEY (section_id)
        REFERENCES sections (id)
);

-- ============================================
-- ATTENDANCE
-- ============================================

CREATE TABLE IF NOT EXISTS attendance
(
    id            UUID PRIMARY KEY,
    player_id     UUID        NOT NULL,
    section_id    UUID        NOT NULL,
    attended_date DATE        NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'PRESENT',
    CONSTRAINT fk_attendance_player FOREIGN KEY (player_id)
        REFERENCES players (id),
    CONSTRAINT fk_attendance_section FOREIGN KEY (section_id)
        REFERENCES sections (id)
);