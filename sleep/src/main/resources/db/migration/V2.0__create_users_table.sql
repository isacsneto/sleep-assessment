-- Users of the sleep logger. Authentication/authorization is out of scope for
-- this exercise, but the API is still aware of the concept of a user: every
-- sleep log belongs to exactly one user.
CREATE TABLE users (
    id         BIGSERIAL   PRIMARY KEY,
    username   VARCHAR(255) NOT NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_username UNIQUE (username)
);

-- Seed a default user so the API is usable out of the box (e.g. from the
-- Postman collection) without a user-management endpoint. Requests that do not
-- specify an X-User-Id header fall back to this user.
INSERT INTO users (id, username) VALUES (1, 'default_user');

-- Keep the sequence in sync with the explicitly-inserted id above.
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
