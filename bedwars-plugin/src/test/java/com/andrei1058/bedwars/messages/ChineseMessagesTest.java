package com.andrei1058.bedwars.messages;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.messages.ChineseMessages;
import com.andrei1058.bedwars.api.messages.Messages;
import com.andrei1058.bedwars.database.Database;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChineseMessagesTest {
    @TempDir Path directory;

    @Test void loadsOnlyFixedChineseFilePreservesCustomTextAndFillsChineseDefaults() throws Exception {
        BedWars previousPlugin = BedWars.plugin;
        var field = ChineseMessages.class.getDeclaredField("instance"); field.setAccessible(true);
        Object previousMessages = field.get(null);
        BedWars.plugin = mock(BedWars.class);
        when(BedWars.plugin.getDataFolder()).thenReturn(directory.toFile());
        when(BedWars.plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        Path folder = Files.createDirectories(directory.resolve("Languages"));
        Files.writeString(folder.resolve("messages_zh_cn.yml"), "custom: 自定义中文\ncmd-join-usage: 自定义加入提示\n"
                + Messages.FORMATTING_ACTION_BAR_TRACKING + ": '&f正在追踪：{team} &f- 距离：{distance}m'\n");
        Files.writeString(folder.resolve("messages_en.yml"), "custom: English\n");
        Files.writeString(folder.resolve("messages_zh_cn.backup.yml"), "custom: 旧备份\n");
        var services = mock(org.bukkit.plugin.ServicesManager.class);
        @SuppressWarnings("unchecked")
        org.bukkit.plugin.RegisteredServiceProvider<com.andrei1058.bedwars.api.BedWars> registration =
                mock(org.bukkit.plugin.RegisteredServiceProvider.class);
        var api = mock(com.andrei1058.bedwars.api.BedWars.class, RETURNS_DEEP_STUBS);
        when(services.getRegistration(com.andrei1058.bedwars.api.BedWars.class)).thenReturn(registration);
        when(registration.getProvider()).thenReturn(api);
        try (var bukkit = mockStatic(org.bukkit.Bukkit.class)) {
            bukkit.when(org.bukkit.Bukkit::getServicesManager).thenReturn(services);
            ChineseMessages messages = new SimplifiedChinese();
            assertSame(messages, ChineseMessages.get());
            assertEquals("自定义中文", messages.getYml().getString("custom"));
            assertEquals("自定义加入提示", messages.getYml().getString("cmd-join-usage"));
            assertEquals("&f正在追踪：{player} &f- 距离：{distance}m",
                    messages.getYml().getString(Messages.FORMATTING_ACTION_BAR_TRACKING));
            assertTrue(messages.getYml().getString("match-history.not-found").contains("未找到"));
            messages.reload();
            assertEquals("自定义中文", messages.getYml().getString("custom"));
            assertEquals("custom: English\n", Files.readString(folder.resolve("messages_en.yml")));
            assertEquals("custom: 旧备份\n", Files.readString(folder.resolve("messages_zh_cn.backup.yml")));
            try (var files = Files.list(folder)) { assertEquals(3, files.count()); }
        } finally { BedWars.plugin = previousPlugin; field.set(null, previousMessages); }
    }

    @Test void polishesOnlyExactOldDefaultsAndPreservesCustomTextAndLists() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("old", "原默认"); yaml.set("custom", "管理员文本");
        yaml.set("lore", List.of("原说明")); yaml.set("custom-lore", List.of("管理员说明"));
        for (String key : List.of("missing", "old", "custom"))
            SimplifiedChinese.addPolishedDefault(yaml, key, "原默认", "新默认");
        SimplifiedChinese.addPolishedDefault(yaml, "lore", List.of("原说明"), List.of("新说明"));
        SimplifiedChinese.addPolishedDefault(yaml, "custom-lore", List.of("原说明"), List.of("新说明"));
        assertEquals("新默认", yaml.getString("missing"));
        assertEquals("新默认", yaml.getString("old"));
        assertEquals("管理员文本", yaml.getString("custom"));
        assertEquals(List.of("新说明"), yaml.getStringList("lore"));
        assertEquals(List.of("管理员说明"), yaml.getStringList("custom-lore"));
        SimplifiedChinese.addPolishedDefault(yaml, "old", "原默认", "新默认");
        assertEquals("新默认", yaml.getString("old"));
    }

    @Test void retiredI18nClassesAndCompatibilityMethodsAreAbsent() {
        assertThrows(ClassNotFoundException.class, () -> Class.forName("com.andrei1058.bedwars.api.language.Language"));
        assertThrows(ClassNotFoundException.class, () -> Class.forName("com.andrei1058.bedwars.api.events.player.PlayerLangChangeEvent"));
        for (Class<?> type : List.of(ChineseMessages.class, com.andrei1058.bedwars.api.BedWars.class,
                Database.class, com.andrei1058.bedwars.api.arena.generator.IGenHolo.class,
                com.andrei1058.bedwars.api.arena.generator.IGenerator.class,
                com.andrei1058.bedwars.api.arena.shop.ShopHolo.class)) {
            for (var method : type.getDeclaredMethods()) {
                String name = method.getName().toLowerCase(Locale.ROOT);
                assertFalse(name.contains("language") || name.contains("iso") || name.contains("locale")
                        || name.contains("getlang") || name.contains("setlang"), type + ": " + name);
            }
        }
        assertEquals(1, ChineseMessages.class.getConstructors().length);
        assertArrayEquals(new Class<?>[]{org.bukkit.plugin.Plugin.class}, ChineseMessages.class.getConstructors()[0].getParameterTypes());
    }

    @Test void removesLegacySelectionHelpWhileKeepingAdministratorMessages() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("cmd-lang-usage", "旧语言提示"); yaml.addDefault("cmd-lang-list", "旧默认值");
        yaml.set(Messages.COMMAND_MAIN, List.of("/bw lang", "/bw message en", "/bw join", "自定义帮助"));
        yaml.set("custom", "保留管理员中文");
        SimplifiedChinese.removeLanguageSelectionMessages(yaml);
        assertNull(yaml.get("cmd-lang-usage")); assertNull(yaml.get("cmd-lang-list"));
        assertEquals(List.of("/bw join", "自定义帮助"), yaml.getStringList(Messages.COMMAND_MAIN));
        assertEquals("保留管理员中文", yaml.getString("custom"));
    }
}
