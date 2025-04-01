package com.cavetale.hive;

import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.hive.mob.HiveEntityComponent;
import com.cavetale.hive.mob.SimpleSpawnMob;
import com.destroystokyo.paper.event.entity.EntityPathfindEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BellRingEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.SlimeSplitEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

public final class HiveListener implements Listener {
    public void enable() {
        Bukkit.getPluginManager().registerEvents(this, HivePlugin.hivePlugin());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onEntityDeath(EntityDeathEvent event) {
        final HiveEntityComponent component = HiveEntityComponent.get(event.getEntity());
        if (component == null) return;
        component.getHive().onEntityDeath(event.getEntity());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onEntityExplode(EntityExplodeEvent event) {
        final HiveEntityComponent component = HiveEntityComponent.get(event.getEntity());
        if (component == null) return;
        component.getHive().onEntityDeath(event.getEntity());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onPlayerHud(PlayerHudEvent event) {
        final Hive hive = Hive.at(event.getPlayer().getLocation());
        if (hive != null) hive.onPlayerHud(event);
    }

    private Hive slimeSplitHive;
    private int slimeSplitCount;

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onSlimeSplit(SlimeSplitEvent event) {
        final HiveEntityComponent component = HiveEntityComponent.get(event.getEntity());
        if (component == null) return;
        slimeSplitHive = component.getHive();
        slimeSplitCount = event.getCount();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onSlimeSplitSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.SLIME_SPLIT) return;
        if (slimeSplitHive == null) return;
        if (slimeSplitCount <= 0) return;
        if (!slimeSplitHive.isInsideArena(event.getLocation())) return;
        final SimpleSpawnMob spawnMob = new SimpleSpawnMob(event.getEntity().getType());
        spawnMob.shouldRespawn(false);
        slimeSplitHive.getSpawnedMobs().put(event.getEntity().getUniqueId(), spawnMob);
        new HiveEntityComponent(slimeSplitHive, spawnMob).attach(event.getEntity());
        slimeSplitCount -= 1;
        if (slimeSplitCount <= 0) {
            slimeSplitHive = null;
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    private void onEntityPathfind(EntityPathfindEvent event) {
        final HiveEntityComponent component = HiveEntityComponent.get(event.getEntity());
        if (component == null) return;
        if (event.getTargetEntity() == null && !component.getHive().isInsideArena(event.getLoc())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    private void onEntityTarget(EntityTargetEvent event) {
        if (event.getTarget() == null) return;
        final HiveEntityComponent component1 = HiveEntityComponent.get(event.getEntity());
        if (component1 == null) return;
        final HiveEntityComponent component2 = HiveEntityComponent.get(event.getTarget());
        if (component2 == null) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onChunkUnload(ChunkUnloadEvent event) {
        final Hive hive = Hive.at(event.getChunk());
        if (hive != null) hive.hibernate();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onBellRing(BellRingEvent event) {
        final Hive hive = Hive.at(event.getBlock());
        if (hive != null) hive.onBellRing();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    private void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        final HiveEntityComponent damageeComponent = HiveEntityComponent.get(event.getEntity());
        if (damageeComponent == null) return;
        if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter) {
            final HiveEntityComponent damagerComponent = HiveEntityComponent.get(shooter);
            if (damagerComponent != null) event.setCancelled(true);
        } else {
            final HiveEntityComponent damagerComponent = HiveEntityComponent.get(event.getDamager());
            if (damagerComponent != null) event.setCancelled(true);
        }
    }
}
