package me.libelula.pb;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class Listener
  implements org.bukkit.event.Listener
{
  private final LibelulaProtectionBlocks plugin;
  
  public Listener(LibelulaProtectionBlocks plugin)
  {
    this.plugin = plugin;
  }
  
  @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
  public void onBlockPlace(BlockPlaceEvent e)
  {
    if ((e.getPlayer().getItemInHand().getItemMeta().getLore() != null) && 
      (e.getPlayer().getItemInHand().getItemMeta().getLore().size() == 3) && 
      (e.getPlayer().getItemInHand().getItemMeta().getDisplayName() != null))
    {
      ItemMeta dtMeta = e.getPlayer().getItemInHand().getItemMeta();
      int valuesFound = 0;
      if (dtMeta.getDisplayName().matches("(.*)\\s(\\d+)\\sx\\s(\\d+)\\sx\\s(\\d+)(.*)")) {
        valuesFound = 3;
      } else if (dtMeta.getDisplayName().matches("(.*)\\s(\\d+)\\sx\\s?\\sx\\s(\\d+)(.*)")) {
        valuesFound = 2;
      }
      if (valuesFound != 0) {
        TaskManager.protectionBlockPlaced(e, valuesFound, this.plugin);
      }
    }
  }
  
  @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
  public void onItemDrop(ItemSpawnEvent e)
  {
    if (this.plugin.pbs.removeDropEventCancellation(e.getLocation())) {
      e.setCancelled(true);
    }
  }
  
  @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
  public void onBlockBreak(BlockBreakEvent e)
  {
    if (this.plugin.pbs.matches(e.getBlock()))
    {
      ProtectionBlocks.PSBlocks pbInfo = this.plugin.pbs.get(e.getBlock().getLocation());
      if (pbInfo.hidden) {
        return;
      }
      if ((pbInfo.region.isOwner(e.getPlayer().getName())) || 
        (e.getPlayer().isOp()) || (e.getPlayer().hasPermission("pb.break.others")))
      {
        if (this.plugin.pbs.removeProtectionBlock(e.getBlock().getLocation(), e.getPlayer())) {
          e.getPlayer().sendMessage(ChatColor.GREEN + this.plugin.i18n.getText("protection_block_removed"));
        }
      }
      else
      {
        this.plugin.pbs.addDropEventCancellation(e.getBlock().getLocation(), e.getBlock().getDrops().size());
        TaskManager.restoreBlock(e.getBlock().getLocation(), e.getBlock().getType(), Byte.valueOf(e.getBlock().getData()), this.plugin);
        e.getPlayer().sendMessage(ChatColor.RED + this.plugin.i18n.getText("only_owner_can"));
      }
    }
  }
  
  @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
  public void onSignEdit(SignChangeEvent e)
  {
    if (e.getLine(0).toLowerCase().startsWith("[lpb]"))
    {
      PbSizes pbSizes = parseSize(e.getLine(1));
      if (pbSizes == null) {
        return;
      }
      if ((!e.getPlayer().hasPermission("pb.shop.create")) && (!e.getPlayer().isOp()))
      {
        e.setCancelled(true);
        e.getPlayer().sendMessage(ChatColor.RED + this.plugin.i18n.getText("you_dont_have_permissions"));
        return;
      }
      try
      {
        price = Double.parseDouble(e.getLine(2).replace("$", ""));
      }
      catch (NumberFormatException ex)
      {
        double price;
        e.setLine(2, ChatColor.STRIKETHROUGH + e.getLine(2));
        e.getPlayer().sendMessage(ChatColor.RED + this.plugin.i18n.getText("invalid_price")); return;
      }
      double price;
      Material material = Material.getMaterial(e.getLine(3).toUpperCase());
      if ((material == null) || (!material.isBlock()) || (material.hasGravity()) || (!ProtectionController.isMaterialSuitable(new ItemStack(material))))
      {
        e.getPlayer().sendMessage(ChatColor.RED + this.plugin.i18n.getText("invalid_material"));
        e.setLine(3, ChatColor.STRIKETHROUGH + e.getLine(3));
        return;
      }
      if (((pbSizes.length & 0x1) == 0) || ((pbSizes.width & 0x1) == 0) || ((pbSizes.height & 0x1) == 0))
      {
        e.getPlayer().sendMessage(ChatColor.RED + this.plugin.i18n.getText("values_must_be_odd"));
        e.setLine(1, ChatColor.STRIKETHROUGH + e.getLine(1));
        return;
      }
      if (this.plugin.eco == null)
      {
        e.setCancelled(true);
        e.getPlayer().sendMessage(ChatColor.RED + this.plugin.i18n.getText("vault_not_found"));
        return;
      }
      e.setLine(1, pbSizes.length + " x " + pbSizes.height + " x " + pbSizes.width);
      e.setLine(2, "$ " + price);
      e.setLine(3, material.name());
      e.getPlayer().sendMessage(ChatColor.GREEN + this.plugin.i18n.getText("shop_created"));
    }
  }
  
  @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
  public void onPlayerUse(PlayerInteractEvent event)
  {
    if (event.getAction() == Action.RIGHT_CLICK_BLOCK)
    {
      if (this.plugin.eco == null) {
        return;
      }
      if ((event.getClickedBlock().getType() == Material.WALL_SIGN) || 
        (event.getClickedBlock().getType() == Material.SIGN_POST))
      {
        Sign e = (Sign)event.getClickedBlock().getState();
        if (!e.getLine(0).toLowerCase().startsWith("[lpb]")) {
          return;
        }
        PbSizes pbSizes = parseSize(e.getLine(1));
        if (pbSizes == null) {
          return;
        }
        try
        {
          price = Double.parseDouble(e.getLine(2).replace("$", ""));
        }
        catch (NumberFormatException ex)
        {
          double price;
          return;
        }
        double price;
        Material material = Material.getMaterial(e.getLine(3).toUpperCase());
        if ((material == null) || (!material.isBlock()) || (material.hasGravity()) || (!ProtectionController.isMaterialSuitable(new ItemStack(material)))) {
          return;
        }
        if (((pbSizes.length & 0x1) == 0) || ((pbSizes.width & 0x1) == 0) || ((pbSizes.height & 0x1) == 0)) {
          return;
        }
        if (this.plugin.eco.getBalance(event.getPlayer().getName()) < price)
        {
          event.getPlayer().sendMessage(ChatColor.RED + this.plugin.i18n.getText("not_enough_money"));
          return;
        }
        if (event.getPlayer().getItemInHand().getType() != Material.AIR)
        {
          event.getPlayer().sendMessage(ChatColor.RED + this.plugin.i18n.getText("not_empty_hand"));
          return;
        }
        event.getPlayer().setItemInHand(new ItemStack(material));
        String[] flags = e.getLine(0).split("\\+");
        boolean result;
        boolean result;
        if (flags.length > 1) {
          result = this.plugin.pc.createPBFromItemsInHand(event.getPlayer(), pbSizes.length, pbSizes.height, pbSizes.width, flags[1]);
        } else {
          result = this.plugin.pc.createPBFromItemsInHand(event.getPlayer(), pbSizes.length, pbSizes.height, pbSizes.width);
        }
        if (result)
        {
          this.plugin.eco.withdrawPlayer(event.getPlayer().getName(), price);
          event.getPlayer().sendMessage(ChatColor.GREEN + "Cost: " + e.getLine(2));
          this.plugin.getLogger().log(Level.INFO, "The player {0} has bought {1} ({2}) {3}", new Object[] {event
            .getPlayer().getName(), e.getLine(3), e.getLine(1), e.getLine(2) });
        }
      }
    }
  }
  
  private PbSizes parseSize(String line)
  {
    PbSizes pbSizes = new PbSizes(null);
    if (!line.matches("(.*)\\s*(\\d+)\\s*x\\s*(\\d+)\\s*x\\s*(\\d+)(.*)")) {
      return null;
    }
    Pattern p = Pattern.compile("-?\\d+");
    Matcher m = p.matcher(line);
    m.find();
    pbSizes.length = Integer.parseInt(m.group());
    m.find();
    pbSizes.height = Integer.parseInt(m.group());
    m.find();
    pbSizes.width = Integer.parseInt(m.group());
    return pbSizes;
  }
  
  @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
  public void onPistonExtend(BlockPistonExtendEvent e)
  {
    for (Block pushedBlock : e.getBlocks()) {
      if ((this.plugin.pbs.contains(pushedBlock.getLocation())) && 
        (!this.plugin.pbs.get(pushedBlock.getLocation()).hidden))
      {
        e.setCancelled(true);
        return;
      }
    }
    switch (e.getDirection())
    {
    case NORTH: 
      if (e.getLength() != 0)
      {
        if (this.plugin.pbs.contains(((Block)e.getBlocks().get(e.getLength() - 1)).getLocation().subtract(0.0D, 0.0D, 1.0D))) {
          e.setCancelled(true);
        }
      }
      else if (this.plugin.pbs.contains(e.getBlock().getLocation().subtract(0.0D, 0.0D, 1.0D))) {
        e.setCancelled(true);
      }
      break;
    case SOUTH: 
      if (e.getLength() != 0)
      {
        if (this.plugin.pbs.contains(((Block)e.getBlocks().get(e.getLength() - 1)).getLocation().add(0.0D, 0.0D, 1.0D))) {
          e.setCancelled(true);
        }
      }
      else if (this.plugin.pbs.contains(e.getBlock().getLocation().add(0.0D, 0.0D, 1.0D))) {
        e.setCancelled(true);
      }
      break;
    case WEST: 
      if (e.getLength() != 0)
      {
        if (this.plugin.pbs.contains(((Block)e.getBlocks().get(e.getLength() - 1)).getLocation().subtract(1.0D, 0.0D, 0.0D))) {
          e.setCancelled(true);
        }
      }
      else if (this.plugin.pbs.contains(e.getBlock().getLocation().subtract(1.0D, 0.0D, 0.0D))) {
        e.setCancelled(true);
      }
      break;
    case EAST: 
      if (e.getLength() != 0)
      {
        if (this.plugin.pbs.contains(((Block)e.getBlocks().get(e.getLength() - 1)).getLocation().add(1.0D, 0.0D, 0.0D))) {
          e.setCancelled(true);
        }
      }
      else if (this.plugin.pbs.contains(e.getBlock().getLocation().add(1.0D, 0.0D, 0.0D))) {
        e.setCancelled(true);
      }
      break;
    case DOWN: 
      if (e.getLength() != 0)
      {
        if (this.plugin.pbs.contains(((Block)e.getBlocks().get(e.getLength() - 1)).getLocation().subtract(0.0D, 1.0D, 0.0D))) {
          e.setCancelled(true);
        }
      }
      else if (this.plugin.pbs.contains(e.getBlock().getLocation().subtract(0.0D, 1.0D, 0.0D))) {
        e.setCancelled(true);
      }
      break;
    case UP: 
      if (e.getLength() != 0)
      {
        if (this.plugin.pbs.contains(((Block)e.getBlocks().get(e.getLength() - 1)).getLocation().add(0.0D, 1.0D, 0.0D))) {
          e.setCancelled(true);
        }
      }
      else if (this.plugin.pbs.contains(e.getBlock().getLocation().add(0.0D, 1.0D, 0.0D))) {
        e.setCancelled(true);
      }
      break;
    }
  }
  
  @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
  public void onPistonRetract(BlockPistonRetractEvent e)
  {
    if ((e.isSticky()) && (this.plugin.pbs.contains(e.getRetractLocation()))) {
      e.setCancelled(true);
    }
  }
  
  @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
  public void onPlayerKick(PlayerKickEvent e)
  {
    if (e.getPlayer().isBanned()) {
      TaskManager.checkBannedForStones(this.plugin, e.getPlayer().getName());
    }
  }
  
  @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
  public void onPlayerCommandPreprocessEvent(PlayerCommandPreprocessEvent e)
  {
    if ((e.getMessage().toLowerCase().startsWith("/ban")) && 
      (e.getMessage().split(" ").length >= 2))
    {
      OfflinePlayer bannedPlayer = this.plugin.getServer().getOfflinePlayer(e.getMessage().split(" ")[1]);
      if (bannedPlayer != null) {
        TaskManager.checkBannedForStones(this.plugin, bannedPlayer.getName());
      }
    }
  }
  
  private class PbSizes
  {
    int length;
    int height;
    int width;
    
    private PbSizes() {}
  }
}
