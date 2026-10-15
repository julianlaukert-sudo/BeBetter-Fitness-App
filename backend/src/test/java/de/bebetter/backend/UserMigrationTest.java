package de.bebetter.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class UserMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void migrationsV1AndV2AreApplied() {
        Integer applied = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where version in ('1', '2') and success = true",
                Integer.class);
        assertThat(applied).isEqualTo(2);
    }

    @Test
    void emailMustBeUnique() {
        insertUser("duplicate@test.com", "USER");
        assertThatThrownBy(() -> insertUser("duplicate@test.com", "USER"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void roleOnlyAllowsUserOrAdmin() {
        assertThatThrownBy(() -> insertUser("guest@test.com", "GUEST"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // SQL nutzt bewusst die deutschen Tabellen- und Spaltennamen aus V2__nutzer.sql.
    private void insertUser(String email, String role) {
        jdbcTemplate.update(
                "insert into nutzer (name, email, passwort_hash, rolle, registriert_am) values (?, ?, ?, ?, now())",
                "Test User", email, "hash", role);
    }
}