package com.cavetale.hive.mob.boss;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Boss;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.DARK_RED;
import static net.kyori.adventure.text.format.TextDecoration.BOLD;

/**
 * A simple boss which is an upscaled mob.
 */
@Getter
@RequiredArgsConstructor
public final class SimpleBoss extends SpawnBoss {
    private final EntityType entityType;
    private final double scale;

    @Override
    public String getName() {
        return entityType + "_BOSS";
    }

    @Override
    @SuppressWarnings("unchecked")
    public Mob spawn(Location location) {
        final Class<? extends Mob> livingEntityClass = (Class<? extends Mob>) entityType.getEntityClass();
        return location.getWorld().spawn(location, livingEntityClass, SpawnReason.CUSTOM, true, this::onSpawnMob);
    }

    @Override
    protected void onSpawnMob(Mob mob) {
        super.onSpawnMob(mob);
        mob.getAttribute(Attribute.SCALE).setBaseValue(scale);
        mob.customName(text("BOSS", DARK_RED, BOLD));
        if (mob instanceof Boss boss && boss.getBossBar() != null) {
            boss.getBossBar().setVisible(false);
        }
        mob.setGlowing(true);
    }

    @Override
    public double getSpawnHeight() {
        return switch (entityType) {
        case GHAST, PHANTOM -> 16.0;
        default -> 0.0;
        };
    }
}
