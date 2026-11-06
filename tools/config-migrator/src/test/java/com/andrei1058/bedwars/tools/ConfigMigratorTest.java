package com.andrei1058.bedwars.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class ConfigMigratorTest {
    @TempDir Path temp;

    @Test void mergesOnlyKnownTargetPathsAndReportsLegacyConversions() throws Exception {
        Path source = temp.resolve("modern.yml"), template = temp.resolve("legacy.yml"), output = temp.resolve("out");
        Files.writeString(source, "config-version: 37\nlobbyServer: proxy-hub\ngui:\n  material: PLAYER_HEAD\nsounds:\n  step: BLOCK_WOOL_STEP\npaper-only: true\n");
        Files.writeString(template, "lobbyServer: hub\ngui:\n  material: SKULL_ITEM\nsounds:\n  step: STEP_STONE\nlegacy-only: 9\n");
        ConfigMigrator.main(new String[]{source.toString(), template.toString(), output.toString()});
        String migrated = Files.readString(output.resolve("legacy.yml"));
        String report = Files.readString(output.resolve("migration-report.txt"));
        assertTrue(migrated.contains("lobbyServer: proxy-hub"));
        assertTrue(migrated.contains("material: SKULL_ITEM"));
        assertTrue(migrated.contains("step: STEP_WOOL"));
        assertTrue(migrated.contains("legacy-only: 9"));
        assertFalse(migrated.contains("config-version"));
        assertFalse(migrated.contains("paper-only"));
        assertTrue(report.contains("PLAYER_HEAD -> SKULL_ITEM"));
        assertTrue(report.contains("config-version (由目标版自行管理)"));
        assertEquals("config-version: 37\nlobbyServer: proxy-hub\ngui:\n  material: PLAYER_HEAD\nsounds:\n  step: BLOCK_WOOL_STEP\npaper-only: true\n", Files.readString(source));
    }
}
