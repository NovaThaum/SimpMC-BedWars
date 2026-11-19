package com.andrei1058.bedwars.database;

import org.junit.jupiter.api.Test;
import java.sql.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StatsReadFailureTest {
    @Test void sqliteAcceptsNullHistoricalDatesAndPreservesCounters() throws Exception {
        SQLite database = mock(SQLite.class, CALLS_REAL_METHODS);
        Connection connection = mock(Connection.class);
        var field = SQLite.class.getDeclaredField("connection"); field.setAccessible(true); field.set(database, connection);
        PreparedStatement statement = mock(PreparedStatement.class); ResultSet rows = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(rows); when(rows.next()).thenReturn(true);
        when(rows.getInt("kills")).thenReturn(31);
        var stats = database.fetchStats(UUID.randomUUID());
        assertNull(stats.getFirstPlay()); assertNull(stats.getLastPlay()); assertEquals(31, stats.getKills());
        when(statement.executeQuery()).thenThrow(new SQLException("test read failure"));
        assertInstanceOf(SQLException.class, assertThrows(IllegalStateException.class,
                () -> database.fetchStats(UUID.randomUUID())).getCause());
    }

    @Test void mysqlReadFailureDoesNotReturnZeroStats() throws Exception {
        MySQL database = mock(MySQL.class, CALLS_REAL_METHODS);
        com.zaxxer.hikari.HikariDataSource pool = mock(com.zaxxer.hikari.HikariDataSource.class);
        var field = MySQL.class.getDeclaredField("dataSource"); field.setAccessible(true); field.set(database, pool);
        when(pool.getConnection()).thenThrow(new SQLException("test pool failure"));
        assertInstanceOf(SQLException.class, assertThrows(IllegalStateException.class,
                () -> database.fetchStats(UUID.randomUUID())).getCause());
    }
}
