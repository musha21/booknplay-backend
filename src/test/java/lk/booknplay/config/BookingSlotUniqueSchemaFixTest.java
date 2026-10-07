package lk.booknplay.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingSlotUniqueSchemaFixTest {

    @Mock private DataSource dataSource;
    @Mock private JdbcTemplate jdbc;
    @Mock private Connection connection;
    @Mock private DatabaseMetaData metaData;

    @Test
    void dropsLegacyUkCourtSlotAndAddsActiveUniqueOnMysql() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("MySQL");
        when(jdbc.queryForObject(ArgumentMatchers.contains("index_name"), eq(Integer.class), eq("uk_court_slot")))
                .thenReturn(1);
        when(jdbc.queryForObject(ArgumentMatchers.contains("column_name = 'active_court_slot'"), eq(Integer.class)))
                .thenReturn(0);
        when(jdbc.queryForObject(ArgumentMatchers.contains("index_name = 'uk_active_court_slot'"), eq(Integer.class)))
                .thenReturn(0);

        new BookingSlotUniqueSchemaFix(dataSource, jdbc).run(new DefaultApplicationArguments(new String[0]));

        verify(jdbc).execute("ALTER TABLE bookings DROP INDEX `uk_court_slot`");
        verify(jdbc, atLeastOnce()).execute(ArgumentMatchers.contains("ADD COLUMN active_court_slot"));
        verify(jdbc).execute("ALTER TABLE bookings ADD UNIQUE KEY uk_active_court_slot (active_court_slot)");
    }

    @Test
    void skipsNonMysqlDatabases() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("H2");

        new BookingSlotUniqueSchemaFix(dataSource, jdbc).run(new DefaultApplicationArguments(new String[0]));

        verify(jdbc, never()).execute(anyString());
    }
}
