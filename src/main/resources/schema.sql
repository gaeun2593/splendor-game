-- schema.sql
CREATE TABLE static_cards (
    id BIGINT PRIMARY KEY,
    level INT,
    points INT,
    bonus_gem VARCHAR(20),
    cost_diamond INT,
    cost_sapphire INT,
    cost_emerald INT,
    cost_ruby INT,
    cost_onyx INT
);

CREATE TABLE static_nobles (
    id BIGINT PRIMARY KEY,
    points INT,
    cost_diamond INT,
    cost_sapphire INT,
    cost_emerald INT,
    cost_ruby INT,
    cost_onyx INT
);