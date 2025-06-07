package com.cavetale.hive;

import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.hud.PlayerHudPriority;
import com.cavetale.core.item.ItemKinds;
import com.cavetale.core.struct.Cuboid;
import com.cavetale.core.struct.Vec2i;
import com.cavetale.core.struct.Vec3i;
import com.cavetale.hive.mob.HiveEntityComponent;
import com.cavetale.hive.mob.SpawnMob;
import com.cavetale.hive.mob.boss.SpawnBoss;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.loot.Loot;
import com.cavetale.mytems.util.Collision;
import com.cavetale.worldmarker.block.BlockMarker;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import lombok.Data;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.TrialSpawner;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import static com.cavetale.hive.HivePlugin.hivePlugin;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.title.Title.title;

/**
 * The Hive runtime.
 */
@Data
public final class Hive {
    private static final List<Hive> ALL_HIVES = new ArrayList<>();
    private final Block block;
    private final Cuboid area;
    private final World world;
    private final Set<Vec2i> chunks = new HashSet<>();
    private int radius = 32;
    private BukkitTask task;
    private final Random random = ThreadLocalRandom.current();
    private int ticks = 0;

    // Saved state
    private int ticksLived = 0;
    private int level = 1; // save?
    private int levelTicks = 0;
    private boolean levelDefeated;
    private int mobCount = 0;

    // Statistics
    private int totalMobCount;
    private float percentage;

    // Make everything go super fast
    private boolean testing = false;
    private int glowTicks = 0;

    // We need the SpawnMob here because it will not be available if
    // the entity goes missing.
    private final Map<UUID, SpawnMob> spawnedMobs = new HashMap<>();
    private final List<SpawnMob> mobsToSpawn = new ArrayList<>();
    private final List<Vec2i> blocksToBuild = new ArrayList<>();
    private final List<Orbit> orbits = new ArrayList<>();
    private List<ItemStack> loot = new ArrayList<>();

    public Hive(final Block block) {
        this.block = block;
        this.world = block.getWorld();
        this.area = new Cuboid(block.getX() - radius, block.getY() - 16, block.getZ() - radius,
                               block.getX() + radius, world.getMaxHeight(), block.getZ() + radius);
        for (Vec3i v3 : area.blockToChunk().enumerate()) {
            chunks.add(v3.toVec2i());
        }
    }

    public void enable() {
        task = Bukkit.getScheduler().runTaskTimer(hivePlugin(), this::tick, 1L, 1L);
        ALL_HIVES.add(this);
    }

