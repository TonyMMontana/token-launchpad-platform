CREATE TABLE users
(
    id       UUID PRIMARY KEY,
    email    VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL
);

CREATE TABLE user_roles
(
    user_id UUID         NOT NULL,
    roles   VARCHAR(255) NOT NULL,
    CONSTRAINT fk_user_roles_user_id
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_user_roles_user_id ON user_roles (user_id);