DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'kong') THEN
        CREATE ROLE kong WITH LOGIN PASSWORD 'kong_password';
    END IF;
END
$$;

SELECT 'CREATE DATABASE kong_db OWNER kong'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'kong_db')\gexec

GRANT ALL PRIVILEGES ON DATABASE kong_db TO kong;
GRANT ALL PRIVILEGES ON DATABASE harbor_db TO harbor_admin;
