-- ============================================
-- USERS & ROLES
-- ============================================

CREATE TABLE IF NOT EXISTS app_user
(
    id            UUID PRIMARY KEY,
    email         VARCHAR(100) NOT NULL UNIQUE,
    password_hash TEXT         NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    force_password_change BOOLEAN NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP DEFAULT NOW(),
    updated_at    TIMESTAMP DEFAULT NOW(),
    created_by    VARCHAR,
    modified_by   VARCHAR
);

CREATE INDEX idx_app_user_email ON app_user(email);

CREATE TABLE IF NOT EXISTS app_role
(
    code        VARCHAR(50) PRIMARY KEY,
    description VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS app_user_role
(
    user_id   UUID NOT NULL,
    role_code VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role_code),

    CONSTRAINT fk_app_user_role_user FOREIGN KEY (user_id)
        REFERENCES app_user (id) ON DELETE CASCADE,

    CONSTRAINT fk_app_user_role_role FOREIGN KEY (role_code)
        REFERENCES app_role (code)
);

CREATE INDEX idx_app_user_role_user ON app_user_role(user_id);
CREATE INDEX idx_app_user_role_role ON app_user_role(role_code);

-- ============================================
-- REFRESH TOKENS
-- ============================================

CREATE TABLE IF NOT EXISTS refresh_token
(
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL,
    jti             UUID NOT NULL,
    token_hash      TEXT NOT NULL,
    issued_at       TIMESTAMP DEFAULT NOW() NOT NULL,
    expires_at      TIMESTAMP NOT NULL,
    revoked         BOOLEAN NOT NULL DEFAULT FALSE,
    replaced_by_jty UUID,
    user_agent      VARCHAR,

    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id)
        REFERENCES app_user (id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_token_user ON refresh_token(user_id);
CREATE INDEX idx_refresh_token_jti ON refresh_token(jti);
CREATE INDEX idx_refresh_token_expires ON refresh_token(expires_at);

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
    is_active   BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW(),
    created_by  VARCHAR,
    modified_by VARCHAR
);

CREATE INDEX idx_clubs_slug ON clubs(slug);
CREATE INDEX idx_clubs_active ON clubs(is_active);

-- ============================================
-- DISPATCHER PROFILES
-- ============================================

CREATE TABLE IF NOT EXISTS dispatcher_profiles
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

CREATE INDEX idx_dispatcher_profiles_phone ON dispatcher_profiles(phone);

CREATE TABLE IF NOT EXISTS dispatcher_club
(
    dispatcher_id UUID NOT NULL,
    club_id       UUID NOT NULL,
    PRIMARY KEY (dispatcher_id, club_id),

    CONSTRAINT fk_dispatcher_club_dispatcher FOREIGN KEY (dispatcher_id)
        REFERENCES dispatcher_profiles (id) ON DELETE CASCADE,

    CONSTRAINT fk_dispatcher_club_club FOREIGN KEY (club_id)
        REFERENCES clubs (id) ON DELETE CASCADE
);

CREATE INDEX idx_dispatcher_club_dispatcher ON dispatcher_club(dispatcher_id);
CREATE INDEX idx_dispatcher_club_club ON dispatcher_club(club_id);

-- ============================================
-- BRANCHES
-- ============================================

CREATE TABLE IF NOT EXISTS branches
(
    id          UUID PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    address     TEXT,
    active      BOOLEAN DEFAULT FALSE,
    club_id     UUID NOT NULL,
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW(),
    created_by  VARCHAR,
    modified_by VARCHAR,

    CONSTRAINT fk_branches_club FOREIGN KEY (club_id)
        REFERENCES clubs (id) ON DELETE CASCADE
);

CREATE INDEX idx_branches_club_id ON branches(club_id);
CREATE INDEX idx_branches_active ON branches(active);

-- ============================================
-- ADMIN PROFILES
-- ============================================

CREATE TABLE admin_profiles
(
    id          UUID PRIMARY KEY,
    first_name  VARCHAR(100) NOT NULL,
    last_name   VARCHAR(100),
    email       VARCHAR(100) UNIQUE,
    phone       VARCHAR(20) UNIQUE,
    active      BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW(),
    created_by  VARCHAR,
    modified_by VARCHAR,

    CONSTRAINT fk_admin_profiles_user FOREIGN KEY (id)
        REFERENCES app_user (id) ON DELETE CASCADE
);

CREATE INDEX idx_admin_profiles_email ON admin_profiles(email);
CREATE INDEX idx_admin_profiles_phone ON admin_profiles(phone);
CREATE INDEX idx_admin_profiles_active ON admin_profiles(active);

