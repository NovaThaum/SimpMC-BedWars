package com.andrei1058.bedwars.language;

import com.andrei1058.bedwars.api.language.Language;
import com.andrei1058.bedwars.api.language.Messages;
import com.andrei1058.bedwars.database.MySQL;
import com.andrei1058.bedwars.database.SQLite;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChineseOnlyLanguageTest {
    @TempDir Path directory;

    @Test void oldPreferencesAndApiMutationCannotSelectAnotherLanguage() throws Exception {
        Plugin plugin = mock(Plugin.class);
        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        Path folder = Files.createDirectories(directory.resolve("Languages"));
        Files.writeString(folder.resolve("messages_zh_cn.yml"), "custom: 中文自定义\nlines:\n  - 自定义列表\n");
        Files.writeString(folder.resolve("messages_en.yml"), "custom: English\n");
        Language oldDefault = Language.getDefaultLanguage();
        try {
            Language language = new Language(plugin, "ZH_CN");
            UUID uuid = UUID.randomUUID();
            Player player = mock(Player.class);
            when(player.getUniqueId()).thenReturn(uuid);
            assertFalse(Language.setPlayerLanguage(uuid, "en"));
            assertTrue(Language.setPlayerLanguage(uuid, "zh_cn"));
            assertTrue(Language.setPlayerLanguage(uuid, null));
            Language.getLangByPlayer().put(uuid, mock(Language.class));
            assertSame(language, Language.getPlayerLanguage(uuid));
            assertSame(language, Language.getPlayerLanguage(player));
            assertSame(language, Language.getLang("en"));
            assertEquals("zh_cn", language.getIso());
            assertFalse(Language.isLanguageExist("en"));
            assertEquals(List.of(language), Language.getLanguages());
            assertThrows(UnsupportedOperationException.class, () -> Language.getLanguages().clear());
            assertThrows(IllegalArgumentException.class, () -> new Language(plugin, "fr_fr"));
            assertFalse(Files.exists(folder.resolve("messages_fr_fr.yml")));
            assertEquals("中文自定义", language.getYml().getString("custom"));
            assertEquals(List.of("自定义列表"), Language.getList(player, "lines"));
            language.reload();
            assertEquals("中文自定义", language.getYml().getString("custom"));
            assertEquals("custom: English\n", Files.readString(folder.resolve("messages_en.yml")));
        } finally {
            var field = Language.class.getDeclaredField("defaultLanguage");
            field.setAccessible(true);
            field.set(null, oldDefault);
        }
    }

    @Test void removesRetiredLanguageHelpButKeepsCustomChineseMessages() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("cmd-lang-usage", "旧语言提示");
        yaml.addDefault("cmd-lang-list", "旧默认值");
        yaml.set(Messages.COMMAND_MAIN, List.of("/bw lang", "/bw message en", "/bw join", "自定义帮助"));
        yaml.set("custom", "保留管理员中文");
        SimplifiedChinese.removeLanguageSelectionMessages(yaml);
        assertNull(yaml.get("cmd-lang-usage"));
        assertNull(yaml.get("cmd-lang-list"));
        assertEquals(List.of("/bw join", "自定义帮助"), yaml.getStringList(Messages.COMMAND_MAIN));
        assertEquals("保留管理员中文", yaml.getString("custom"));
    }

    @Test void compatibilityDatabaseMethodsNeedNoConnection() {
        UUID player = UUID.randomUUID();
        MySQL mysql = mock(MySQL.class, CALLS_REAL_METHODS);
        SQLite sqlite = mock(SQLite.class, CALLS_REAL_METHODS);
        mysql.setLanguage(player, "en");
        sqlite.setLanguage(player, "fr");
        assertEquals("zh_cn", mysql.getLanguage(player));
        assertEquals("zh_cn", sqlite.getLanguage(player));
    }

    @Test void compatibilityDefaultTemplatesAlwaysUseChinese() {
        for (MatchQueryMessages.Key key : MatchQueryMessages.Key.values())
            assertEquals(MatchQueryMessages.defaults(true).text(key), MatchQueryMessages.defaults(false).text(key));
        for (MatchmakingMessages.Key key : MatchmakingMessages.Key.values())
            assertEquals(MatchmakingMessages.defaults(true).text(key), MatchmakingMessages.defaults(false).text(key));
    }
}