    public void disable() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        ALL_HIVES.remove(this);
        removeAllSpawnedMobs();
        for (Orbit orbit : orbits) orbit.remove();
        orbits.clear();
    }

    public static void disableAll() {
        for (Hive hive : List.copyOf(ALL_HIVES)) {
            hive.hibernate();
        }
    }

    public void hibernate() {
        save();
        disable();
        hivePlugin().getLogger().info("Hibernate: " + world.getName() + " " + block.getX() + " " + block.getY() + " " + block.getZ()
                                      + " level=" + level
                                      + " radius=" + radius);
    }

    public static Hive wakeUp(Block hiveBlock) {
        final Hive hive = new Hive(hiveBlock);
        if (!hive.load()) return null;
        hive.enable();
        hivePlugin().getLogger().info("Wake Up: " + hive.world.getName() + " " + hive.block.getX() + " " + hive.block.getY() + " " + hive.block.getZ()
                                      + " level=" + hive.level
                                      + " radius=" + hive.radius);
        return hive;
    }

    public static Hive at(Location location) {
        final World locationWorld = location.getWorld();
        for (Hive it : ALL_HIVES) {
            if (it.world.equals(locationWorld) && it.area.contains(location)) {
                return it;
            }
        }
        return null;
    }

    public static Hive at(Block block) {
        final World blockWorld = block.getWorld();
        for (Hive it : ALL_HIVES) {
            if (it.world.equals(blockWorld) && it.area.contains(block)) {
                return it;
            }
        }
        return null;
    }

    public static Hive at(Chunk chunk) {
        final World chunkWorld = chunk.getWorld();
        final Vec2i vec2i = Vec2i.of(chunk);
        for (Hive it : ALL_HIVES) {
            if (it.world.equals(chunkWorld) && it.chunks.contains(vec2i)) {
                return it;
            }
        }
        return null;
    }

    public boolean isInsideArena(Location location) {
        if (!location.getWorld().equals(world)) return false;
        if (!area.contains(location)) return false;
        final int dx = location.getBlockX() - block.getX();
        final int dz = location.getBlockZ() - block.getZ();
        return dx * dx + dz * dz <= radius * radius;
    }

    public void removeAllSpawnedMobs() {
        for (UUID uuid : spawnedMobs.keySet()) {
            final Entity entity = Bukkit.getEntity(uuid);
            if (entity != null) entity.remove();
        }
    }

    private void tick() {
        final int currentLevelTicks = levelTicks++;
        final int currentTicks = ticks++;
        checkSpawnedMobs();
        if (currentLevelTicks == 20) {
            for (Player player : getPresentPlayers()) {
                player.showTitle(title(empty(), text("Hive Level " + level, RED)));
            }
        } else if (currentLevelTicks == 100) {
            generateSpawnMobs();
        } else if (currentLevelTicks > 100) {
            totalMobCount = mobsToSpawn.size() + spawnedMobs.size();
            percentage = (float) totalMobCount / (float) mobCount;
            if (totalMobCount == 0) {
                // Defeated
                if (level % 10 == 0) {
                    rollLoot();
                }
                level += 1;
                levelTicks = 0;
                levelDefeated = false;
                save();
            } else {
                tryToSpawnMob();
                buildFortifications();
            }
        }
        if (glowTicks > 0) {
            glowTicks -= 1;
            if (glowTicks == 0) {
                unGlowAll();
            }
        }
        // Spawn Orbits
        for (Orbit orbit : orbits) orbit.onTick();
        orbits.removeIf(Orbit::isDead);
    }

    private void rollLoot() {
        final int total = level < 80
            ? 7
            : Math.max(3, 7 - ((level - 70) / 10));
        final int primeCount = Math.min(total, level / 10);
        final int regularCount = total - primeCount;
        final List<ItemStack> prime = new ArrayList<>();
        Loot.loot().populatePrimeLoot(prime, primeCount);
        Collections.shuffle(prime);
        final List<ItemStack> regular = new ArrayList<>();
        for (int i = 0; i < regularCount; i += 1) {
            switch (random.nextInt(3)) {
            case 0: regular.add(Mytems.COPPER_COIN.createItemStack()); break;
            case 1: regular.add(Mytems.SILVER_COIN.createItemStack()); break;
            case 2: regular.add(Mytems.GOLDEN_COIN.createItemStack()); break;
            default: break;
            }
        }
        Collections.shuffle(regular);
        loot.clear();
        for (int i = 0; i < primeCount; i += 1) {
            loot.add(prime.get(i));
        }
        for (int i = 0; i < regularCount; i += 1) {
            loot.add(regular.get(i));
        }
        Collections.shuffle(loot);
        for (Orbit orbit : orbits) orbit.setDiminish(true);
        for (ItemStack l : loot) {
            final Orbit orbit = new Orbit(block.getLocation().add(0.5, 0.5, 0.5), l.clone());
            orbits.add(orbit);
        }
    }

    private void generateSpawnMobs() {
        final Map<SpawnMob, Integer> newMobs = new HashMap<>();
        if (level % 10 == 0) {
            if (level == 50) {
                mobsToSpawn.add(SpawnBoss.WITHER);
            } else if (level == 100) {
                mobsToSpawn.add(SpawnBoss.WARDEN);
            } else {
                final List<SpawnBoss> bossTypes = SpawnBoss.getBosses();
                mobsToSpawn.add(bossTypes.get(random.nextInt(bossTypes.size())));
            }
        }
        int totalChance = 0;
        for (SpawnMob spawnMob : SpawnMob.getSpawnMobs()) {
            if (level < spawnMob.getMinWave()) continue;
            totalChance += spawnMob.getWeight();
            newMobs.put(spawnMob, spawnMob.getWeight());
        }
        int totalMobs = 10 + (level / 10) * 5;
        for (SpawnMob type : newMobs.keySet()) {
            int chance = newMobs.get(type);
            int amount = Math.max(1, (totalMobs * chance) / totalChance);
            for (int i = 0; i < amount; i += 1) mobsToSpawn.add(type);
        }
        mobCount = mobsToSpawn.size();
    }

    public void onEntityDeath(Entity e) {
        final SpawnMob spawnMob = spawnedMobs.remove(e.getUniqueId());
    }

    public void onPlayerHud(PlayerHudEvent event) {
        event.bossbar(PlayerHudPriority.HIGH, text("Hive Level " + level, RED), BossBar.Color.RED, BossBar.Overlay.PROGRESS,
                      Set.of(BossBar.Flag.CREATE_WORLD_FOG), percentage);
    }

    private void buildFortifications() {
        if (!testing && level < 20) return;
        if (blocksToBuild.isEmpty()) {
            final int radiusSq = radius * radius;
            for (int z = -radius; z <= radius; z += 1) {
                for (int x = -radius; x <= radius; x += 1) {
                    if (x * x + z * z > radiusSq) continue;
                    blocksToBuild.add(Vec2i.of(x, z));
                }
            }
        }
        Collections.shuffle(blocksToBuild);
        final Vec2i blockToBuild = blocksToBuild.remove(blocksToBuild.size() - 1);
        if (testing || level >= 20) {
            spawnSoilBlocks(blockToBuild);
        }
        if (testing || level >= 30) {
            spawnFortBlocks(blockToBuild);
        }
    }

    private void spawnSoilBlocks(Vec2i blockVector) {
        Block blockSoil = world.getHighestBlockAt(block.getX() + blockVector.x, block.getZ() + blockVector.z);
        while (blockSoil.isLiquid()) {
            blockSoil = blockSoil.getRelative(0, -1, 0);
        }
        final int floorLevel = block.getY() - 3;
        if (blockSoil.getY() < floorLevel) {
            if (tryToBreakBelow(blockSoil)) return;
            if (tryToBreakBelow(blockSoil.getRelative(0, 1, 0))) return;
            final Cuboid pillar = new Cuboid(blockSoil.getX(), world.getMinHeight(), blockSoil.getZ(),
                                             blockSoil.getX(), world.getMaxHeight(), blockSoil.getZ());
            for (Entity e : world.getNearbyEntities(pillar.toBoundingBox())) {
                if (e instanceof FallingBlock) return;
            }
            // Find highest block
            int highest = blockSoil.getY() + 1;
            for (int dx = -1; dx <= 1; dx += 1) {
                for (int dz = -1; dz <= 1; dz += 1) {
                    final int here = world.getHighestBlockYAt(blockSoil.getX() + dx, blockSoil.getZ() + dz) + 1;
                    if (here > highest) highest = here;
                }
            }
            highest = Math.min(floorLevel, highest);
            final int spawnBlockCount = Math.min(16, highest - blockSoil.getY());
            for (int i = 0; i < spawnBlockCount; i += 1) {
                final Material mat;
                if (blockSoil.getY() + i + 1 == floorLevel) {
                    mat = random.nextBoolean()
                        ? Material.RED_NETHER_BRICKS
                        : Material.OBSIDIAN;
                } else {
                    mat = Material.NETHERRACK;
                }
                world.spawn(blockSoil.getLocation().add(0.5, 128.0 + (double) i, 0.5), FallingBlock.class, e -> {
                        e.setBlockData(mat.createBlockData());
                        e.setDropItem(false);
                    });
            }
        } else if (blockSoil.getY() >= floorLevel) {
            if (tryToBreakAbove(blockSoil)) return;
            tryToBreakBelow(blockSoil);
        }
    }

    private static boolean tryToBreakAbove(Block block) {
        final Material material = block.getType();
        if (material.isAir()) return false;
        switch (material) {
        case DIRT:
        case GRASS_BLOCK:
        case CACTUS:
        case PUMPKIN:
        case MELON:
        case SAND:
        case RED_SAND:
        case SANDSTONE:
        case TORCH:
        case RED_SANDSTONE:
        case STONE:
        case ANDESITE:
        case GRANITE:
        case DIORITE:
        case NETHERRACK:
        case TERRACOTTA:
        case POINTED_DRIPSTONE:
        case MYCELIUM:
        case PODZOL:
        case FARMLAND:
        case DIRT_PATH:
        case ROOTED_DIRT:
        case MOSS_BLOCK:
        case PALE_MOSS_BLOCK:
            block.breakNaturally();
            return true;
        case WATER:
            if (((org.bukkit.block.data.Levelled) block.getBlockData()).getLevel() == 0) {
                block.setType(Material.ICE);
            }
            return true;
        case LAVA:
            block.setType(Material.OBSIDIAN);
            return true;
        default:
            if (Tag.BADLANDS_TERRACOTTA.isTagged(material)
                || Tag.SLABS.isTagged(material)
                || Tag.CROPS.isTagged(material)) {
                block.breakNaturally();
                return true;
            } else if (Tag.LEAVES.isTagged(material)) {
                block.setType(Material.FIRE);
                return true;
            }
            return false;
        }
    }

    private static boolean tryToBreakBelow(Block block) {
        final Material material = block.getType();
        if (material.isAir()) return false;
        switch (material) {
        case CACTUS:
        case TORCH:
        case LEVER:
        case POINTED_DRIPSTONE:
        case FARMLAND:
        case DIRT_PATH:
        case KELP:
        case SEAGRASS:
        case TALL_SEAGRASS:
            block.breakNaturally();
            return true;
        default:
            if (Tag.BUTTONS.isTagged(material)
                || Tag.SAPLINGS.isTagged(material)
                || Tag.SLABS.isTagged(material)
                || Tag.FLOWERS.isTagged(material)
                || Tag.CROPS.isTagged(material)) {
                block.breakNaturally();
                return true;
            }
            return false;
        }
    }

    private void spawnFortBlocks(Vec2i blockVector) {
        final int extFort = (int) Math.sqrt(blockVector.x * blockVector.x + blockVector.z * blockVector.z) - radius + 4;
        if (extFort < 0) return;
        final Block blockFort = world.getHighestBlockAt(block.getX() + blockVector.x, block.getZ() + blockVector.z);
        final int wallLevel = block.getY() + extFort * (level / 30);
        if (blockFort.getY() < wallLevel) {
            final Cuboid pillar = new Cuboid(blockFort.getX(), world.getMinHeight(), blockFort.getZ(),
                                             blockFort.getX(), world.getMaxHeight(), blockFort.getZ());
            for (Entity e : world.getNearbyEntities(pillar.toBoundingBox())) {
                if (e instanceof FallingBlock) return;
            }
            final Material mat = switch (random.nextInt(4)) {
            case 0, 1 -> Material.NETHER_BRICKS;
            case 2 -> Material.CRACKED_NETHER_BRICKS;
            default -> Material.CHISELED_NETHER_BRICKS;
            };
            world.spawn(blockFort.getLocation().add(0.5, 128.0, 0.5), FallingBlock.class, e -> {
                    e.setBlockData(mat.createBlockData());
                    e.setDropItem(false);
                });
        }
    }

    private void checkSpawnedMobs() {
        // Check spawned mobs
        for (Iterator<Map.Entry<UUID, SpawnMob>> iter = spawnedMobs.entrySet().iterator(); iter.hasNext();) {
            Map.Entry<UUID, SpawnMob> entry = iter.next();
            final UUID uuid = entry.getKey();
            final SpawnMob spawnMob = entry.getValue();
            final Entity e = Bukkit.getEntity(uuid);
            if (e == null || e.isDead()) {
                if (e != null) e.remove();
                iter.remove();
                if (spawnMob.shouldRespawn()) {
                    mobsToSpawn.add(spawnMob);
                }
                continue;
            }
            final HiveEntityComponent component = HiveEntityComponent.get(e);
            if (!isInsideArena(e.getLocation())) {
                for (int i = 0; i < 10; i += 1) {
                    e.teleport(getRandomSpawnLocation().add(0, component.getSpawnMob().getSpawnHeight(), 0));
                    if (!Collision.collidesWithBlock(world, e.getBoundingBox())) {
                        break;
                    }
                }
                continue;
            }
        }
    }

    private boolean tryToSpawnMob() {
        if (mobsToSpawn.isEmpty()) return false;
        final SpawnMob spawnMob = mobsToSpawn.get(mobsToSpawn.size() - 1);
        final Entity entity = spawnMob.spawn(getRandomSpawnLocation().add(0, spawnMob.getSpawnHeight(), 0));
        if (entity == null) return false;
        for (int i = 0; i < 10 && Collision.collidesWithBlock(world, entity.getBoundingBox()); i += 1) {
            entity.teleport(getRandomSpawnLocation());
        }
        final HiveEntityComponent component = new HiveEntityComponent(this, spawnMob);
        component.attach(entity);
        if (spawnMob.isBoss()) component.setBoss(true);
        spawnedMobs.put(entity.getUniqueId(), spawnMob);
        mobsToSpawn.remove(mobsToSpawn.size() - 1);
        return true;
    }

    public Location getRandomSpawnLocation() {
        final double cx = random.nextDouble() - 0.5;
        final double cz = random.nextDouble() - 0.5;
        final Vector vecSpawn = new Vector(cx, 0.0, cz).normalize().multiply(random.nextDouble() * (double) (radius - 4));
        final Block spawnBlock = world.getHighestBlockAt(block.getX() + vecSpawn.getBlockX(), block.getZ() + vecSpawn.getBlockZ());
        final Location result = spawnBlock.getLocation().add(0.5, 1.0, 0.5);
        result.setYaw(random.nextFloat() * 360f);
        return result;
    }

    public List<Player> getPresentPlayers() {
        final List<Player> result = new ArrayList<>();
        for (Player player : world.getPlayers()) {
            if (area.contains(player.getLocation())) {
                result.add(player);
            }
        }
        return result;
    }

    private void save() {
        if (block.getType() != Material.TRIAL_SPAWNER) {
            block.setType(Material.TRIAL_SPAWNER);
        }
        final TrialSpawner trialSpawner = (TrialSpawner) block.getState();
        final PersistentDataContainer tag = trialSpawner.getPersistentDataContainer();
        tag.set(new NamespacedKey(hivePlugin(), "level"), PersistentDataType.INTEGER, level);
        tag.set(new NamespacedKey(hivePlugin(), "radius"), PersistentDataType.INTEGER, radius);
        trialSpawner.update();
    }

    private boolean load() {
        if (!(block.getState() instanceof TrialSpawner trialSpawner)) return false;
        final PersistentDataContainer tag = trialSpawner.getPersistentDataContainer();
        level = tag.getOrDefault(new NamespacedKey(hivePlugin(), "level"), PersistentDataType.INTEGER, level);
        radius = tag.getOrDefault(new NamespacedKey(hivePlugin(), "radius"), PersistentDataType.INTEGER, radius);
        return true;
    }

    public void onBellRing() {
        if (glowTicks > 0) return;
        glowTicks = 100;
        for (UUID uuid : spawnedMobs.keySet()) {
            final Entity entity = Bukkit.getEntity(uuid);
            if (entity == null) continue;
            final HiveEntityComponent component = HiveEntityComponent.get(entity);
            if (component == null) continue;
            if (entity.isGlowing() || component.isGlowing()) continue;
            entity.setGlowing(true);
            component.setGlowing(true);
        }
    }

    private void unGlowAll() {
        for (UUID uuid : spawnedMobs.keySet()) {
            final Entity entity = Bukkit.getEntity(uuid);
            if (entity == null) continue;
            final HiveEntityComponent component = HiveEntityComponent.get(entity);
            if (component == null) continue;
            if (!component.isGlowing()) continue;
            entity.setGlowing(false);
            component.setGlowing(false);
        }
    }

    public void breakBlock(Player player) {
        block.breakNaturally();
        BlockMarker.resetId(block);
        if (!loot.isEmpty()) {
            final ItemStack drop = loot.get(random.nextInt(loot.size()));
            block.getWorld().dropItem(block.getLocation().add(0.5, 0.5, 0.5), drop);
            player.sendMessage(textOfChildren(text("Hive reward ", GOLD),
                                              ItemKinds.chatDescription(drop)));
        }
        disable();
    }
}