CREATE TABLE admin_branches
(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    admin_id  UUID NOT NULL REFERENCES admin_profiles(id) ON DELETE CASCADE,
    branch_id UUID NOT NULL REFERENCES branches(id) ON DELETE CASCADE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by  VARCHAR(255),
    modified_by VARCHAR(255),

    CONSTRAINT uq_admin_branch UNIQUE (admin_id, branch_id)
);

CREATE INDEX idx_admin_branches_admin ON admin_branches(admin_id);
CREATE INDEX idx_admin_branches_branch ON admin_branches(branch_id);

-- ============================================
-- CLIENT PROFILES
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

CREATE INDEX idx_client_profiles_branch ON client_profiles(branch_id);
CREATE INDEX idx_client_profiles_status ON client_profiles(status);

-- ============================================
-- PLAYERS
-- ============================================

CREATE TABLE IF NOT EXISTS players
(
    id          UUID PRIMARY KEY,
    first_name  VARCHAR NOT NULL,
    last_name   VARCHAR NOT NULL,
    birth_date  DATE NOT NULL,
    position    VARCHAR,
    parent_id   UUID NOT NULL,
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW(),
    created_by  VARCHAR,
    modified_by VARCHAR,

    CONSTRAINT fk_players_client FOREIGN KEY (parent_id)
        REFERENCES client_profiles (id) ON DELETE CASCADE
);

CREATE INDEX idx_players_parent ON players(parent_id);

-- ============================================
-- COACH PROFILES
-- ============================================

CREATE TABLE IF NOT EXISTS coach_profiles
(
    id          UUID PRIMARY KEY,
    first_name  VARCHAR NOT NULL,
    last_name   VARCHAR NOT NULL,
    birth_date  DATE,
    phone       VARCHAR NOT NULL,
    email       VARCHAR NOT NULL,
    status      VARCHAR NOT NULL,

    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW(),
    created_by  VARCHAR,
    modified_by VARCHAR,

    CONSTRAINT fk_coach_profiles_user FOREIGN KEY (id)
        REFERENCES app_user (id) ON DELETE CASCADE
);

CREATE INDEX idx_coach_profiles_status ON coach_profiles(status);

CREATE TABLE IF NOT EXISTS coach_branches
(
    id        UUID PRIMARY KEY,
    coach_id  UUID NOT NULL,
    branch_id UUID NOT NULL,
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW(),
    created_by  VARCHAR,
    modified_by VARCHAR,

    CONSTRAINT fk_coach_branch_coach FOREIGN KEY (coach_id)
        REFERENCES coach_profiles (id),

    CONSTRAINT fk_coach_branch_branch FOREIGN KEY (branch_id)
        REFERENCES branches (id)
);

CREATE INDEX idx_coach_branches_coach ON coach_branches(coach_id);
CREATE INDEX idx_coach_branches_branch ON coach_branches(branch_id);

-- ============================================
-- GROUPS
-- ============================================

CREATE TABLE IF NOT EXISTS groups
(
    id          UUID PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    branch_id   UUID NOT NULL,
    age_from    INT2 NOT NULL,
    age_to      INT2 NOT NULL,
    level       VARCHAR NOT NULL,
    description TEXT,
    status      VARCHAR NOT NULL,
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW(),
    created_by  VARCHAR,
    modified_by VARCHAR,

    CONSTRAINT fk_groups_branch FOREIGN KEY (branch_id)
        REFERENCES branches (id) ON DELETE CASCADE
);

CREATE INDEX idx_groups_branch ON groups(branch_id);
CREATE INDEX idx_groups_status ON groups(status);
CREATE INDEX idx_groups_age_range ON groups(age_from, age_to);

-- ============================================
-- LOCATION
-- ============================================

CREATE TABLE IF NOT EXISTS location
(
    id        UUID PRIMARY KEY,
    branch_id UUID NOT NULL,
    name      VARCHAR(255) NOT NULL,
    address   VARCHAR(255),
    active    BOOLEAN DEFAULT TRUE,
    latitude  DECIMAL(10,8),
    longitude DECIMAL(11,8),

    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW(),
    created_by  VARCHAR,
    modified_by VARCHAR,

    CONSTRAINT fk_location_branch FOREIGN KEY (branch_id)
        REFERENCES branches(id) ON DELETE CASCADE
);

CREATE INDEX idx_location_branch ON location(branch_id);
CREATE INDEX idx_location_active ON location(active);

-- ============================================
-- GROUP_COACHES
-- ============================================

