package com.cavetale.hive;

import com.cavetale.mytems.block.BlockEventHandler;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import static com.cavetale.core.font.Unicode.subscript;
import static com.cavetale.core.font.Unicode.superscript;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextColor.color;
import static net.kyori.adventure.text.format.TextDecoration.*;

public final class HiveBlockEventHandler implements BlockEventHandler {
    @Override
    public void onPlayerInteract(PlayerInteractEvent event, Block block) {
        switch (event.getAction()) {
        case LEFT_CLICK_BLOCK: case RIGHT_CLICK_BLOCK: break;
        default: return;
        }
        switch (event.getHand()) {
        case HAND: break;
        default: return;
        }
        Hive hive = Hive.at(block);
        if (hive != null) return;
        hive = hive.wakeUp(block);
    }

    @Override
    public void onCustomBlockDamage(Player player, Block block, ItemStack item, int ticks) {
        final int max = 200;
        if (item == null && item.getType() != Material.DIAMOND_PICKAXE && item.getType() != Material.NETHERITE_PICKAXE) {
            return;
        }
        final int bars = 20;
        final int full = (ticks * bars) / max;
        final int empty = bars - full;
        player.sendActionBar(textOfChildren(text("Hive ", GOLD, BOLD),
                                            text(superscript(String.format("%02d", ticks)) + "/" + subscript(max), GRAY),
                                            text("|".repeat(full), GOLD), text("|".repeat(empty), color(0x303030))));
        if (ticks < max) return;
        Hive hive = Hive.at(block);
        if (hive == null) return;
        hive.breakBlock(player);
    }

    @Override
    public void onBlockBreak(BlockBreakEvent event, Block block) {
        Hive hive = Hive.at(block);
        if (hive != null) hive.disable();
        hive.breakBlock(event.getPlayer());
    }
}
