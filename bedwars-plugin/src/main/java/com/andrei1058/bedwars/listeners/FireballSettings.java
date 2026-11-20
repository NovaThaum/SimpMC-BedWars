package com.andrei1058.bedwars.listeners;

/** 与 1.21.11 的火球平衡一致；固定规则，不读取配置。 */
final class FireballSettings {
    static final double EXPLOSION_SIZE = 3.25D;
    static final double SPEED_MULTIPLIER = 15D;
    static final double SNEAK_SPEED_MULTIPLIER = 1.6D;
    static final double SNEAK_ACCELERATION_MULTIPLIER = 2D;
    static final double SNEAK_RECOIL = 0.1D;
    static final double COOLDOWN = 0.4D;
    static final double HORIZONTAL_KNOCKBACK = 1.35D;
    static final double VERTICAL_KNOCKBACK = 0.75D;
    static final double SELF_DAMAGE = 2D;
    static final double ENEMY_DAMAGE = 3.5D;

    private FireballSettings() { }
}
