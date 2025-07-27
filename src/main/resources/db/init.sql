-- example 테이블이 없으면 생성
CREATE TABLE IF NOT EXISTS example (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

GRANT ALL PRIVILEGES ON TABLE example TO manager;

INSERT INTO example (name) VALUES
('샘플1'),
('샘플2'),
('샘플3');
