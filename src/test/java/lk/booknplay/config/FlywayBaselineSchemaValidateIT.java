package lk.booknplay.config;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs only when FLYWAY_DISPOSABLE_VALIDATE=true against booknplay_flyway_test.
 * Confirms Flyway V1 + Hibernate ddl-auto=validate. Never targets production booknplay.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.open-in-view=false"
})
@EnabledIfEnvironmentVariable(named = "FLYWAY_DISPOSABLE_VALIDATE", matches = "true")
class FlywayBaselineSchemaValidateIT {

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        String url = System.getenv().getOrDefault(
                "FLYWAY_TEST_JDBC_URL",
                "jdbc:mysql://localhost:3306/booknplay_flyway_test?useSSL=false&serverTimezone=Asia/Colombo&allowPublicKeyRetrieval=true");
        String user = System.getenv().getOrDefault("FLYWAY_TEST_USER", "booknplay_flyway");
        String pass = System.getenv().getOrDefault("FLYWAY_TEST_PASSWORD", "flyway_test_only");
        Assumptions.assumeTrue(url.contains("booknplay_flyway_test"), "Refusing non-disposable JDBC URL");
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", () -> user);
        registry.add("spring.datasource.password", () -> pass);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    }

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void flywayBaselineMatchesHibernateValidate() {
        Integer tables = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE()",
                Integer.class);
        assertThat(tables).isGreaterThanOrEqualTo(34);

        Integer activeCourtSlot = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = 'bookings' AND column_name = 'active_court_slot'",
                Integer.class);
        assertThat(activeCourtSlot).isEqualTo(1);

        Integer nameNorm = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = 'sports' AND column_name = 'name_norm'",
                Integer.class);
        assertThat(nameNorm).isEqualTo(1);

        Integer customerNullable = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = 'bookings' "
                        + "AND column_name = 'customer_id' AND is_nullable = 'YES'",
                Integer.class);
        assertThat(customerNullable).isEqualTo(1);
    }
}
