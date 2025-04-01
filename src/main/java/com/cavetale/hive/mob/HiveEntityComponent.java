package com.cavetale.hive.mob;

import com.cavetale.hive.Hive;
import com.cavetale.hive.HivePlugin;
import com.cavetale.mytems.entity.component.EntityComponent;
import com.cavetale.mytems.entity.component.EntityComponentTag;
import lombok.Data;
import org.bukkit.entity.Entity;

@Data
public final class HiveEntityComponent implements EntityComponent {
    private final Hive hive;
    private final SpawnMob spawnMob;
    private boolean boss;
    private boolean glowing;

    @Override
    public HivePlugin getPlugin() {
        return HivePlugin.hivePlugin();
    }

    public static boolean isHiveMob(Entity entity) {
        return get(entity) != null;
    }

    public static HiveEntityComponent get(Entity entity) {
        return EntityComponentTag.getComponent(entity, HiveEntityComponent.class);
    }
}
