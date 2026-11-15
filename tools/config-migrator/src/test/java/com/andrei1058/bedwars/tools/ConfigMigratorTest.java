package com.andrei1058.bedwars.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class ConfigMigratorTest {
    @Test void ignoresOtherLanguagesWithoutChangingTheirExistingFiles() throws Exception {
        Path source = Files.createDirectories(temp.resolve("chinese-only-source"));
        Path target = Files.createDirectories(temp.resolve("chinese-only-target"));
        Files.createDirectories(source.resolve("Languages"));
        Files.createDirectories(target.resolve("Languages"));
        Files.writeString(source.resolve("Languages/messages_en.yml"), "custom: source\n");
        Files.writeString(target.resolve("Languages/messages_en.yml"), "custom: retained\n");
        ConfigMigrator.main(new String[]{source.toString(), target.toString()});
        assertEquals("custom: retained\n", Files.readString(target.resolve("Languages/messages_en.yml")));
        assertTrue(Files.readString(target.resolve("migration-report.txt")).contains("仅支持简体中文"));
    }
    @Test void normalizesCoordinateListsForNewAndExistingArenas() throws Exception {
        Path source = Files.createDirectories(temp.resolve("coords-source/Arenas"));
        Path target = Files.createDirectories(temp.resolve("coords-target/Arenas"));
        String arena = "Team:\n  Red:\n    Spawn: '1.25,64.75,-3.5,90,20'\n    Iron:\n      - '1,2,3'\n      - '[4,5,6,180,30]'\n    Gold:\n      - '7,8,9'\ngenerator:\n  Diamond:\n    - '10,11,12,90,0'\ncustom: '1,2,3,90,20'\n";
        Files.writeString(source.resolve("new.yml"), arena);
        Files.writeString(source.resolve("existing.yml"), arena);
        Files.writeString(target.resolve("existing.yml"), arena);
        ConfigMigrator.main(new String[]{source.getParent().toString(), target.getParent().toString()});
        org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
        for (String name : new String[]{"new.yml", "existing.yml"}) {
            java.util.Map<?,?> result = yaml.load(Files.readString(target.resolve(name)));
            java.util.Map<?,?> red = (java.util.Map<?,?>)((java.util.Map<?,?>) result.get("Team")).get("Red");
            assertEquals("1.25,64.75,-3.5", red.get("Spawn"));
            assertEquals(java.util.List.of("1,2,3", "4,5,6"), red.get("Iron"));
            assertEquals(java.util.List.of("10,11,12"), ((java.util.Map<?,?>)result.get("generator")).get("Diamond"));
            assertEquals("1,2,3,90,20", result.get("custom"));
        }
        String once = Files.readString(target.resolve("existing.yml"));
        ConfigMigrator.main(new String[]{source.getParent().toString(), target.getParent().toString()});
        assertEquals(once, Files.readString(target.resolve("existing.yml")));
    }

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
        Files.writeString(sourceDir.resolve("Languages/messages_zh_cn.yml"), "existing: modern\nnew-key: migrated\nrejoin:\n  waiting: new text\n");
        Files.writeString(targetDir.resolve("Languages/messages_zh_cn.yml"), "existing: custom\nrejoin: legacy custom text\n");
        Files.writeString(sourceDir.resolve("Cache/map.zip.yml"), "should: skip\n");
        ConfigMigrator.main(new String[]{sourceDir.toString(), targetDir.toString()});
        String migrated = Files.readString(targetDir.resolve("Languages/messages_zh_cn.yml"));
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
        assertTrue(migrated.contains("spawn: 1,2,3"));
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

    @Test void retainsUpgradePriceSourceVersionAndResetsTargetMigrationMarker() throws Exception {
        Path sourceDir = temp.resolve("modern"), targetDir = temp.resolve("legacy");
        Files.createDirectories(sourceDir); Files.createDirectories(targetDir);
        Files.writeString(sourceDir.resolve("upgrades2.yml"), "config-version: 9\nupgrade-swords:\n  tier-1:\n    cost: 2\n");
        Files.writeString(targetDir.resolve("upgrades2.yml"), "price-schema: 2\nupgrade-swords:\n  tier-1:\n    cost: 4\n");
        ConfigMigrator.main(new String[]{sourceDir.toString(), targetDir.toString()});
        String migrated = Files.readString(targetDir.resolve("upgrades2.yml"));
        assertTrue(migrated.contains("config-version: 9"));
        assertTrue(migrated.contains("price-schema: 0"));
        assertTrue(migrated.contains("cost: 2"));
        assertTrue(Files.readString(targetDir.resolve("migration-report.txt")).contains("保留升级价格来源"));
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
        assertTrue(migrated.contains("data: 0") || migrated.contains("data: 0.0"));
        assertTrue(migrated.contains("data: 3") || migrated.contains("data: 3.0"));
        assertFalse(migrated.contains("profile: player-texture"));
        assertTrue(report.contains("head.profile"));
        assertTrue(report.contains("自动映射为 5"));
        assertTrue(report.contains("自动映射为 0"));
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

    @Test void migratesModernArenaGroupsForBothExistingAndNewMapsAndRegistersReferences() throws Exception {
        Path source = Files.createDirectories(temp.resolve("groups-source/Arenas")).getParent();
        Path target = Files.createDirectories(temp.resolve("groups-target/Arenas")).getParent();
        Files.writeString(source.resolve("config.yml"), "arenaGroups: [Solo, Doubles]\nlobbyServer: login\n");
        Files.writeString(target.resolve("config.yml"), "arenaGroups: [LegacyOnly, solo]\nlobbyServer: hub\n");
        Files.writeString(source.resolve("Arenas/existing.yml"), "groups: ['', Solo, Doubles]\ngroup: WrongOldValue\ncustom: source\n");
        Files.writeString(target.resolve("Arenas/existing.yml"), "group: LegacyOnly\ngroups: [Stale]\ncustom: target\n");
        Files.writeString(source.resolve("Arenas/new.yml"), "groups: [ImportedOnly, Solo]\ngroup: WrongOldValue\n");
        Files.writeString(source.resolve("Arenas/scalar.yml"), "groups: doubles\n");
        Files.writeString(source.resolve("Arenas/current.yml"), "group: Solo\n");
        Files.writeString(target.resolve("Arenas/current.yml"), "custom: keep\n");
        ConfigMigrator.main(new String[]{source.toString(), target.toString()});
        org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
        java.util.Map<?,?> main = yaml.load(Files.readString(target.resolve("config.yml")));
        assertEquals(java.util.List.of("LegacyOnly", "solo", "Doubles", "ImportedOnly"), main.get("arenaGroups"));
        assertEquals("login", main.get("lobbyServer"));
        for (String[] row : new String[][]{{"existing", "solo"}, {"new", "ImportedOnly"}, {"scalar", "Doubles"}, {"current", "solo"}}) {
            java.util.Map<?,?> arena = yaml.load(Files.readString(target.resolve("Arenas/" + row[0] + ".yml")));
            assertEquals(row[1], arena.get("group"));
            assertFalse(arena.containsKey("groups"));
        }
        String before = Files.readString(target.resolve("config.yml"));
        ConfigMigrator.main(new String[]{source.toString(), target.toString()});
        assertEquals(before, Files.readString(target.resolve("config.yml")));
        assertTrue(Files.readString(target.resolve("migration-report.txt")).contains("竞技场分组转换为 group: Solo"));
    }

    @Test void convertsAliasGroupOnExistingMapEvenWithoutGroupTemplateKey() throws Exception {
        Path source = Files.createDirectories(temp.resolve("alias-source/Arenas")).getParent();
        Path target = Files.createDirectories(temp.resolve("alias-target/Arenas")).getParent();
        Files.writeString(source.resolve("Arenas/map.yml"), "arena-group: Solo\n");
        Files.writeString(target.resolve("Arenas/map.yml"), "custom: retained\n");
        Files.writeString(target.resolve("config.yml"), "lobbyServer: login\n");
        ConfigMigrator.main(new String[]{source.toString(), target.toString()});
        java.util.Map<?,?> arena = new org.yaml.snakeyaml.Yaml().load(Files.readString(target.resolve("Arenas/map.yml")));
        assertEquals("Solo", arena.get("group"));assertEquals("retained", arena.get("custom"));assertFalse(arena.containsKey("arena-group"));
        java.util.Map<?,?> main = new org.yaml.snakeyaml.Yaml().load(Files.readString(target.resolve("config.yml")));
        assertEquals(java.util.List.of("Solo"), main.get("arenaGroups"));assertEquals("login", main.get("lobbyServer"));
        assertFalse(Files.readString(target.resolve("migration-report.txt")).contains("group (目标版无对应配置项"));
    }
}
