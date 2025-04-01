package com.cavetale.hive;

import com.cavetale.hive.mob.SpawnMob;
import com.cavetale.hive.mob.boss.SpawnBoss;
import com.cavetale.mytems.block.BlockRegistryEntry;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;
import static com.cavetale.mytems.MytemsPlugin.mytemsPlugin;

@Getter
public final class HivePlugin extends JavaPlugin {
    private static HivePlugin instance;
    private final HiveCommand hiveCommand = new HiveCommand(this);
    private final HiveListener hiveListener = new HiveListener();
    private BlockRegistryEntry blockRegistryEntry;

    public HivePlugin() {
        instance = this;
    }

    @Override
    public void onEnable() {
        hiveCommand.enable();
        hiveListener.enable();
        SpawnMob.init();
        SpawnBoss.init();
        this.blockRegistryEntry = mytemsPlugin().getBlockRegistry().register("hive");
        blockRegistryEntry.addEventHandler(new HiveBlockEventHandler());
    }

    @Override
    public void onDisable() {
        Hive.disableAll();
    }

    public static HivePlugin hivePlugin() {
        return instance;
    }
}
