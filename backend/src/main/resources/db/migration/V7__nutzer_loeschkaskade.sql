ALTER TABLE profil DROP CONSTRAINT profil_nutzer_id_fkey;
ALTER TABLE profil ADD CONSTRAINT profil_nutzer_id_fkey
    FOREIGN KEY (nutzer_id) REFERENCES nutzer(id) ON DELETE CASCADE;

ALTER TABLE trainingsplan DROP CONSTRAINT trainingsplan_nutzer_id_fkey;
ALTER TABLE trainingsplan ADD CONSTRAINT trainingsplan_nutzer_id_fkey
    FOREIGN KEY (nutzer_id) REFERENCES nutzer(id) ON DELETE CASCADE;