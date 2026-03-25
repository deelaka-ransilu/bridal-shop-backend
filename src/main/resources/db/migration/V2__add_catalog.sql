CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE categories (

                            category_id  SERIAL          PRIMARY KEY,
                            public_id    VARCHAR(20)     NOT NULL UNIQUE,
                            name         VARCHAR(100)    NOT NULL,
                            dress_type   VARCHAR(20)     NOT NULL
                                CHECK (dress_type IN ('BRIDAL', 'PARTY')),
                            description  TEXT,
                            is_active    BOOLEAN         NOT NULL DEFAULT TRUE,
                            created_at   TIMESTAMP       NOT NULL DEFAULT NOW(),
                            updated_at   TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_categories_dress_type  ON categories (dress_type);
CREATE INDEX idx_categories_is_active   ON categories (is_active);

CREATE TRIGGER trg_categories_updated_at
    BEFORE UPDATE ON categories
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TABLE dresses (
                         dress_id             SERIAL          PRIMARY KEY,
                         public_id            VARCHAR(20)     NOT NULL UNIQUE,
                         category_id          INT             NOT NULL REFERENCES categories (category_id),
                         name                 VARCHAR(150)    NOT NULL,
                         description          TEXT,
                         dress_type           VARCHAR(20)     NOT NULL
                             CHECK (dress_type IN ('BRIDAL', 'PARTY')),
                         retail_price         DECIMAL(12, 2)  NOT NULL,
                         fabric               VARCHAR(100),
                         color                VARCHAR(100),
                         made_for_customer_id INT             REFERENCES users (user_id),
                         is_available         BOOLEAN         NOT NULL DEFAULT TRUE,
                         is_active            BOOLEAN         NOT NULL DEFAULT TRUE,
                         created_at           TIMESTAMP       NOT NULL DEFAULT NOW(),
                         updated_at           TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_dresses_category_id        ON dresses (category_id);
CREATE INDEX idx_dresses_dress_type         ON dresses (dress_type);
CREATE INDEX idx_dresses_is_available       ON dresses (is_available);
CREATE INDEX idx_dresses_is_active          ON dresses (is_active);
CREATE INDEX idx_dresses_made_for_customer  ON dresses (made_for_customer_id);

CREATE TRIGGER trg_dresses_updated_at
    BEFORE UPDATE ON dresses
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TABLE dress_fulfillment_options (
                                           option_id              SERIAL          PRIMARY KEY,
                                           dress_id               INT             NOT NULL REFERENCES dresses (dress_id),
                                           fulfillment_type       VARCHAR(20)     NOT NULL
                                               CHECK (fulfillment_type IN ('CUSTOM', 'RENTAL', 'PURCHASE')),
                                           price_override         DECIMAL(12, 2),
                                           rental_price_per_day   DECIMAL(12, 2),
                                           rental_deposit         DECIMAL(12, 2),
                                           rental_period_days     INT             DEFAULT 3,
                                           is_active              BOOLEAN         NOT NULL DEFAULT TRUE,

                                           CONSTRAINT uq_dress_fulfillment_type UNIQUE (dress_id, fulfillment_type)
);

CREATE INDEX idx_dfo_dress_id          ON dress_fulfillment_options (dress_id);
CREATE INDEX idx_dfo_fulfillment_type  ON dress_fulfillment_options (fulfillment_type);
CREATE INDEX idx_dfo_is_active         ON dress_fulfillment_options (is_active);

CREATE TABLE dress_images (
                              image_id       SERIAL          PRIMARY KEY,
                              public_id      VARCHAR(20)     NOT NULL UNIQUE,
                              dress_id       INT             NOT NULL REFERENCES dresses (dress_id),
                              image_url      VARCHAR(500)    NOT NULL,
                              thumbnail_url  VARCHAR(500)    NOT NULL,
                              is_primary     BOOLEAN         NOT NULL DEFAULT FALSE,
                              display_order  INT             NOT NULL DEFAULT 0,
                              created_at     TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_dress_images_dress_id   ON dress_images (dress_id);
CREATE INDEX idx_dress_images_is_primary ON dress_images (dress_id, is_primary);

CREATE TABLE customer_measurements (
                                       measurement_id          SERIAL          PRIMARY KEY,
                                       public_id               VARCHAR(20)     NOT NULL UNIQUE,
                                       customer_id             INT             NOT NULL REFERENCES users (user_id),
                                       recorded_by             INT             NOT NULL REFERENCES users (user_id),  -- ADMIN only
                                       notes                   TEXT,
                                       measured_at             TIMESTAMP       NOT NULL,
                                       created_at              TIMESTAMP       NOT NULL DEFAULT NOW(),
                                       updated_at              TIMESTAMP       NOT NULL DEFAULT NOW(),
                                       height_with_shoes        DECIMAL(5, 2),
                                       hollow_to_hem            DECIMAL(5, 2),
                                       full_bust                DECIMAL(5, 2),
                                       under_bust               DECIMAL(5, 2),
                                       natural_waist            DECIMAL(5, 2),
                                       full_hip                 DECIMAL(5, 2),
                                       shoulder_width           DECIMAL(5, 2),
                                       torso_length             DECIMAL(5, 2),
                                       thigh_circumference      DECIMAL(5, 2),
                                       waist_to_knee            DECIMAL(5, 2),
                                       waist_to_floor           DECIMAL(5, 2),
                                       armhole                  DECIMAL(5, 2),
                                       bicep_circumference      DECIMAL(5, 2),
                                       elbow_circumference      DECIMAL(5, 2),
                                       wrist_circumference      DECIMAL(5, 2),
                                       sleeve_length            DECIMAL(5, 2),
                                       upper_bust               DECIMAL(5, 2),
                                       bust_apex_distance       DECIMAL(5, 2),
                                       shoulder_to_bust_point   DECIMAL(5, 2),
                                       neck_circumference       DECIMAL(5, 2),
                                       train_length             DECIMAL(5, 2)
);

CREATE INDEX idx_measurements_customer_id  ON customer_measurements (customer_id);
CREATE INDEX idx_measurements_recorded_by  ON customer_measurements (recorded_by);
CREATE INDEX idx_measurements_measured_at  ON customer_measurements (measured_at DESC);

CREATE TRIGGER trg_measurements_updated_at
    BEFORE UPDATE ON customer_measurements
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TABLE rentals (
                         rental_id              SERIAL          PRIMARY KEY,
                         public_id              VARCHAR(20)     NOT NULL UNIQUE,
                         dress_id               INT             NOT NULL REFERENCES dresses (dress_id),
                         customer_id            INT             NOT NULL REFERENCES users (user_id),
                         created_by             INT             NOT NULL REFERENCES users (user_id),
                         nic_number             VARCHAR(20)     NOT NULL,
                         rental_price_per_day   DECIMAL(12, 2)  NOT NULL,
                         rental_period_days     INT             NOT NULL DEFAULT 3,
                         deposit_amount         DECIMAL(12, 2)  NOT NULL,
                         total_rental_fee       DECIMAL(12, 2)  NOT NULL,
                         total_paid_upfront     DECIMAL(12, 2)  NOT NULL,
                         handed_over_at         TIMESTAMP,
                         due_date               DATE,
                         returned_at            TIMESTAMP,
                         days_late              INT             NOT NULL DEFAULT 0,
                         late_fine              DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
                         total_damage_cost      DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
                         total_deductions       DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
                         deposit_refunded       DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
                         outstanding_balance    DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
                         status                 VARCHAR(30)     NOT NULL DEFAULT 'BOOKED'
                             CHECK (status IN (
                                               'BOOKED',
                                               'HANDED_OVER',
                                               'OVERDUE',
                                               'RETURNED_CLEAN',
                                               'RETURNED_DAMAGED',
                                               'RETURNED_LATE',
                                               'RETURNED_LATE_DAMAGED',
                                               'BALANCE_DUE'
                                 )),
                         return_notes           TEXT,

                         created_at             TIMESTAMP       NOT NULL DEFAULT NOW(),
                         updated_at             TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_rentals_dress_id      ON rentals (dress_id);
CREATE INDEX idx_rentals_customer_id   ON rentals (customer_id);
CREATE INDEX idx_rentals_created_by    ON rentals (created_by);
CREATE INDEX idx_rentals_status        ON rentals (status);
CREATE INDEX idx_rentals_due_date      ON rentals (due_date);
CREATE INDEX idx_rentals_handed_over   ON rentals (handed_over_at);

CREATE TRIGGER trg_rentals_updated_at
    BEFORE UPDATE ON rentals
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TABLE rental_damage_items (
                                     damage_id       SERIAL          PRIMARY KEY,
                                     rental_id       INT             NOT NULL REFERENCES rentals (rental_id),
                                     description     VARCHAR(255)    NOT NULL,
                                     estimated_cost  DECIMAL(12, 2)  NOT NULL,
                                     created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_damage_items_rental_id ON rental_damage_items (rental_id);