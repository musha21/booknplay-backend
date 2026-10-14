package lk.booknplay.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Idempotent, MySQL-only migration that adds the court-level {@code facility_type} column and
 * backfills it only where the owning venue's {@code venue_type} clearly indicates indoor/outdoor.
 * Ambiguous rows are left NULL so owners can confirm them in the court editor; the DB-level
 * NOT NULL constraint is deliberately deferred until the catalog is fully backfilled.
 */
@Component
@Order(11)
public class CourtFacilityMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CourtFacilityMigrationRunner.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbc;

    public CourtFacilityMigrationRunner(DataSource dataSource, JdbcTemplate jdbc) {
        this.dataSource = dataSource;
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!isMysql()) {
            return;
        }
        try {
            ensureColumn();
            backfill();
        } catch (Exception ex) {
            log.error("Court facility migration failed: {}", ex.getMessage(), ex);
        }
    }

    private boolean isMysql() {
        try (Connection connection = dataSource.getConnection()) {
            String product = connection.getMetaData().getDatabaseProductName();
            if (product == null || !product.toLowerCase().contains("mysql")) {
                log.info("Skipping court facility migration (database={})", product);
                return false;
            }
            return true;
        } catch (Exception ex) {
            log.warn("Could not detect database product; skipping court facility migration: {}", ex.getMessage());
            return false;
        }
    }

    private void ensureColumn() {
        Integer columnCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = 'courts' AND column_name = 'facility_type'",
                Integer.class);
        if (columnCount == null || columnCount == 0) {
            jdbc.execute("ALTER TABLE courts ADD COLUMN facility_type VARCHAR(20) NULL");
            log.info("Added courts.facility_type column (nullable)");
        }
    }

    private void backfill() {
        int indoor = jdbc.update(
                "UPDATE courts c JOIN venues v ON c.venue_id = v.id "
                        + "SET c.facility_type = 'INDOOR' "
                        + "WHERE c.facility_type IS NULL AND LOWER(v.venue_type) LIKE '%indoor%'");
        int outdoor = jdbc.update(
                "UPDATE courts c JOIN venues v ON c.venue_id = v.id "
                        + "SET c.facility_type = 'OUTDOOR' "
                        + "WHERE c.facility_type IS NULL AND LOWER(v.venue_type) LIKE '%outdoor%'");
        if (indoor > 0 || outdoor > 0) {
            log.info("Backfilled courts.facility_type from venue_type: {} indoor, {} outdoor", indoor, outdoor);
        }
    }
}
