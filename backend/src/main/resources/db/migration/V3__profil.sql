-- V3: Tabelle profil (ein Profil pro Nutzer) gemäß Sprint-2-Anleitung.
-- Zusätzlich die Kleinschreibungsregel für E-Mails, die in V2 noch fehlt
-- (V2 ist bereits ausgeführt und wird nicht mehr verändert).
ALTER TABLE nutzer ADD CONSTRAINT ck_nutzer_email_lower CHECK (email = lower(email));

CREATE TABLE profil (
                        nutzer_id BIGINT PRIMARY KEY REFERENCES nutzer(id),
                        alter_jahre INTEGER NOT NULL CHECK (alter_jahre BETWEEN 16 AND 99),
                        geschlecht VARCHAR(10) NOT NULL,
                        groesse_cm INTEGER NOT NULL CHECK (groesse_cm BETWEEN 120 AND 230),
                        gewicht_kg NUMERIC(4,1) NOT NULL CHECK (gewicht_kg BETWEEN 30 AND 250),
                        ziel VARCHAR(15) NOT NULL,
                        fitnesslevel VARCHAR(10) NOT NULL,
                        trainingstage VARCHAR(20) NOT NULL,
                        erstellt_am TIMESTAMP WITH TIME ZONE NOT NULL,
                        aktualisiert_am TIMESTAMP WITH TIME ZONE NOT NULL
);