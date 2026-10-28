-- V3: password_hash pasa a guardar hashes Argon2id.
--
-- VARCHAR(60) era el largo exacto de un hash BCrypt. Un hash Argon2id con los
-- parámetros actuales ocupa 97 caracteres ($argon2id$v=19$m=16384,t=2,p=1$sal$hash)
-- y crece si se suben los parámetros, por eso se deja margen.
ALTER TABLE usuarios ALTER COLUMN password_hash SET DATA TYPE VARCHAR(255);
