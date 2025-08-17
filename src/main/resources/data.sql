-- ===========================
-- USERS
-- ===========================
INSERT INTO users (id, username, email, password, role, created_at, updated_at) VALUES
(1, 'admin', 'admin@example.com', 'admin123', 'ADMIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'johndoe', 'john@example.com', 'password123', 'USER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'janedoe', 'jane@example.com', 'secure456', 'USER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 'moderator1', 'mod1@example.com', 'modpass', 'MODERATOR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 'guestuser', 'guest@example.com', 'guestpass', 'USER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ===========================
-- POSTS
-- ===========================
INSERT INTO posts (id, title, content, user_id, created_at, updated_at) VALUES
(1, 'Welcome to GraphQL', 'This is an introduction to GraphQL with Spring Boot.', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'Understanding Resolvers', 'Resolvers are how GraphQL fetches data behind the scenes.', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'GraphQL vs REST', 'What are the differences and why should you care?', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 'Spring Boot Tips', 'Helpful advice on building scalable Spring Boot apps.', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 'Security Best Practices', 'How to secure your Spring Boot application effectively.', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, 'Exploring Subscriptions', 'Subscriptions allow real-time data with GraphQL.', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(7, 'Caching Strategies', 'Using Redis for effective API caching.', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(8, 'How to Design Schema', 'Tips for designing clean, efficient GraphQL schemas.', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(9, 'Using JWT with Spring', 'Implement secure auth using JWT tokens.', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(10, 'Minimalist API Design', 'Keeping your API surface area clean and focused.', 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