CREATE TABLE IF NOT EXISTS group_coaches
(
    id            UUID PRIMARY KEY,
    group_id      UUID NOT NULL,
    coach_id      UUID NOT NULL,
    role          VARCHAR(32),
    active        BOOLEAN DEFAULT TRUE,
    assigned_from DATE,
    assigned_to   DATE,

    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW(),
    created_by  VARCHAR,
    modified_by VARCHAR,

    CONSTRAINT fk_group_coaches_group FOREIGN KEY (group_id)
        REFERENCES groups(id) ON DELETE CASCADE,

    CONSTRAINT fk_group_coaches_coach FOREIGN KEY (coach_id)
        REFERENCES coach_profiles(id) ON DELETE CASCADE
);

CREATE INDEX idx_group_coaches_group ON group_coaches(group_id);
CREATE INDEX idx_group_coaches_coach ON group_coaches(coach_id);
CREATE INDEX idx_group_coaches_active ON group_coaches(active);

-- ============================================
-- GROUP SCHEDULES
-- ============================================

CREATE TABLE IF NOT EXISTS group_schedules
(
    id              UUID PRIMARY KEY,
    group_id        UUID NOT NULL,
    coach_id        UUID NOT NULL,

    day_off_week    VARCHAR NOT NULL,
    start_time      TIME NOT NULL,
    end_time        TIME NOT NULL,
    schedule_type   VARCHAR NOT NULL,
    start_date      DATE NOT NULL,
    end_date        DATE NOT NULL,
    status          VARCHAR NOT NULL,
    comment         TEXT,

    substitution BOOLEAN DEFAULT FALSE,
    substitution_coach_id UUID,

    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW(),
    created_by  VARCHAR,
    modified_by VARCHAR,

    CONSTRAINT fk_group_schedule_group FOREIGN KEY (group_id)
        REFERENCES groups(id) ON DELETE CASCADE,

    CONSTRAINT fk_group_schedule_coach FOREIGN KEY (coach_id)
        REFERENCES coach_profiles(id),

    CONSTRAINT fk_group_schedule_substitution_coach FOREIGN KEY (substitution_coach_id)
        REFERENCES coach_profiles(id)
);

CREATE INDEX idx_group_schedules_group ON group_schedules(group_id);
CREATE INDEX idx_group_schedules_coach ON group_schedules(coach_id);
CREATE INDEX idx_group_schedules_day_time ON group_schedules(day_off_week, start_time);
CREATE INDEX idx_group_schedules_status ON group_schedules(status);

-- ============================================
-- SECTIONS
-- ============================================

CREATE TABLE IF NOT EXISTS sections
(
    id          UUID PRIMARY KEY,
    group_id    UUID NOT NULL,
    name        VARCHAR(255) NOT NULL,
    schedule    TEXT,
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW(),
    created_by  VARCHAR,
    modified_by VARCHAR,

    CONSTRAINT fk_sections_group FOREIGN KEY (group_id)
        REFERENCES groups(id) ON DELETE CASCADE
);

CREATE INDEX idx_sections_group ON sections(group_id);

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
        REFERENCES players(id) ON DELETE CASCADE,

    CONSTRAINT fk_trials_section FOREIGN KEY (section_id)
        REFERENCES sections(id) ON DELETE CASCADE
);

CREATE INDEX idx_trials_player ON trials(player_id);
CREATE INDEX idx_trials_section ON trials(section_id);
CREATE INDEX idx_trials_date ON trials(trial_date);

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
    amount      DECIMAL(10,2),
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW(),
    created_by  VARCHAR,
    modified_by VARCHAR,

    CONSTRAINT fk_contracts_player FOREIGN KEY (player_id)
        REFERENCES players(id),

    CONSTRAINT fk_contracts_section FOREIGN KEY (section_id)
        REFERENCES sections(id)
);

CREATE INDEX idx_contracts_player ON contracts(player_id);
CREATE INDEX idx_contracts_section ON contracts(section_id);

-- ============================================
-- ATTENDANCE
-- ============================================

CREATE TABLE IF NOT EXISTS attendance
(
    id            UUID PRIMARY KEY,
    player_id     UUID NOT NULL,
    section_id    UUID NOT NULL,
    attended_date DATE NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'PRESENT',

    CONSTRAINT fk_attendance_player FOREIGN KEY (player_id)
        REFERENCES players(id),

    CONSTRAINT fk_attendance_section FOREIGN KEY (section_id)
        REFERENCES sections(id)
);

CREATE INDEX idx_attendance_player ON attendance(player_id);
CREATE INDEX idx_attendance_section ON attendance(section_id);
CREATE INDEX idx_attendance_date ON attendance(attended_date);
CREATE INDEX idx_attendance_status ON attendance(status);