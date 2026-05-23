package com.kstrinadka.securebankapi.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MigrationIntegrationTest extends AbstractIntegrationTest {

    private static final List<String> EXPECTED_TABLES = List.of(
            "users",
            "account",
            "email_data",
            "phone_data",
            "transfer"
    );

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayCreatesRequiredTables() {
        for (String tableName : EXPECTED_TABLES) {
            Boolean exists = jdbcTemplate.queryForObject(
                    "SELECT to_regclass(?) IS NOT NULL",
                    Boolean.class,
                    "public." + tableName
            );

            assertThat(exists)
                    .as("Table %s should exist", tableName)
                    .isTrue();
        }
    }

    @Test
    void flywayLoadsTestUsers() {
        Integer usersCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users",
                Integer.class
        );
        Integer accountsCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM account",
                Integer.class
        );
        Integer emailsCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM email_data",
                Integer.class
        );
        Integer phonesCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM phone_data",
                Integer.class
        );

        assertThat(usersCount).isEqualTo(3);
        assertThat(accountsCount).isEqualTo(3);
        assertThat(emailsCount).isEqualTo(3);
        assertThat(phonesCount).isEqualTo(3);
    }
}
