package com.cavetale.hive.mob.boss;

import com.cavetale.hive.mob.SpawnMob;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;

public abstract class SpawnBoss implements SpawnMob {
    public static final double HEALTH = 1000.0;
    private static final List<SpawnBoss> SPAWN_BOSSES = new ArrayList<>();
    public static final SimpleBoss WITHER = new SimpleBoss(EntityType.WITHER, 2.0);
    public static final SimpleBoss WARDEN = new SimpleBoss(EntityType.WARDEN, 2.0);
    public static final SimpleBoss GIANT = new SimpleBoss(EntityType.ZOMBIE, 16.0);

    public static List<SpawnBoss> getBosses() {
        return SPAWN_BOSSES;
    }

    public static void init() {
        SPAWN_BOSSES.add(WITHER);
        SPAWN_BOSSES.add(GIANT);
        SPAWN_BOSSES.add(new SimpleBoss(EntityType.BLAZE, 4.0));
        SPAWN_BOSSES.add(new SimpleBoss(EntityType.PHANTOM, 4.0));
        SPAWN_BOSSES.add(new SimpleBoss(EntityType.SPIDER, 4.0));
        SPAWN_BOSSES.add(new SimpleBoss(EntityType.CAVE_SPIDER, 8.0));
        SPAWN_BOSSES.add(new SimpleBoss(EntityType.SLIME, 4.0));
        SPAWN_BOSSES.add(new SimpleBoss(EntityType.MAGMA_CUBE, 4.0));
        // SPAWN_BOSSES.add(new SimpleBoss(EntityType.PIGLIN_BRUTE, 4.0));
        SPAWN_BOSSES.add(new SimpleBoss(EntityType.WITCH, 4.0));
        SPAWN_BOSSES.add(new SimpleBoss(EntityType.SILVERFISH, 16.0));
        SPAWN_BOSSES.add(new SimpleBoss(EntityType.BREEZE, 4.0));
        SPAWN_BOSSES.add(new SimpleBoss(EntityType.GHAST, 2.0));
        SPAWN_BOSSES.add(new SimpleBoss(EntityType.WITHER_SKELETON, 4.0));
    }

    /**
     * Default min wave is 0.
     */
    @Override
    public int getMinWave() {
        return 0;
    }

    /**
     * Default weight is 1.
     */
    @Override
    public int getWeight() {
        return 1;
    }

    @Override
    public final boolean isBoss() {
        return true;
    }

    /**
     * Default callback for any spawned boss.  Must be called by the
     * implementing spawn function in the subclass.
     */
    protected void onSpawnMob(Mob mob) {
        mob.getAttribute(Attribute.MAX_HEALTH).setBaseValue(HEALTH);
        mob.setHealth(HEALTH);
        mob.setRemoveWhenFarAway(false);
        mob.setPersistent(false);
    }
}
