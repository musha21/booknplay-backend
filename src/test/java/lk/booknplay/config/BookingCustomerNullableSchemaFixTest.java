package lk.booknplay.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingCustomerNullableSchemaFixTest {

    @Mock private DataSource dataSource;
    @Mock private JdbcTemplate jdbc;
    @Mock private Connection connection;
    @Mock private DatabaseMetaData metaData;

    @Test
    void altersCustomerIdWhenNotNullableOnMysql() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("MySQL");
        when(jdbc.queryForObject(contains("customer_id"), org.mockito.ArgumentMatchers.eq(Integer.class)))
                .thenReturn(1);

        new BookingCustomerNullableSchemaFix(dataSource, jdbc).run(new DefaultApplicationArguments());

        verify(jdbc).execute("ALTER TABLE bookings MODIFY customer_id VARCHAR(36) NULL");
    }

    @Test
    void skipsWhenAlreadyNullable() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("MySQL");
        when(jdbc.queryForObject(contains("customer_id"), org.mockito.ArgumentMatchers.eq(Integer.class)))
                .thenReturn(0);

        new BookingCustomerNullableSchemaFix(dataSource, jdbc).run(new DefaultApplicationArguments());

        verify(jdbc, never()).execute(anyString());
    }
}
