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
 * Live MySQL still had {@code uk_court_slot} on (court, date, start, end) even after JPA dropped it.
 * That blocked rebooking after CANCELLED/FAILED. Replace with a status-aware unique key.
 */
@Component
public class BookingSlotUniqueSchemaFix implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BookingSlotUniqueSchemaFix.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbc;

    public BookingSlotUniqueSchemaFix(DataSource dataSource, JdbcTemplate jdbc) {
        this.dataSource = dataSource;
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!isMysql()) {
            return;
        }
        dropIndexIfPresent("uk_court_slot");
        ensureActiveCourtSlotUnique();
    }

    private boolean isMysql() {
        try (Connection connection = dataSource.getConnection()) {
            String product = connection.getMetaData().getDatabaseProductName();
            if (product == null || !product.toLowerCase().contains("mysql")) {
                log.info("Skipping booking slot unique schema fix (database={})", product);
                return false;
            }
            return true;
        } catch (Exception ex) {
            log.warn("Could not detect database product; skipping slot unique schema fix: {}", ex.getMessage());
            return false;
        }
    }

    private void dropIndexIfPresent(String indexName) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.statistics "
                        + "WHERE table_schema = DATABASE() AND table_name = 'bookings' AND index_name = ?",
                Integer.class,
                indexName
        );
        if (count != null && count > 0) {
            jdbc.execute("ALTER TABLE bookings DROP INDEX `" + indexName + "`");
            log.info("Dropped obsolete bookings.{}", indexName);
        }
    }

    private void ensureActiveCourtSlotUnique() {
        Integer columnCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = 'bookings' AND column_name = 'active_court_slot'",
                Integer.class
        );
        if (columnCount == null || columnCount == 0) {
            jdbc.execute(
                    "ALTER TABLE bookings "
                            + "ADD COLUMN active_court_slot VARCHAR(120) "
                            + "GENERATED ALWAYS AS ("
                            + "CASE WHEN status IN ('PENDING','CONFIRMED') "
                            + "THEN CONCAT(court_id, '|', booking_date, '|', start_time, '|', end_time) "
                            + "ELSE NULL END"
                            + ") STORED"
            );
            log.info("Added bookings.active_court_slot generated column");
        }

        Integer indexCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.statistics "
                        + "WHERE table_schema = DATABASE() AND table_name = 'bookings' AND index_name = 'uk_active_court_slot'",
                Integer.class
        );
        if (indexCount == null || indexCount == 0) {
            jdbc.execute("ALTER TABLE bookings ADD UNIQUE KEY uk_active_court_slot (active_court_slot)");
            log.info("Added bookings.uk_active_court_slot unique key");
        }
    }
}
