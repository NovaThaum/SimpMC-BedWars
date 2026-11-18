package com.andrei1058.bedwars.database;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.configuration.MainConfig;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.sql.DriverManager;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MySqlDriverTest {
    @Test void poolExplicitlyUsesBundledDriverDespiteServerLegacyDriverRegistration() throws Exception {
        MainConfig previous = BedWars.config;
        MainConfig config = mock(MainConfig.class); YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("database.host", "db.example.invalid"); yaml.set("database.port", 3307);
        yaml.set("database.database", "custom_db"); yaml.set("database.user", "custom_user");
        yaml.set("database.pass", "test-only-password"); yaml.set("database.ssl", true);
        yaml.set("database.verify-certificate", true); yaml.set("database.pool-size", 4);
        when(config.getYml()).thenReturn(yaml); BedWars.config = config;
        java.sql.Driver old = mock(java.sql.Driver.class);
        when(old.acceptsURL(anyString())).thenReturn(true); DriverManager.registerDriver(old);
        try {
            var pool = new MySQL().createPoolConfig();
            assertEquals(com.mysql.cj.jdbc.Driver.class.getName(), pool.getDriverClassName());
            assertEquals("jdbc:mysql://db.example.invalid:3307/custom_db", pool.getJdbcUrl());
            assertEquals("custom_user", pool.getUsername()); assertEquals("test-only-password", pool.getPassword());
            assertEquals(4, pool.getMaximumPoolSize());
            assertEquals("true", pool.getDataSourceProperties().getProperty("useSSL"));
            assertNull(pool.getDataSourceProperties().getProperty("allowPublicKeyRetrieval"));
            assertNotNull(new com.mysql.cj.protocol.a.authentication.CachingSha2PasswordPlugin());
            assertNotNull(new com.mysql.cj.protocol.a.authentication.Sha256PasswordPlugin());
            verify(old, never()).connect(anyString(), any());
        } finally { DriverManager.deregisterDriver(old); BedWars.config = previous; }
    }
}
