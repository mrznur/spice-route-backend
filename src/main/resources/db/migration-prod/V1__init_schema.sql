-- Restaurant Feedback Collector: initial schema (matches erd.mmd)

CREATE TABLE restaurants (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(150) NOT NULL,
    slug       VARCHAR(150) NOT NULL UNIQUE,
    owner_id   UUID,
    status     VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
               CHECK (status IN ('ACTIVE', 'SUSPENDED')),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(150) NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL
                  CHECK (role IN ('SUPER_ADMIN', 'OWNER', 'MANAGER')),
    restaurant_id UUID REFERENCES restaurants (id) ON DELETE CASCADE,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    -- only the super admin may have no restaurant
    CONSTRAINT users_restaurant_required
        CHECK (role = 'SUPER_ADMIN' OR restaurant_id IS NOT NULL)
);

ALTER TABLE restaurants
    ADD CONSTRAINT fk_restaurants_owner
    FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE SET NULL;

CREATE INDEX idx_users_restaurant ON users (restaurant_id);

CREATE TABLE branches (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id UUID         NOT NULL REFERENCES restaurants (id) ON DELETE CASCADE,
    name          VARCHAR(150) NOT NULL,
    address       VARCHAR(255),
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_branches_restaurant ON branches (restaurant_id);

CREATE TABLE manager_branches (
    user_id   UUID NOT NULL REFERENCES users (id)    ON DELETE CASCADE,
    branch_id UUID NOT NULL REFERENCES branches (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, branch_id)
);

CREATE TABLE tables (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id UUID        NOT NULL REFERENCES restaurants (id) ON DELETE CASCADE,
    branch_id     UUID        NOT NULL REFERENCES branches (id)    ON DELETE CASCADE,
    label         VARCHAR(50) NOT NULL,
    qr_token      VARCHAR(64) NOT NULL UNIQUE,
    is_active     BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (branch_id, label)
);

CREATE INDEX idx_tables_restaurant ON tables (restaurant_id);

CREATE TABLE feedbacks (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id  UUID        NOT NULL REFERENCES restaurants (id) ON DELETE CASCADE,
    branch_id      UUID        NOT NULL REFERENCES branches (id)    ON DELETE CASCADE,
    table_id       UUID        NOT NULL REFERENCES tables (id)      ON DELETE CASCADE,
    rating         SMALLINT    NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment        TEXT,
    customer_name  VARCHAR(150),
    customer_phone VARCHAR(30),
    is_anonymous   BOOLEAN     NOT NULL DEFAULT FALSE,
    language       VARCHAR(10) NOT NULL DEFAULT 'en'
                   CHECK (language IN ('en', 'bn', 'banglish')),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_feedbacks_tenant_branch_date ON feedbacks (restaurant_id, branch_id, created_at);
CREATE INDEX idx_feedbacks_table ON feedbacks (table_id);

CREATE TABLE ai_summaries (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id UUID        NOT NULL REFERENCES restaurants (id) ON DELETE CASCADE,
    branch_id     UUID        NOT NULL REFERENCES branches (id)    ON DELETE CASCADE,
    requested_by  UUID        REFERENCES users (id) ON DELETE SET NULL,
    date_from     DATE        NOT NULL,
    date_to       DATE        NOT NULL,
    input_hash    VARCHAR(64) NOT NULL,
    comment_count INT         NOT NULL,
    result        JSONB,
    status        VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_ai_summaries_cache ON ai_summaries (restaurant_id, branch_id, input_hash);

CREATE TABLE ai_usage_logs (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id UUID        NOT NULL REFERENCES restaurants (id) ON DELETE CASCADE,
    summary_id    UUID        REFERENCES ai_summaries (id) ON DELETE SET NULL,
    input_tokens  INT         NOT NULL DEFAULT 0,
    output_tokens INT         NOT NULL DEFAULT 0,
    status        VARCHAR(20) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_ai_usage_restaurant_date ON ai_usage_logs (restaurant_id, created_at);
