package me.libelula.pb;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import java.sql.SQLException;
import java.util.List;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitRunnable;

public class TaskManager
  extends BukkitRunnable
{
  private final LibelulaProtectionBlocks plugin;
  private final Task task;
  private final Object[] objects;
  
  private static enum Task
  {
    CHECK_FOR_BANNED,  CHECK_PLACED_BLOCK,  DEL_AVLB_ID_DB,  DISABLE_OLDPS,  IMPORT,  INS_AVLB_ID_DB,  INS_PSB_INTO_DB,  LOAD_PBS,  NEW_PROTECTION,  REGISTER_COMMANDS,  REMOVE_PROTECTION_BLOCK,  RESTORE_BLOCK,  UPDATE_PSBLOCKS,  PUT_FENCE;
    
    private Task() {}
  }
  
  private TaskManager(LibelulaProtectionBlocks plugin, Task task, Object[] objects)
  {
    this.plugin = plugin;
    this.task = task;
    this.objects = objects;
  }
  
  public void run()
  {
    Byte data;
    switch (this.task)
    {
    case DISABLE_OLDPS: 
      if ((this.objects.length == 1) && ((this.objects[0] instanceof Plugin)))
      {
        disableOldFashionedPluginSync((Plugin)this.objects[0]);
        new TaskManager(this.plugin, Task.REGISTER_COMMANDS, null).runTask(this.plugin);
      }
      break;
    case REGISTER_COMMANDS: 
      new Commands(this.plugin);
      break;
    case IMPORT: 
      importFromPSWhenWGIsEnabled(this.plugin);
      break;
    case LOAD_PBS: 
      try
      {
        this.plugin.sql.loadPSBlocks(this.plugin.pbs);
      }
      catch (Exception ex)
      {
        this.plugin.getLogger().severe("Error importing Protection Blocks from DB: ".concat(ex.toString()));
      }
    case INS_PSB_INTO_DB: 
      if ((this.objects.length == 1) && ((this.objects[0] instanceof ProtectionBlocks.PSBlocks))) {
        try
        {
          this.plugin.sql.insertPSBlocks((ProtectionBlocks.PSBlocks)this.objects[0]);
        }
        catch (SQLException ex)
        {
          this.plugin.getLogger().severe("Error inserting Protection Blocks into DB: ".concat(ex.toString()));
        }
      }
      break;
    case INS_AVLB_ID_DB: 
      if ((this.objects.length == 1) && ((this.objects[0] instanceof String))) {
        try
        {
          this.plugin.sql.insertAvailableIDs((String)this.objects[0]);
        }
        catch (SQLException ex)
        {
          this.plugin.getLogger().severe("Error inserting Available ID into DB: ".concat(ex.toString()));
        }
      }
      break;
    case CHECK_PLACED_BLOCK: 
      if ((this.objects.length == 6) && ((this.objects[0] instanceof ItemMeta)) && ((this.objects[1] instanceof String)) && ((this.objects[2] instanceof Material)) && ((this.objects[3] instanceof Byte)) && ((this.objects[4] instanceof Location)) && ((this.objects[5] instanceof Integer))) {
        checkProtectionBlockPlaced((ItemMeta)this.objects[0], (String)this.objects[1], (Material)this.objects[2], (Byte)this.objects[3], (Location)this.objects[4], ((Integer)this.objects[5])
          .intValue());
      }
      break;
    case REMOVE_PROTECTION_BLOCK: 
      if ((this.objects.length == 1) && ((this.objects[0] instanceof Location))) {
        try
        {
          this.plugin.sql.removePSBlocks((Location)this.objects[0]);
        }
        catch (SQLException ex)
        {
          this.plugin.getLogger().severe("Error removing Protection Blocks into DB: ".concat(ex.toString()));
        }
      }
      break;
    case NEW_PROTECTION: 
      if ((this.objects.length == 6) && ((this.objects[0] instanceof ProtectedCuboidRegion)) && ((this.objects[1] instanceof Location)) && ((this.objects[2] instanceof String)) && ((this.objects[3] instanceof ItemMeta)) && ((this.objects[4] instanceof Material)) && ((this.objects[5] instanceof Byte)))
      {
        Player player = this.plugin.getServer().getPlayer((String)this.objects[2]);
        if (player == null) {
          return;
        }
        this.plugin.pbs.newBlock((ProtectedCuboidRegion)this.objects[0], (Location)this.objects[1], player, (ItemMeta)this.objects[3], (Material)this.objects[4], (Byte)this.objects[5]);
      }
      break;
    case DEL_AVLB_ID_DB: 
      if ((this.objects.length == 1) && ((this.objects[0] instanceof String))) {
        try
        {
          this.plugin.sql.delAvailableIDs((String)this.objects[0]);
        }
        catch (SQLException ex)
        {
          this.plugin.getLogger().severe("Error deleting Available ID from DB: ".concat(ex.toString()));
        }
      }
      break;
    case RESTORE_BLOCK: 
      if ((this.objects.length == 3) && ((this.objects[0] instanceof Location)) && ((this.objects[1] instanceof Material)) && ((this.objects[2] instanceof Byte)))
      {
        Location loc = (Location)this.objects[0];
        Material mat = (Material)this.objects[1];
        data = (Byte)this.objects[2];
        loc.getBlock().setTypeIdAndData(mat.getId(), data.byteValue(), true);
      }
      break;
    case UPDATE_PSBLOCKS: 
      if ((this.objects.length == 1) && ((this.objects[0] instanceof ProtectionBlocks.PSBlocks))) {
        try
        {
          this.plugin.sql.updatePSBlocks((ProtectionBlocks.PSBlocks)this.objects[0]);
        }
        catch (SQLException ex)
        {
          this.plugin.getLogger().severe("Error updating Protection Block on DB: ".concat(ex.toString()));
        }
      }
      break;
    case CHECK_FOR_BANNED: 
      if ((this.objects.length == 1) && ((this.objects[0] instanceof String)))
      {
        String playerName = (String)this.objects[0];
        ProtectionBlocks.PSBlocks[] protectionList = this.plugin.pbs.getOwnedPSList(playerName);
        if (protectionList.length > 0)
        {
          this.plugin.getLogger().log(Level.INFO, "{0} {1} {2} {3} {4}", new Object[] { this.plugin.i18n.getText("banned_player"), playerName, this.plugin.i18n.getText("is_owner_of"), Integer.valueOf(protectionList.length), this.plugin.i18n.getText("placed_protections") });
          this.plugin.getLogger().log(Level.INFO, "{0} {1}", new Object[] { this.plugin.i18n.getText("remove_all_ps"), playerName });
          for (Player onLinePlayer : this.plugin.getServer().getOnlinePlayers()) {
            if (onLinePlayer.isOp())
            {
              onLinePlayer.sendMessage(ChatColor.YELLOW + this.plugin.i18n
                .getText("banned_player") + " " + playerName + " " + this.plugin.i18n
                .getText("is_owner_of") + " " + protectionList.length + " " + this.plugin.i18n
                .getText("placed_protections"));
              onLinePlayer.sendMessage(ChatColor.YELLOW + this.plugin.i18n.getText("remove_all_ps") + " " + playerName);
            }
          }
        }
      }
      break;
    case PUT_FENCE: 
      if ((this.objects.length == 2) && ((this.objects[0] instanceof World)) && ((this.objects[1] instanceof TreeSet))) {
        FlagsProcessor.putFence(this.plugin, (World)this.objects[0], (TreeSet)this.objects[1]);
      }
      break;
    }
  }
  
  public static void disablePSAndLoadCommands(Plugin oldPS, LibelulaProtectionBlocks plugin)
  {
    Object[] objects = { oldPS };
    new TaskManager(plugin, Task.DISABLE_OLDPS, objects).runTask(plugin);
  }
  
  public static void registerCommands(LibelulaProtectionBlocks plugin)
  {
    new TaskManager(plugin, Task.REGISTER_COMMANDS, null).runTaskLater(plugin, 20L);
  }
  
  private void disableOldFashionedPluginSync(Plugin oldPS)
  {
    if (oldPS.isEnabled())
    {
      this.plugin.getServer().getPluginManager().disablePlugin(oldPS);
      this.plugin.getServer().getConsoleSender().sendMessage(ChatColor.YELLOW + "Incompatible old fashioned ProtectionStones plugin disabled!");
    }
    else
    {
      this.plugin.getLogger().info("Old fashioned ProtectionStones plugin found but disabled.");
    }
  }
  
  public static void importFromPSWhenWGIsEnabled(LibelulaProtectionBlocks plugin)
  {
    if (plugin.wgm.isEnabled()) {
      Importer.importFromPS(plugin);
    } else {
      new TaskManager(plugin, Task.IMPORT, null).runTaskLater(plugin, 10L);
    }
  }
  
  public static void loadProtectionBlocks(LibelulaProtectionBlocks plugin)
  {
    new TaskManager(plugin, Task.LOAD_PBS, null).runTaskAsynchronously(plugin);
  }
  
  public static void addPSBlock(ProtectionBlocks.PSBlocks psb, LibelulaProtectionBlocks plugin)
  {
    Object[] objects = { psb };
    new TaskManager(plugin, Task.INS_PSB_INTO_DB, objects).runTaskAsynchronously(plugin);
  }
  
  public static void addAvailableID(String hash, LibelulaProtectionBlocks plugin)
  {
    Object[] objects = { hash };
    new TaskManager(plugin, Task.INS_AVLB_ID_DB, objects).runTaskAsynchronously(plugin);
  }
  
  public static void removeAvailableID(String hash, LibelulaProtectionBlocks plugin)
  {
    Object[] objects = { hash };
    new TaskManager(plugin, Task.DEL_AVLB_ID_DB, objects).runTaskAsynchronously(plugin);
  }
  
  public static void protectionBlockPlaced(BlockPlaceEvent e, int values, LibelulaProtectionBlocks plugin)
  {
    ItemMeta dtMeta = e.getPlayer().getItemInHand().getItemMeta();
    String playerName = e.getPlayer().getName();
    Material material = e.getBlock().getType();
    Byte materialData = Byte.valueOf(e.getBlock().getData());
    Location location = e.getBlock().getLocation();
    Object[] objects = { dtMeta, playerName, material, materialData, location, Integer.valueOf(values) };
    new TaskManager(plugin, Task.CHECK_PLACED_BLOCK, objects).runTaskAsynchronously(plugin);
  }
  
  private void checkProtectionBlockPlaced(ItemMeta dtMeta, String playerName, Material material, Byte materialData, Location location, int values)
  {
    Pattern p = Pattern.compile("-?\\d+");
    Matcher m = p.matcher(dtMeta.getDisplayName());
    int z;
    if (values == 2)
    {
      m.find();
      int x = Integer.parseInt(m.group());
      int y = 0;
      m.find();
      z = Integer.parseInt(m.group());
    }
    else
    {
      int z;
      if (values == 3)
      {
        m.find();
        int x = Integer.parseInt(m.group());
        m.find();
        int y = Integer.parseInt(m.group());
        m.find();
        z = Integer.parseInt(m.group());
      }
      else
      {
        return;
      }
    }
    int z;
    int y;
    int x;
    if (dtMeta.getLore().size() != 3) {
      return;
    }
    String hash = ProtectionController.getHashFromValues(x, y, z, material.getId());
    if (!((String)dtMeta.getLore().get(2)).startsWith(hash))
    {
      this.plugin.getLogger().log(Level.WARNING, "{0} " + this.plugin.i18n.getText("ruined_stone") + " ({1})", new Object[] { playerName, hash });
      
      return;
    }
    if (!this.plugin.pc.containsSync((String)dtMeta.getLore().get(2)))
    {
      this.plugin.getLogger().log(Level.WARNING, "{0} " + this.plugin.i18n.getText("missing_stone") + " ({1})", new Object[] { playerName, dtMeta
        .getLore().get(2) });
      return;
    }
    ProtectedCuboidRegion cuboidRegion = this.plugin.wgm.getPBregion(location, x, y, z, playerName);
    
    cuboidRegion.setFlags(this.plugin.config.getFlags(playerName));
    
    Object[] objects1 = { cuboidRegion, location, playerName, dtMeta, material, materialData };
    new TaskManager(this.plugin, Task.NEW_PROTECTION, objects1).runTask(this.plugin);
  }
  
  public static void removeProtectionBlock(Location location, LibelulaProtectionBlocks plugin)
  {
    Object[] objects = { location };
    new TaskManager(plugin, Task.REMOVE_PROTECTION_BLOCK, objects).runTaskAsynchronously(plugin);
  }
  
  public static void restoreBlock(Location loc, Material mat, Byte data, LibelulaProtectionBlocks plugin)
  {
    Object[] objects = { loc, mat, data };
    new TaskManager(plugin, Task.RESTORE_BLOCK, objects).runTask(plugin);
  }
  
  public static void updatePSBlocks(ProtectionBlocks.PSBlocks psb, LibelulaProtectionBlocks plugin)
  {
    Object[] objects = { psb };
    new TaskManager(plugin, Task.UPDATE_PSBLOCKS, objects).runTask(plugin);
  }
  
  public static void checkBannedForStones(LibelulaProtectionBlocks plugin, String playerName)
  {
    if (!plugin.bannedAdvicedPlayers.contains(playerName))
    {
      plugin.bannedAdvicedPlayers.add(playerName);
      Object[] objects = { playerName };
      new TaskManager(plugin, Task.CHECK_FOR_BANNED, objects).runTask(plugin);
    }
  }
  
  public static void putFence(LibelulaProtectionBlocks plugin, World world, TreeSet<BlockVector> blockVectors)
  {
    Object[] objects = { world, blockVectors };
    new TaskManager(plugin, Task.PUT_FENCE, objects).runTask(plugin);
  }
}
