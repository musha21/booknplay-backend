package lk.booknplay.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Walk-in bookings store guestName/guestPhone without a registered Customer.
 * Older MySQL schemas still had {@code bookings.customer_id NOT NULL}.
 */
@Component
public class BookingCustomerNullableSchemaFix implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BookingCustomerNullableSchemaFix.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbc;

    public BookingCustomerNullableSchemaFix(DataSource dataSource, JdbcTemplate jdbc) {
        this.dataSource = dataSource;
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!isMysql()) {
            return;
        }
        makeCustomerIdNullable();
    }

    private boolean isMysql() {
        try (Connection connection = dataSource.getConnection()) {
            String product = connection.getMetaData().getDatabaseProductName();
            if (product == null || !product.toLowerCase().contains("mysql")) {
                log.info("Skipping bookings.customer_id nullability fix (database={})", product);
                return false;
            }
            return true;
        } catch (Exception ex) {
            log.warn("Could not detect database product; skipping customer_id nullability fix: {}", ex.getMessage());
            return false;
        }
    }

    private void makeCustomerIdNullable() {
        Integer notNullable = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() "
                        + "AND table_name = 'bookings' "
                        + "AND column_name = 'customer_id' "
                        + "AND is_nullable = 'NO'",
                Integer.class
        );
        if (notNullable == null || notNullable == 0) {
            return;
        }
        jdbc.execute("ALTER TABLE bookings MODIFY customer_id VARCHAR(36) NULL");
        log.info("Altered bookings.customer_id to allow NULL for walk-in bookings");
    }
}
