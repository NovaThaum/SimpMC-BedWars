package com.andrei1058.bedwars.commands.bedwars.subcmds.sensitive;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.command.ParentCommand;
import com.andrei1058.bedwars.commands.bedwars.MainCommand;
import com.andrei1058.bedwars.configuration.MainConfig;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ArenaGroupCompletionTest {
    @TempDir Path temp;
    @Test void mainCommandCompletesActionsSavedArenasAndGroupsWithPermissionAndPrefixes() throws Exception {
        var oldPlugin = BedWars.plugin; var oldConfig = BedWars.config;
        try {
            BedWars.plugin = mock(BedWars.class); when(BedWars.plugin.getDataFolder()).thenReturn(temp.toFile());
            BedWars.config = mock(MainConfig.class); YamlConfiguration yaml = new YamlConfiguration();
            yaml.set("arenaGroups", List.of("Solo", "Doubles")); when(BedWars.config.getYml()).thenReturn(yaml);
            Files.createDirectories(temp.resolve("Arenas")); Files.writeString(temp.resolve("Arenas/Castle.yml"), "group: Solo");
            Files.writeString(temp.resolve("Arenas/Temple.yml"), "group: Doubles");Files.writeString(temp.resolve("Arenas/backup.yml.bak"), "");
            Files.createDirectory(temp.resolve("Arenas/not-a-map.yml"));
            ArenaGroup group = spy(new ArenaGroup(mock(ParentCommand.class), "arenaGroup"));
            MainCommand main = mock(MainCommand.class, CALLS_REAL_METHODS);
            doReturn(group).when(main).getSubCommand("arenaGroup"); Player player = mock(Player.class);
            doReturn(true).when(group).canSee(eq(player), any());
            assertEquals(List.of("set"), main.tabComplete(player,"bw", new String[]{"arenaGroup", "se"}, null));
            assertEquals(List.of("Castle", "Temple"), main.tabComplete(player,"bw", new String[]{"arenaGroup", "set", ""}, null));
            assertEquals(List.of("Castle"), main.tabComplete(player,"bw", new String[]{"arenaGroup", "set", "ca"}, null));
            assertEquals(List.of("Default", "Doubles", "Solo"), main.tabComplete(player,"bw", new String[]{"arenaGroup", "set", "Castle", ""}, null));
            assertEquals(List.of("Solo"), main.tabComplete(player,"bw", new String[]{"arenaGroup", "set", "Castle", "s"}, null));
            assertEquals(List.of("Doubles", "Solo"), main.tabComplete(player,"bw", new String[]{"arenaGroup", "remove", ""}, null));
            assertEquals(List.of(), main.tabComplete(player,"bw", new String[]{"arenaGroup", "set", "Castle", "Solo", ""}, null));
            doReturn(false).when(group).canSee(eq(player), any());
            assertEquals(List.of(), main.tabComplete(player,"bw", new String[]{"arenaGroup", "set", ""}, null));
        } finally { BedWars.plugin = oldPlugin; BedWars.config = oldConfig; }
    }
}
