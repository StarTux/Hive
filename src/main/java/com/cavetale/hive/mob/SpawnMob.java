package com.cavetale.hive.mob;

import com.cavetale.hive.mob.boss.SpawnBoss;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

public interface SpawnMob {
    List<SpawnMob> SPAWN_MOBS = new ArrayList<>();

    static List<SpawnMob> getSpawnMobs() {
        return SPAWN_MOBS;
    }

    static List<SpawnMob> getAllMobs() {
        final List<SpawnMob> result = new ArrayList<>();
        result.addAll(SPAWN_MOBS);
        result.addAll(SpawnBoss.getBosses());
        return result;
    }

    static void init() {
        SPAWN_MOBS.clear();
        int minWave = 0;
        SPAWN_MOBS.add(new SimpleSpawnMob(EntityType.CREEPER).minWave(minWave).weight(10));
        SPAWN_MOBS.add(new SimpleSpawnMob(EntityType.ZOMBIE).minWave(minWave).weight(5));
        SPAWN_MOBS.add(new SimpleSpawnMob(EntityType.SKELETON).minWave(minWave).weight(5));
        SPAWN_MOBS.add(new SimpleSpawnMob(EntityType.SPIDER).minWave(minWave).weight(5));
        minWave = 10;
        SPAWN_MOBS.add(new SimpleSpawnMob(EntityType.DROWNED).minWave(minWave).weight(3));
        SPAWN_MOBS.add(new SimpleSpawnMob(EntityType.HUSK).minWave(minWave).weight(3));
        SPAWN_MOBS.add(new SimpleSpawnMob(EntityType.STRAY).minWave(minWave).weight(3));
        SPAWN_MOBS.add(new SimpleSpawnMob(EntityType.SLIME).minWave(minWave).weight(3));
        minWave = 20;
        SPAWN_MOBS.add(new SimpleSpawnMob(EntityType.BLAZE).minWave(minWave).weight(3));
        SPAWN_MOBS.add(new SimpleSpawnMob(EntityType.MAGMA_CUBE).minWave(minWave).weight(3));
        SPAWN_MOBS.add(new SimpleSpawnMob(EntityType.PIGLIN).minWave(minWave).weight(3));
        SPAWN_MOBS.add(new SimpleSpawnMob(EntityType.ENDERMAN).minWave(minWave).weight(1));
        SPAWN_MOBS.add(new SimpleSpawnMob(EntityType.BOGGED).minWave(minWave).weight(3));
        minWave = 30;
        SPAWN_MOBS.add(new SimpleSpawnMob(EntityType.HOGLIN).minWave(minWave).weight(3));
        SPAWN_MOBS.add(new SimpleSpawnMob(EntityType.WITHER_SKELETON).minWave(minWave).weight(3));
        SPAWN_MOBS.add(new SimpleSpawnMob(EntityType.CAVE_SPIDER).minWave(minWave).weight(3));
        minWave = 40;
        SPAWN_MOBS.add(new SimpleSpawnMob(EntityType.PHANTOM).minWave(minWave).weight(3));
        SPAWN_MOBS.add(new SimpleSpawnMob(EntityType.WITCH).minWave(minWave).weight(3));
        SPAWN_MOBS.add(new SimpleSpawnMob(EntityType.ZOGLIN).minWave(minWave).weight(3));
        SPAWN_MOBS.add(new SimpleSpawnMob(EntityType.ZOMBIFIED_PIGLIN).minWave(minWave).weight(3));
        minWave = 50;
        SPAWN_MOBS.add(new SimpleSpawnMob(EntityType.PIGLIN_BRUTE).minWave(minWave).weight(3));
        SPAWN_MOBS.add(new SimpleSpawnMob(EntityType.PILLAGER).minWave(minWave).weight(3));
        SPAWN_MOBS.add(new SimpleSpawnMob(EntityType.GHAST).minWave(minWave).weight(3));
        SPAWN_MOBS.add(new SimpleSpawnMob(EntityType.VINDICATOR).minWave(minWave).weight(3));
        minWave = 60;
        SPAWN_MOBS.add(new SimpleSpawnMob(EntityType.EVOKER).minWave(minWave).weight(1));
        SPAWN_MOBS.add(new SimpleSpawnMob(EntityType.RAVAGER).minWave(minWave).weight(1));
        SPAWN_MOBS.add(new SimpleSpawnMob(EntityType.ILLUSIONER).minWave(minWave).weight(1));
        SPAWN_MOBS.add(new SimpleSpawnMob(EntityType.BREEZE).minWave(minWave).weight(1));
        // Special mobs
        SPAWN_MOBS.add(FartGoblin.getInstance().minWave(30).weight(3));
    }

    int getMinWave();

    int getWeight();

    Entity spawn(Location location);

    String getName();

    default double getSpawnHeight() {
        return 0.0;
    }

    default boolean shouldRespawn() {
        return true;
    }

    default boolean isBoss() {
        return false;
    }

    /**
     * This default implementation only works if the mob has the
     * HiveEntityComponent attached, which is only done by the hive.
     * Special mobs with listeners may have to override this.
     */
    default boolean is(Entity entity) {
        if (entity == null) return false;
        final HiveEntityComponent component = HiveEntityComponent.get(entity);
        return component != null && component.getSpawnMob() == this;
    }
}
