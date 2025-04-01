package com.cavetale.hive;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.hive.mob.SpawnMob;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class HiveCommand extends AbstractCommand<HivePlugin> {
    protected HiveCommand(final HivePlugin plugin) {
        super(plugin, "hive");
    }

    @Override
    protected void onEnable() {
        rootNode.addChild("create").denyTabCompletion()
            .description("Create a hive")
            .playerCaller(this::create);
        rootNode.addChild("hibernate").denyTabCompletion()
            .description("Hibernate current hive")
            .playerCaller(this::hibernate);
        rootNode.addChild("wave").arguments("<wave>")
            .description("Set current wave")
            .completers(CommandArgCompleter.integer(i -> i > 0))
            .playerCaller(this::wave);
        rootNode.addChild("testing").arguments("true|false")
            .description("Set testing status")
            .completers(CommandArgCompleter.BOOLEAN)
            .playerCaller(this::testing);
        rootNode.addChild("clear").denyTabCompletion()
            .description("Clear all mobs")
            .playerCaller(this::clear);
        rootNode.addChild("place").arguments("<world>")
            .description("Place in world")
            .completers(CommandArgCompleter.supplyList(() -> {
                        final List<String> result = new ArrayList<>();
                        for (World world : Bukkit.getWorlds()) {
                            result.add(world.getName());
                        }
                        return result;
                    }))
            .senderCaller(this::place);
        rootNode.addChild("summon").arguments("<type>")
            .description("Spawn a mob")
            .completers(CommandArgCompleter.supplyList(() -> {
                        final List<String> result = new ArrayList<>();
                        for (SpawnMob spawnMob : SpawnMob.getAllMobs()) {
                            result.add(spawnMob.getName());
                        }
                        return result;
                    }))
            .playerCaller(this::summon);
    }

    private void create(Player player) {
        final Block block = player.getTargetBlockExact(5);
        if (block == null) throw new CommandWarn("Must look at block");
        final Hive hive = new Hive(block);
        hive.enable();
        player.sendMessage(text("Hive created at " + block.getX() + " " + block.getY() + " " + block.getZ(), YELLOW));
    }

    private void hibernate(Player player) {
        final Hive hive = Hive.at(player.getLocation());
        if (hive == null) throw new CommandWarn("There is no hive here!");
        hive.hibernate();
        player.sendMessage(text("Wave hibernated", YELLOW));
    }

    private boolean wave(Player player, String[] args) {
        if (args.length != 1) return false;
        final int wave = CommandArgCompleter.requireInt(args[0], i -> i > 0);
        final Hive hive = Hive.at(player.getLocation());
        if (hive == null) throw new CommandWarn("There is no hive here!");
        hive.setLevel(wave);
        player.sendMessage(text("Wave set to " + wave, YELLOW));
        return true;
    }

    private boolean testing(Player player, String[] args) {
        if (args.length != 1) return false;
        final boolean value = CommandArgCompleter.requireBoolean(args[0]);
        final Hive hive = Hive.at(player.getLocation());
        if (hive == null) throw new CommandWarn("There is no hive here!");
        hive.setTesting(value);
        player.sendMessage(text("Testing set to " + value, YELLOW));
        return true;
    }

    private void clear(Player player) {
        final Hive hive = Hive.at(player.getLocation());
        if (hive == null) throw new CommandWarn("There is no hive here!");
        hive.removeAllSpawnedMobs();
        player.sendMessage(text("Spawned mobs cleared", YELLOW));
    }

    private boolean place(CommandSender sender, String[] args) {
        if (!(sender instanceof ConsoleCommandSender)) {
            throw new CommandWarn("Console required!");
        }
        if (args.length != 1) return false;
        final String worldName = args[0];
        final World world = Bukkit.getWorld(worldName);
        if (world == null) {
            throw new CommandWarn("World not found: " + worldName);
        }
        if (world.getWorldBorder().getSize() > 32768.0) {
            throw new CommandWarn("World border too large: " + world.getName() + ", " + world.getWorldBorder().getSize());
        }
        new HivePlacer(world).start();
        sender.sendMessage(text("Placer started in world " + world.getName(), YELLOW));
        return true;
    }

    private boolean summon(Player player, String[] args) {
        if (args.length != 1) return false;
        final String name = args[0];
        SpawnMob spawnMob = null;
        for (SpawnMob it : SpawnMob.getAllMobs()) {
            if (it.getName().equals(name)) {
                spawnMob = it;
                break;
            }
        }
        if (spawnMob == null) {
            throw new CommandWarn("Mob not found: " + name);
        }
        spawnMob.spawn(player.getLocation());
        return true;
    }
}
