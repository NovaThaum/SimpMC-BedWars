package com.andrei1058.bedwars.arena;

/** 保留管理员文本，兼容旧版和迁入消息使用的竞技场名称占位符。 */
public final class ArenaMessages {
    private ArenaMessages() { }
    public static String withArena(String message, String displayName) {
        return message.replace("{area}", displayName).replace("{arena}", displayName);
    }
}
