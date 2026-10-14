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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Idempotent, MySQL-only migration that produces a clean, unique global sports catalog:
 * <ol>
 *   <li>Renames legacy seeded labels to plain canonical names (e.g. "Indoor Cricket" -> "Cricket",
 *       "Futsal / Indoor Football" -> "Futsal"). If the canonical target already exists the old
 *       row is merged into it instead of renamed.</li>
 *   <li>Merges historical duplicates grouped by {@code LOWER(TRIM(name))}: picks a survivor,
 *       repoints the sport foreign keys on courts/bookings/booking_quotes, then deletes the dupes.</li>
 *   <li>Adds a generated {@code sports.name_norm} column plus a unique key so the catalog can no
 *       longer contain case/whitespace-variant duplicates.</li>
 * </ol>
 * Distinct sports (Football vs Futsal) are never merged - only exact/alias name collisions.
 */
@Component
@Order(10)
public class SportsCatalogMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SportsCatalogMigrationRunner.class);

    /** legacy/seeded label -> clean canonical name. */
    private static final Map<String, String> ALIAS_RENAMES = new LinkedHashMap<>();
    static {
        ALIAS_RENAMES.put("Indoor Cricket", "Cricket");
        ALIAS_RENAMES.put("Futsal / Indoor Football", "Futsal");
        ALIAS_RENAMES.put("Football Field", "Football");
        ALIAS_RENAMES.put("Tennis Court", "Tennis");
    }

    private final DataSource dataSource;
    private final JdbcTemplate jdbc;

    public SportsCatalogMigrationRunner(DataSource dataSource, JdbcTemplate jdbc) {
        this.dataSource = dataSource;
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!isMysql()) {
            return;
        }
        try {
            applyAliasRenames();
            mergeDuplicateNames();
            ensureNameNormUnique();
        } catch (Exception ex) {
            log.error("Sports catalog migration failed: {}", ex.getMessage(), ex);
        }
    }

    private boolean isMysql() {
        try (Connection connection = dataSource.getConnection()) {
            String product = connection.getMetaData().getDatabaseProductName();
            if (product == null || !product.toLowerCase().contains("mysql")) {
                log.info("Skipping sports catalog migration (database={})", product);
                return false;
            }
            return true;
        } catch (Exception ex) {
            log.warn("Could not detect database product; skipping sports catalog migration: {}", ex.getMessage());
            return false;
        }
    }

    private void applyAliasRenames() {
        for (Map.Entry<String, String> entry : ALIAS_RENAMES.entrySet()) {
            String oldName = entry.getKey();
            String canonical = entry.getValue();
            List<String> oldIds = jdbc.queryForList(
                    "SELECT id FROM sports WHERE LOWER(TRIM(name)) = LOWER(TRIM(?))",
                    String.class, oldName);
            if (oldIds.isEmpty()) {
                continue;
            }
            List<String> targetIds = jdbc.queryForList(
                    "SELECT id FROM sports WHERE LOWER(TRIM(name)) = LOWER(TRIM(?))",
                    String.class, canonical);
            if (targetIds.isEmpty()) {
                // Safe rename of the first; merge the rest into it.
                String survivor = oldIds.get(0);
                jdbc.update("UPDATE sports SET name = ? WHERE id = ?", canonical, survivor);
                for (int i = 1; i < oldIds.size(); i++) {
                    repointAndDelete(survivor, oldIds.get(i));
                }
                log.info("Renamed sport '{}' -> '{}' ({} row(s) consolidated)", oldName, canonical, oldIds.size());
            } else {
                String survivor = targetIds.get(0);
                for (String dup : oldIds) {
                    repointAndDelete(survivor, dup);
                }
                log.info("Merged sport '{}' into existing '{}' ({} row(s))", oldName, canonical, oldIds.size());
            }
        }
    }

    private void mergeDuplicateNames() {
        List<Map<String, Object>> groups = jdbc.queryForList(
                "SELECT LOWER(TRIM(name)) AS norm, COUNT(*) AS cnt FROM sports "
                        + "GROUP BY LOWER(TRIM(name)) HAVING COUNT(*) > 1");
        for (Map<String, Object> group : groups) {
            String norm = (String) group.get("norm");
            List<String> ids = jdbc.queryForList(
                    "SELECT id FROM sports WHERE LOWER(TRIM(name)) = ? ORDER BY id",
                    String.class, norm);
            if (ids.size() < 2) {
                continue;
            }
            String survivor = pickSurvivor(norm, ids);
            for (String dup : ids) {
                if (!dup.equals(survivor)) {
                    repointAndDelete(survivor, dup);
                }
            }
            log.info("Merged duplicate sports for '{}': kept {}, removed {}", norm, survivor, ids.size() - 1);
        }
    }

    private String pickSurvivor(String norm, List<String> ids) {
        // Prefer a row whose name already matches a clean canonical label (title-case of norm).
        String pretty = toPretty(norm);
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, name FROM sports WHERE LOWER(TRIM(name)) = ?", norm);
        for (Map<String, Object> row : rows) {
            String id = (String) row.get("id");
            String name = row.get("name") == null ? "" : row.get("name").toString().trim();
            if (ids.contains(id) && name.equalsIgnoreCase(pretty)) {
                return id;
            }
        }
        return ids.get(0);
    }

    private void repointAndDelete(String survivorId, String duplicateId) {
        if (survivorId.equals(duplicateId)) {
            return;
        }
        jdbc.update("UPDATE courts SET sport_id = ? WHERE sport_id = ?", survivorId, duplicateId);
        jdbc.update("UPDATE bookings SET sport_id = ? WHERE sport_id = ?", survivorId, duplicateId);
        jdbc.update("UPDATE booking_quotes SET sport_id = ? WHERE sport_id = ?", survivorId, duplicateId);
        jdbc.update("DELETE FROM sports WHERE id = ?", duplicateId);
    }

    private void ensureNameNormUnique() {
        Integer columnCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = 'sports' AND column_name = 'name_norm'",
                Integer.class);
        if (columnCount == null || columnCount == 0) {
            jdbc.execute("ALTER TABLE sports ADD COLUMN name_norm VARCHAR(255) "
                    + "GENERATED ALWAYS AS (LOWER(TRIM(name))) STORED");
            log.info("Added sports.name_norm generated column");
        }

        Integer indexCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.statistics "
                        + "WHERE table_schema = DATABASE() AND table_name = 'sports' AND index_name = 'uk_sports_name_norm'",
                Integer.class);
        if (indexCount == null || indexCount == 0) {
            jdbc.execute("ALTER TABLE sports ADD UNIQUE KEY uk_sports_name_norm (name_norm)");
            log.info("Added sports.uk_sports_name_norm unique key");
        }
    }

    private static String toPretty(String norm) {
        if (norm == null || norm.isBlank()) {
            return norm;
        }
        String[] words = norm.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            String w = words[i];
            if (!w.isEmpty()) {
                sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
            }
        }
        return sb.toString();
    }
}
