package com.andrei1058.bedwars.arena;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ArenaMessagesTest {
    @Test void replacesBothArenaAliasesWithoutChangingPlayerTokensOrCustomText() {
        assertEquals("加入 §b卫城 / §b卫城 {playername}", ArenaMessages.withArena("加入 {area} / {arena} {playername}", "§b卫城"));
        assertEquals("原有欢迎词", ArenaMessages.withArena("原有欢迎词", "卫城"));
    }
}
