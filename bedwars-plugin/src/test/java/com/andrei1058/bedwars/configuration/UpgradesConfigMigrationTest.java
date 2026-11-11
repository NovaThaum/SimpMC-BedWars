package com.andrei1058.bedwars.configuration;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import com.andrei1058.bedwars.BedWars;
import static org.mockito.Mockito.mock;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UpgradesConfigMigrationTest {

    @Test
    void startupMigrationSavesMissingTiersAndPreservesCustomFields(@TempDir Path directory) throws Exception {
        YamlConfiguration config = costs("upgrade-swords", 2, 4, 8, 16);
        config.set("config-version", 10);
        config.set("upgrade-swords.tier-2.display-item.material", "GOLD_SWORD");
        config.set("upgrade-armor.tier-1.cost", 7);
        config.set("upgrade-armor.tier-1.currency", "gold");
        config.set("upgrade-forge.tier-1.receive", java.util.Collections.singletonList("generator-edit: iron,5,1,20"));
        config.save(directory.resolve("upgrades2.yml").toFile());
        BedWars original = BedWars.plugin;
        BedWars.plugin = mock(BedWars.class);
        try {
            new UpgradesConfig("upgrades2", directory.toString());
            YamlConfiguration saved = YamlConfiguration.loadConfiguration(directory.resolve("upgrades2.yml").toFile());
            assertCosts(saved, "upgrade-swords", 4, 8, 16, 32);
            assertCosts(saved, "upgrade-armor", 7, 4, 10, 24);
            assertEquals("gold", saved.getString("upgrade-armor.tier-1.currency"));
            assertEquals("GOLD_SWORD", saved.getString("upgrade-swords.tier-2.display-item.material"));
            assertEquals(config.getStringList("upgrade-forge.tier-1.receive"), saved.getStringList("upgrade-forge.tier-1.receive"));
            assertEquals(2, saved.getInt("price-schema"));
            String first = saved.saveToString();
            new UpgradesConfig("upgrades2", directory.toString());
            assertEquals(first, YamlConfiguration.loadConfiguration(directory.resolve("upgrades2.yml").toFile()).saveToString());
        } finally {
            BedWars.plugin = original;
        }
    }

    @Test
    void migratesSchemaTenSwordPrices() {
        YamlConfiguration config = costs("upgrade-swords", 2, 4, 8, 16);
        UpgradesConfig.migrateLegacySwordTierCosts(config, 10);
        assertCosts(config, "upgrade-swords", 4, 8, 16, 32);
    }

    @Test
    void migratesSchemaNineAndEightSwordPrices() {
        YamlConfiguration schemaNine = costs("upgrade-swords", 2, 4, 8, 14);
        UpgradesConfig.migrateLegacySwordTierCosts(schemaNine, 9);
        assertCosts(schemaNine, "upgrade-swords", 4, 8, 16, 32);

        YamlConfiguration schemaEight = costs("upgrade-swords", 4, 6, 9, 14);
        UpgradesConfig.migrateLegacySwordTierCosts(schemaEight, 8);
        assertCosts(schemaEight, "upgrade-swords", 4, 8, 16, 32);
    }

    @Test
    void migratesOldArmorPricesWithoutTouchingCustomValues() {
        YamlConfiguration old = costs("upgrade-armor", 2, 4, 8, 16);
        UpgradesConfig.migrateLegacyArmorTierCosts(old, 9);
        assertCosts(old, "upgrade-armor", 2, 4, 10, 24);

        YamlConfiguration custom = costs("upgrade-armor", 2, 4, 8, 17);
        UpgradesConfig.migrateLegacyArmorTierCosts(custom, 9);
        assertCosts(custom, "upgrade-armor", 2, 4, 8, 17);
    }

    @Test
    void newerSchemasAndAlreadyMigratedValuesAreStable() {
        YamlConfiguration config = costs("upgrade-swords", 4, 8, 16, 32);
        UpgradesConfig.migrateLegacySwordTierCosts(config, 11);
        assertCosts(config, "upgrade-swords", 4, 8, 16, 32);
        UpgradesConfig.migrateLegacySwordTierCosts(config, 1);
        assertCosts(config, "upgrade-swords", 4, 8, 16, 32);
    }

    @Test
    void usesModernConfigVersionRatherThanLegacyPriceMarker() throws Exception {
        for (int schema = 8; schema <= 10; schema++) {
            int[] old = schema == 8 ? new int[]{4, 6, 9, 14}
                    : schema == 9 ? new int[]{2, 4, 8, 14} : new int[]{2, 4, 8, 16};
            YamlConfiguration config = costs("upgrade-swords", old);
            config.set("config-version", schema);
            config.set("price-schema", 1);
            UpgradesConfig.migratePrices(config);
            assertCosts(config, "upgrade-swords", 4, 8, 16, 32);
            assertEquals(2, config.getInt("price-schema"));
            YamlConfiguration reloaded = new YamlConfiguration();
            reloaded.loadFromString(config.saveToString());
            reloaded.set("upgrade-swords.tier-1.cost", 2);
            UpgradesConfig.migratePrices(reloaded);
            assertEquals(2, reloaded.getInt("upgrade-swords.tier-1.cost"));
        }
    }

    @Test
    void legacyMarkerOneDoesNotRepeatArmorMigrationOrGuessSwordSource() {
        YamlConfiguration config = costs("upgrade-armor", 2, 4, 8, 16);
        config.set("price-schema", 1);
        config.set("upgrade-swords.tier-1.cost", 2);
        config.set("upgrade-swords.tier-2.cost", 4);
        config.set("upgrade-swords.tier-3.cost", 8);
        config.set("upgrade-swords.tier-4.cost", 16);
        UpgradesConfig.migratePrices(config);
        assertCosts(config, "upgrade-armor", 2, 4, 8, 16);
        assertCosts(config, "upgrade-swords", 2, 4, 8, 16);
    }

    @Test
    void unsupportedFutureSchemasRemainUntouched() {
        YamlConfiguration config = costs("upgrade-armor", 2, 4, 8, 16);
        config.set("price-schema", 100);
        config.set("config-version", 8);
        String before = config.saveToString();
        UpgradesConfig.migratePrices(config);
        assertEquals(before, config.saveToString());
    }

    @Test
    void missingOrFractionalCostsAndCustomCurrenciesDoNotMatchOldDefaults() {
        YamlConfiguration missing = costs("upgrade-swords", 2, 4, 8);
        missing.addDefault("upgrade-swords.tier-4.cost", 16);
        UpgradesConfig.migrateLegacySwordTierCosts(missing, 10);
        assertEquals(2, missing.getInt("upgrade-swords.tier-1.cost"));
        YamlConfiguration fractional = costs("upgrade-armor", 2, 4, 8, 16);
        fractional.set("upgrade-armor.tier-4.cost", 16.5);
        UpgradesConfig.migrateLegacyArmorTierCosts(fractional, 0);
        assertEquals(16.5, fractional.getDouble("upgrade-armor.tier-4.cost"));
        YamlConfiguration gold = costs("upgrade-armor", 2, 4, 8, 16);
        gold.set("upgrade-armor.tier-1.currency", "gold");
        UpgradesConfig.migrateLegacyArmorTierCosts(gold, 0);
        assertCosts(gold, "upgrade-armor", 2, 4, 8, 16);
    }

    @Test
    void everyCustomSwordTierIsPreservedForEachHistoricalSchema() {
        for (int schema = 8; schema <= 10; schema++) {
            int[] old = schema == 8 ? new int[]{4, 6, 9, 14}
                    : schema == 9 ? new int[]{2, 4, 8, 14} : new int[]{2, 4, 8, 16};
            for (int tier = 0; tier < 4; tier++) {
                int[] custom = old.clone();
                custom[tier] += 3;
                YamlConfiguration config = costs("upgrade-swords", custom);
                config.set("config-version", schema);
                UpgradesConfig.migratePrices(config);
                assertCosts(config, "upgrade-swords", custom);
            }
        }
        YamlConfiguration legacy = costs("upgrade-armor", 2, 4, 8, 16);
        UpgradesConfig.migratePrices(legacy);
        assertCosts(legacy, "upgrade-armor", 2, 4, 10, 24);
        YamlConfiguration newer = costs("upgrade-swords", 2, 4, 8, 16);
        newer.set("config-version", 11);
        UpgradesConfig.migratePrices(newer);
        assertCosts(newer, "upgrade-swords", 2, 4, 8, 16);
    }

    private static YamlConfiguration costs(String section, int... values) {
        YamlConfiguration config = new YamlConfiguration();
        for (int i = 0; i < values.length; i++) config.set(section + ".tier-" + (i + 1) + ".cost", values[i]);
        return config;
    }

    private static void assertCosts(YamlConfiguration config, String section, int... values) {
        for (int i = 0; i < values.length; i++)
            assertEquals(values[i], config.getInt(section + ".tier-" + (i + 1) + ".cost"));
    }
}
