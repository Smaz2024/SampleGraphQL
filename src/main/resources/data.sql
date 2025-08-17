-- ===========================
-- USERS
-- ===========================
INSERT INTO users (username, email, password, role, created_at, updated_at) VALUES
('admin', 'admin@example.com', '$2a$10$7sZ8kQ.X2K7Xzwnh3Wh3Xeuv2DnUe4ZzVq0HxV0VJtEp9nRzFObcW', 'ROLE_ADMIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('johndoe', 'john@example.com', '$2a$10$0Yt6x12nsxPN1dQp6ryNce9dN7j0WwN9pCE5M./LWxH6Wwlz4fKZ2', 'ROLE_USER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('janedoe', 'jane@example.com', '$2a$10$h9GZPvxpj2mQZ3eTPOyB7eypXgIsUiQkqZhG0Cz3tV1I3zDMS3QWW', 'ROLE_USER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('moderator1', 'mod1@example.com', '$2a$10$zTwM71c2kR2q.wVrT0G.oeXaz4B4W9U8R3iE5lm6ZLsfVqWxYcK1a', 'ROLE_MODERATOR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('guestuser', 'guest@example.com', '$2a$10$C5p6gRr6mHzfFfWm7NYkfuDwUHT8qzVYimrVkpP6/abcdEfghIjKl', 'ROLE_USER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ===========================
-- POSTS
-- ===========================
INSERT INTO posts (title, content, user_id, created_at, updated_at) VALUES
('Welcome to GraphQL', 'This is an introduction to GraphQL with Spring Boot.', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Understanding Resolvers', 'Resolvers are how GraphQL fetches data behind the scenes.', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('GraphQL vs REST', 'What are the differences and why should you care?', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Spring Boot Tips', 'Helpful advice on building scalable Spring Boot apps.', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Security Best Practices', 'How to secure your Spring Boot application effectively.', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Exploring Subscriptions', 'Subscriptions allow real-time data with GraphQL.', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Caching Strategies', 'Using Redis for effective API caching.', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('How to Design Schema', 'Tips for designing clean, efficient GraphQL schemas.', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Using JWT with Spring', 'Implement secure auth using JWT tokens.', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Minimalist API Design', 'Keeping your API surface area clean and focused.', 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
