package com.cavetale.hive.mob;

import com.cavetale.worldmarker.entity.EntityMarker;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import static com.cavetale.hive.HivePlugin.hivePlugin;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.DARK_GREEN;

public final class FartGoblin extends AbstractSpawnMob implements Listener {
    private static final double HEALTH = 50;
    private static FartGoblin instance;
    private static String worldMarkerId = "fart_goblin";
    private static String worldMarkerIdCloud = "fart_goblin_cloud";

    private FartGoblin() { }

    public static FartGoblin getInstance() {
        if (instance == null) {
            instance = new FartGoblin();
            Bukkit.getPluginManager().registerEvents(instance, hivePlugin());
        }
        return instance;
    }

    @Override
    public String getName() {
        return "FartGoblin";
    }

    @Override
    public Entity spawn(Location location) {
        return location.getWorld().spawn(location, Creeper.class, e -> {
                e.customName(text("Fart Goblin", DARK_GREEN));
                e.getAttribute(Attribute.MAX_HEALTH).setBaseValue(HEALTH);
                e.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.5);
                e.getAttribute(Attribute.KNOCKBACK_RESISTANCE).setBaseValue(1.0);
                e.getAttribute(Attribute.SCALE).setBaseValue(0.75);
                e.setHealth(HEALTH);
                e.setRemoveWhenFarAway(false);
                e.setPersistent(false);
                EntityMarker.setId(e, worldMarkerId);
            });
    }

    @Override
    public boolean is(Entity entity) {
        return EntityMarker.hasId(entity, worldMarkerId);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onExplosionPrime(ExplosionPrimeEvent event) {
        if (!is(event.getEntity())) return;
        event.setCancelled(true);
        final Creeper creeper = (Creeper) event.getEntity();
        creeper.getWorld().playSound(creeper.getEyeLocation(), Sound.ENTITY_PLAYER_BURP, SoundCategory.HOSTILE, 1.0f, 0.5f);
        creeper.getWorld().spawn(creeper.getLocation(), AreaEffectCloud.class, e -> {
                e.addCustomEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 20, 2), true);
                e.addCustomEffect(new PotionEffect(PotionEffectType.WITHER, 20 * 10, 0), true);
                e.addCustomEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 10, 0), true);
                e.addCustomEffect(new PotionEffect(PotionEffectType.NAUSEA, 20 * 10, 0), true);
                e.addCustomEffect(new PotionEffect(PotionEffectType.POISON, 20 * 20, 1), true);
                e.setColor(Color.GREEN);
                e.setDuration(200);
                e.setRadius(2.0f);
                e.setRadiusPerTick(1.0f / 100.0f);
                e.setSource(creeper);
                EntityMarker.setId(e, worldMarkerIdCloud);
            });
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onAreaEffectCloudApply(AreaEffectCloudApplyEvent event) {
        if (!EntityMarker.hasId(event.getEntity(), worldMarkerIdCloud)) return;
        for (Entity e : List.copyOf(event.getAffectedEntities())) {
            if (e instanceof Player) continue;
            event.getAffectedEntities().remove(e);
        }
    }
}
