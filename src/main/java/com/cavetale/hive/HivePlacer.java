package com.cavetale.hive;

import com.cavetale.core.struct.Cuboid;
import com.cavetale.core.struct.Vec2i;
import com.cavetale.structure.cache.Structure;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitTask;
import static com.cavetale.hive.HivePlugin.hivePlugin;
import static com.cavetale.structure.StructurePlugin.structurePlugin;

/**
 * Attempt to place one Hive per region.
 */
public final class HivePlacer {
    private final World world;
    private Cuboid worldBorderCuboid;
    private List<Vec2i> regionList = new ArrayList<>();
    private List<Vec2i> chunkList = new ArrayList<>();
    private final List<Cuboid> placedHives = new ArrayList<>();
    private BukkitTask task;
    private boolean busy;
    private final Random random = new Random();
    private boolean shouldPlaceForReal = true;

    public HivePlacer(final World world) {
        this.world = world;
        final WorldBorder border = world.getWorldBorder();
        final double hsize = border.getSize() * 0.5;
        final Location center = border.getCenter();
        worldBorderCuboid = new Cuboid((int) (center.getX() - hsize), world.getMinHeight(), (int) (center.getZ() - hsize),
                                       (int) (center.getX() + hsize), world.getMaxHeight(), (int) (center.getZ() + hsize));
        final Cuboid regionCuboid = worldBorderCuboid.applyCoords(i -> i >> 9);
        hivePlugin().getLogger().info("[Placer] regionCuboid " + regionCuboid);
        for (int rz = regionCuboid.az + 1; rz < regionCuboid.bz; rz += 1) {
            for (int rx = regionCuboid.ax + 1; rx < regionCuboid.bx; rx += 1) {
                regionList.add(new Vec2i(rx, rz));
            }
        }
        regionList.sort(Comparator.comparing(r -> -Math.abs(r.x) - Math.abs(r.z)));
        hivePlugin().getLogger().info("[Placer] " + world.getName() + " " + regionList.size() + " regions");
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(hivePlugin(), this::tick, 1L, 1L);
    }

    public void stop() {
        task.cancel();
    }

    public void tick() {
        if (busy) return;
        if (chunkList.isEmpty()) {
            if (regionList.isEmpty()) {
                stop();
                hivePlugin().getLogger().info("[Placer] finished " + world.getName());
                return;
            }
            final Vec2i region = regionList.remove(regionList.size() - 1);
            final int cx = region.x << 5;
            final int cz = region.z << 5;
            for (int z = 0; z < 32; z += 1) {
                for (int x = 0; x < 32; x += 1) {
                    final Vec2i chunk = Vec2i.of(cx + x, cz + z);
                    if (!worldBorderCuboid.contains(chunk.x << 4, 0, chunk.z << 4)) continue;
                    if (!worldBorderCuboid.contains((chunk.x << 4) + 15, 0, (chunk.z << 4) + 15)) continue;
                    chunkList.add(chunk);
                }
            }
            Collections.shuffle(chunkList);
            hivePlugin().getLogger().info("[Placer] Start region " + world.getName() + " " + region.x + " " + region.z
                                          + " " + chunkList.size() + " chunks");
        } else {
            final Vec2i chunk = chunkList.remove(chunkList.size() - 1);
            busy = true;
            world.getChunkAtAsync(chunk.x, chunk.z, (Consumer<Chunk>) this::onChunkLoaded);
        }
    }

    private void onChunkLoaded(Chunk chunk) {
        busy = false;
        final int bx = (chunk.getX() << 4) + 1 + random.nextInt(14);
        final int bz = (chunk.getZ() << 4) + 1 + random.nextInt(14);
        final Block floor = world.getHighestBlockAt(bx, bz);
        if (floor.getRelative(0, 1, 0).isLiquid()) return;
        if (!isGoodFloor(floor.getType())) return;
        final Block nborN = world.getHighestBlockAt(bx, bz - 1);
        if (!isGoodFloor(nborN.getType()) || nborN.getY() != floor.getY()) return;
        final Block nborS = world.getHighestBlockAt(bx, bz + 1);
        if (!isGoodFloor(nborS.getType()) || nborS.getY() != floor.getY()) return;
        final Block nborW = world.getHighestBlockAt(bx - 1, bz);
        if (!isGoodFloor(nborW.getType()) || nborW.getY() != floor.getY()) return;
        final Block nborE = world.getHighestBlockAt(bx + 1, bz);
        if (!isGoodFloor(nborE.getType()) || nborE.getY() != floor.getY()) return;
        final Block hiveBlock = floor.getRelative(0, 3, 0);
        final Cuboid hiveArea = new Cuboid(hiveBlock.getX() - 32, hiveBlock.getY() - 16, hiveBlock.getZ() - 32,
                                           hiveBlock.getX() + 32, hiveBlock.getY() + 16, hiveBlock.getZ() + 32);
        final Cuboid expandedHiveArea = hiveArea.outset(240, 0, 240);
        // Cannot overlap any existing structures
        if (!structurePlugin().getStructureCache().within(world.getName(), hiveArea).isEmpty()) {
            return;
        }
        // Cannot be too close to world border
        if (!worldBorderCuboid.contains(expandedHiveArea)) {
            return;
        }
        // Cannot be too close to any placed hive
        for (Cuboid placed : placedHives) {
            if (placed.overlaps(expandedHiveArea)) {
                return;
            }
        }
        // No return
        chunkList.clear();
        placedHives.add(hiveArea);
        if (shouldPlaceForReal) {
            hivePlugin().getLogger().info("[Placer] place at " + hiveBlock.getX() + " " + hiveBlock.getY() + " " + hiveBlock.getZ());
            final Structure structure = new Structure(world.getName(), new NamespacedKey(hivePlugin(), "hive"), Vec2i.of(chunk), hiveArea, "{}", false);
            structurePlugin().getStructureCache().addStructure(structure);
            floor.setType(Material.BEDROCK);
            nborN.setType(Material.BEDROCK);
            nborS.setType(Material.BEDROCK);
            nborW.setType(Material.BEDROCK);
            nborE.setType(Material.BEDROCK);
            hiveBlock.setType(Material.TRIAL_SPAWNER);
            hivePlugin().getBlockRegistryEntry().set(hiveBlock);
        } else {
            hivePlugin().getLogger().info("[Placer] WOULD place at " + hiveBlock.getX() + " " + hiveBlock.getY() + " " + hiveBlock.getZ());
        }
    }

    private static boolean isGoodFloor(Material material) {
        switch (material) {
        case GRASS_BLOCK:
        case SAND:
        case RED_SAND:
        case MYCELIUM:
        case PODZOL:
        case TERRACOTTA:
            return true;
        default:
            if (Tag.BADLANDS_TERRACOTTA.isTagged(material)) return true;
            return false;
        }
    }
}
