-- Catálogo de salas. La escalera de capacidades (A=4, B=6, C=8, D=12, E=20) es una decisión de
-- producto propia -- el enunciado exige un límite por sala pero no da los números -- documentada
-- en el ADR de la épica E09 (issue #10).
INSERT INTO room (id, capacity) VALUES
    ('A', 4),
    ('B', 6),
    ('C', 8),
    ('D', 12),
    ('E', 20);

-- Usuarios de la demo. El hash es BCrypt del password del enunciado (`TechnicalChallengePromtior`);
-- el password en texto plano no vive en ningún archivo versionado, solo su hash.
INSERT INTO app_user (username, password_hash) VALUES
    ('User1', '$2a$10$fKOV03W.15OuBSINwXtLcOx1zjO55mc5Oj.undKt7EEOMTbhT4ctO'),
    ('User2', '$2a$10$RPGbGa/ecQfpEGxdBRw15uXXgYHkbnaaEejAw16V5tu9uKwI7PQqy');
