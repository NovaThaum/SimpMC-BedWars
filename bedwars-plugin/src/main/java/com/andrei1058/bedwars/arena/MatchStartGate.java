package com.andrei1058.bedwars.arena;

import java.util.concurrent.atomic.AtomicBoolean;

/** 本服进程内的新对局开关；重启后默认开放，不影响进行中的比赛。 */
public final class MatchStartGate {
    private static final AtomicBoolean OPEN = new AtomicBoolean(true);
    private MatchStartGate() { }
    public static boolean isOpen() { return OPEN.get(); }
    public static boolean stop() { return OPEN.compareAndSet(true, false); }
    public static boolean resume() { return OPEN.compareAndSet(false, true); }
}
