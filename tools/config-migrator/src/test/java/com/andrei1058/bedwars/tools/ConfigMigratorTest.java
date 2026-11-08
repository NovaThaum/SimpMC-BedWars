package com.andrei1058.bedwars.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class ConfigMigratorTest {
    @TempDir Path temp;

    @Test void mergesOnlyKnownTargetPathsAndReportsLegacyConversions() throws Exception {
        Path sourceDir = temp.resolve("modern"), templateDir = temp.resolve("legacy");
        Files.createDirectories(sourceDir); Files.createDirectories(templateDir);
        Path source = sourceDir.resolve("config.yml"), template = templateDir.resolve("config.yml");
        Files.writeString(source, "config-version: 37\nlobbyServer: proxy-hub\ngui:\n  material: PLAYER_HEAD\nsounds:\n  step: BLOCK_WOOL_STEP\npaper-only: true\n");
        Files.writeString(template, "lobbyServer: hub\ngui:\n  material: SKULL_ITEM\nsounds:\n  step: STEP_STONE\nlegacy-only: 9\n");
        ConfigMigrator.main(new String[]{sourceDir.toString(), templateDir.toString()});
        String migrated = Files.readString(templateDir.resolve("config.yml"));
        String report = Files.readString(templateDir.resolve("migration-report.txt"));
        assertTrue(migrated.contains("lobbyServer: proxy-hub"));
        assertTrue(migrated.contains("material: SKULL_ITEM"));
        assertTrue(migrated.contains("step: STEP_WOOL"));
        assertTrue(migrated.contains("legacy-only: 9"));
        assertFalse(migrated.contains("config-version"));
        assertFalse(migrated.contains("paper-only"));
        assertTrue(report.contains("PLAYER_HEAD -> SKULL_ITEM"));
        assertTrue(report.contains("config-version 已忽略，由目标插件管理"));
        assertEquals("config-version: 37\nlobbyServer: proxy-hub\ngui:\n  material: PLAYER_HEAD\nsounds:\n  step: BLOCK_WOOL_STEP\npaper-only: true\n", Files.readString(source));
    }

    @Test void addsMissingLanguageScalarsAndSkipsCache() throws Exception {
        Path sourceDir = temp.resolve("modern"), targetDir = temp.resolve("legacy");
        Files.createDirectories(sourceDir.resolve("Languages")); Files.createDirectories(sourceDir.resolve("Cache"));
        Files.createDirectories(targetDir.resolve("Languages")); Files.createDirectories(targetDir.resolve("Cache"));
        Files.writeString(sourceDir.resolve("Languages/messages_en.yml"), "existing: modern\nnew-key: migrated\nrejoin:\n  waiting: new text\n");
        Files.writeString(targetDir.resolve("Languages/messages_en.yml"), "existing: custom\nrejoin: legacy custom text\n");
        Files.writeString(sourceDir.resolve("Cache/map.zip.yml"), "should: skip\n");
        ConfigMigrator.main(new String[]{sourceDir.toString(), targetDir.toString()});
        String migrated = Files.readString(targetDir.resolve("Languages/messages_en.yml"));
        String report = Files.readString(targetDir.resolve("migration-report.txt"));
        assertTrue(migrated.contains("existing: modern"));
        assertTrue(migrated.contains("new-key: migrated"));
        assertTrue(migrated.contains("rejoin: legacy custom text"));
        assertTrue(report.contains("旧版不支持/结构冲突") || report.contains("目标版默认值"));
        assertFalse(Files.exists(targetDir.resolve("Cache/map.zip.yml")));
        assertTrue(report.contains("Cache") && report.contains("map.zip.yml"));
    }

    @Test void createsMissingArenaFromModernFile() throws Exception {
        Path sourceDir = temp.resolve("modern"), targetDir = temp.resolve("legacy");
        Files.createDirectories(sourceDir.resolve("Arenas")); Files.createDirectories(targetDir);
        Files.writeString(sourceDir.resolve("Arenas/new.yml"), "config-version: 24\nminPlayers: 2\nminInTeam: 1\ngame-rules:\n  - doWeatherCycle:false\n");
        ConfigMigrator.main(new String[]{sourceDir.toString(), targetDir.toString()});
        Path output = targetDir.resolve("Arenas/new.yml");
        String migrated = Files.readString(output);
        assertTrue(migrated.contains("minPlayers: 2"));
        assertFalse(migrated.contains("minInTeam"));
        assertFalse(migrated.contains("config-version"));
        assertTrue(migrated.contains("doDaylightCycle:false"));
    }

    @Test void convertsModernLocationsAndTeamColorNames() throws Exception {
        Path sourceDir = temp.resolve("modern"), targetDir = temp.resolve("legacy");
        Files.createDirectories(sourceDir.resolve("Arenas")); Files.createDirectories(targetDir);
        Files.writeString(sourceDir.resolve("Arenas/arena.yml"), "team-color: Cyan\nspawn: '[1,2,3]'\n");
        ConfigMigrator.main(new String[]{sourceDir.toString(), targetDir.toString()});
        String migrated = Files.readString(targetDir.resolve("Arenas/arena.yml"));
        assertTrue(migrated.contains("team-color: AQUA"));
        assertTrue(migrated.contains("spawn: 1,2,3,0,0"));
    }
}
