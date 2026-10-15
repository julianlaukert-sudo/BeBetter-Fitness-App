package de.bebetter.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class UserMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void migrationenV1UndV2SindAngewendet() {
        Integer angewendet = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where version in ('1', '2') and success = true",
                Integer.class);

        assertThat(angewendet).isEqualTo(2);
    }

    @Test
    void emailDarfNurEinmalVorkommen() {
        nutzerEinfuegen("doppelt@test.de", "USER");

        assertThatThrownBy(() -> nutzerEinfuegen("doppelt@test.de", "USER"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rolleErlaubtNurUserOderAdmin() {
        assertThatThrownBy(() -> nutzerEinfuegen("gast@test.de", "GAST"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private void nutzerEinfuegen(String email, String rolle) {
        jdbcTemplate.update(
                "insert into nutzer (name, email, passwort_hash, rolle, registriert_am) values (?, ?, ?, ?, now())",
                "Testnutzer", email, "hash", rolle);
    }
}