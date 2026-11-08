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
        assertTrue(report.contains("目标版默认值"));
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

    @Test void mapsArenaGroupsAndLobbyToLegacyConfig() throws Exception {
        Path sourceDir = temp.resolve("modern"), targetDir = temp.resolve("legacy");
        Files.createDirectories(sourceDir); Files.createDirectories(targetDir);
        Files.writeString(sourceDir.resolve("config.yml"),
                "lobbyLoc: '[1,2,3,90,0,world]'\n"
                        + "arenaGroups:\n  - 1v1\n  - 2v2\n");
        Files.writeString(targetDir.resolve("config.yml"),
                "lobbyLoc: ''\narenaGroups:\n  - Default\nlobbyServer: hub\n");

        ConfigMigrator.main(new String[]{sourceDir.toString(), targetDir.toString()});

        String migrated = Files.readString(targetDir.resolve("config.yml"));
        String report = Files.readString(targetDir.resolve("migration-report.txt"));
        assertTrue(migrated.contains("arenaGroups:"));
        assertTrue(migrated.contains("- 1v1"));
        assertTrue(migrated.contains("- 2v2"));
        assertTrue(migrated.contains("lobbyLoc:") && migrated.contains("1,2,3,90,0,world"));
        assertFalse(report.contains("lobbyLoc 已忽略"));
    }

    @Test void migratesAllSupportedSwordUpgradeTiers() throws Exception {
        Path sourceDir = temp.resolve("modern"), targetDir = temp.resolve("legacy");
        Files.createDirectories(sourceDir); Files.createDirectories(targetDir);
        Files.writeString(sourceDir.resolve("upgrades2.yml"),
                "upgrade-swords:\n"
                        + "  tier-2:\n    cost: 8\n    currency: diamond\n    display-item:\n      material: IRON_SWORD\n      data: 0\n      amount: 2\n      enchanted: false\n    receive:\n      - 'enchant-item: DAMAGE_ALL,2,sword'\n"
                        + "  tier-3:\n    cost: 16\n    currency: diamond\n    display-item:\n      material: IRON_SWORD\n      data: 0\n      amount: 3\n      enchanted: false\n    receive:\n      - 'enchant-item: DAMAGE_ALL,3,sword'\n"
                        + "  tier-4:\n    cost: 32\n    currency: diamond\n    display-item:\n      material: IRON_SWORD\n      data: 0\n      amount: 4\n      enchanted: false\n    receive:\n      - 'enchant-item: DAMAGE_ALL,4,sword'\n");
        Files.writeString(targetDir.resolve("upgrades2.yml"),
                "upgrade-swords:\n  tier-2: {}\n  tier-3: {}\n  tier-4: {}\n");

        ConfigMigrator.main(new String[]{sourceDir.toString(), targetDir.toString()});

        String migrated = Files.readString(targetDir.resolve("upgrades2.yml"));
        assertTrue(migrated.contains("cost: 8"));
        assertTrue(migrated.contains("cost: 16"));
        assertTrue(migrated.contains("cost: 32"));
        assertTrue(migrated.contains("DAMAGE_ALL,4,sword"));
    }

    @Test void convertsLegacyMaterialDataAlongsideMaterialNames() throws Exception {
        Path sourceDir = temp.resolve("modern"), targetDir = temp.resolve("legacy");
        Files.createDirectories(sourceDir); Files.createDirectories(targetDir);
        Files.writeString(sourceDir.resolve("config.yml"),
                "waiting:\n  material: LIME_CONCRETE\n"
                        + "bed:\n  material: RED_BED\n"
                        + "head:\n  material: PLAYER_HEAD\n  profile: player-texture\n");
        Files.writeString(targetDir.resolve("config.yml"),
                "waiting:\n  material: STAINED_GLASS_PANE\n"
                        + "bed:\n  material: BED\n"
                        + "head:\n  material: SKULL_ITEM\n");

        ConfigMigrator.main(new String[]{sourceDir.toString(), targetDir.toString()});

        String migrated = Files.readString(targetDir.resolve("config.yml"));
        String report = Files.readString(targetDir.resolve("migration-report.txt"));
        assertTrue(migrated.contains("material: STAINED_GLASS_PANE"));
        assertTrue(migrated.contains("material: BED"));
        assertTrue(migrated.contains("material: SKULL_ITEM"));
        assertTrue(migrated.contains("data: 5") || migrated.contains("data: 5.0"));
        assertTrue(migrated.contains("data: 14") || migrated.contains("data: 14.0"));
        assertTrue(migrated.contains("data: 3") || migrated.contains("data: 3.0"));
        assertFalse(migrated.contains("profile: player-texture"));
        assertTrue(report.contains("head.profile"));
        assertTrue(report.contains("自动映射为 5"));
        assertTrue(report.contains("自动映射为 14"));
        assertTrue(report.contains("自动映射为 3"));
    }

    @Test void keepsLegacyShopDefaultsForUnsupportedModernItems() throws Exception {
        Path sourceDir = temp.resolve("modern"), targetDir = temp.resolve("legacy");
        Files.createDirectories(sourceDir); Files.createDirectories(targetDir);
        Files.writeString(sourceDir.resolve("shop.yml"),
                "utility-category:\n  category-content:\n    recall-scroll:\n      content-tiers:\n        tier1:\n          buy-items:\n            scroll:\n              bedwars-item: recall-scroll\n    self-rescue-platform:\n      content-tiers:\n        tier1:\n          buy-items:\n            self-rescue-platform:\n              bedwars-item:\n                material: BLAZE_ROD\n");
        Files.writeString(targetDir.resolve("shop.yml"),
                "utility-category:\n  category-content:\n    recall-scroll: false\n    self-rescue-platform:\n      content-tiers:\n        tier1:\n          buy-items:\n            self-rescue-platform:\n              material: BLAZE_ROD\n              custom-data: 1058\n");

        ConfigMigrator.main(new String[]{sourceDir.toString(), targetDir.toString()});

        String migrated = Files.readString(targetDir.resolve("shop.yml"));
        String report = Files.readString(targetDir.resolve("migration-report.txt"));
        assertTrue(migrated.contains("recall-scroll: false"));
        assertTrue(migrated.contains("material: BLAZE_ROD"));
        assertTrue(migrated.contains("custom-data: 1058"));
        assertTrue(report.contains("recall-scroll"));
        assertTrue(report.contains("bedwars-item"));
    }
}
