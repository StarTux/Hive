package com.cavetale.hive.mob;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Hoglin;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.PiglinAbstract;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

@Getter
@RequiredArgsConstructor
public final class SimpleSpawnMob extends AbstractSpawnMob {
    private final EntityType entityType;

    @Override
    @SuppressWarnings("unchecked")
    public Mob spawn(Location location) {
        final Class<? extends Mob> livingEntityClass = (Class<? extends Mob>) entityType.getEntityClass();
        return location.getWorld().spawn(location, livingEntityClass, SpawnReason.CUSTOM, true, this::onSpawnMob);
    }

    private void onSpawnMob(Mob mob) {
        mob.setRemoveWhenFarAway(false);
        mob.setPersistent(false);
        if (mob instanceof Zombie zombie) {
            zombie.setShouldBurnInDay(false);
        } else if (mob instanceof AbstractSkeleton skeleton) {
            skeleton.setShouldBurnInDay(false);
        } else if (mob instanceof Phantom phantom) {
            phantom.setShouldBurnInDay(false);
        } else if (mob instanceof PiglinAbstract piglin) {
            piglin.setImmuneToZombification(true);
        } else if (mob instanceof Hoglin hoglin) {
            hoglin.setImmuneToZombification(true);
        }
        if (mob instanceof Ageable ageable) {
            ageable.setAdult();
        }
    }

    @Override
    public String getName() {
        return entityType.name();
    }

    @Override
    public double getSpawnHeight() {
        return switch (entityType) {
        case GHAST, PHANTOM -> 16.0;
        default -> 0.0;
        };
    }
}
