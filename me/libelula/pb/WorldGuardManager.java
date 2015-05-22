package me.libelula.pb;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.managers.storage.StorageException;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.util.Comparator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

public class WorldGuardManager
{
  private final Plugin plugin;
  private WorldGuardPlugin wgp;
  
  public static class FlagComparator
    implements Comparator<Flag<?>>
  {
    public int compare(Flag<?> o1, Flag<?> o2)
    {
      return o1.toString().compareTo(o2.toString());
    }
  }
  
  public WorldGuardManager(Plugin plugin)
  {
    this.plugin = plugin;
    initialize();
  }
  
  public boolean isWorldGuardActive()
  {
    if (this.wgp != null) {
      return true;
    }
    return false;
  }
  
  private void initialize()
  {
    Plugin wgPlugin = this.plugin.getServer().getPluginManager().getPlugin("WorldGuard");
    if ((wgPlugin == null) || (!(wgPlugin instanceof WorldGuardPlugin)))
    {
      this.wgp = null;
      return;
    }
    this.wgp = ((WorldGuardPlugin)wgPlugin);
  }
  
  public Map<String, ProtectedRegion> getRegions(World world)
  {
    return this.wgp.getRegionManager(world).getRegions();
  }
  
  public boolean isEnabled()
  {
    if (this.wgp != null) {
      return (this.wgp.isInitialized()) && (this.wgp.isEnabled());
    }
    return false;
  }
  
  public RegionManager getRegionManager(World world)
  {
    return this.wgp.getRegionManager(world);
  }
  
  public LocalPlayer wrapPlayer(Player player)
  {
    return this.wgp.wrapPlayer(player);
  }
  
  public ProtectedCuboidRegion getPBregion(Location loc, int length, int height, int width, String playerName)
  {
    BlockVector min = new BlockVector(loc.getBlockX() - (length - 1) / 2, 0, loc.getBlockZ() - (width - 1) / 2);
    
    BlockVector max = new BlockVector(loc.getBlockX() + (length - 1) / 2, 255, loc.getBlockZ() + (width - 1) / 2);
    if (height != 0)
    {
      min = min.setY(loc.getBlockY() - (height - 1) / 2).toBlockVector();
      max = max.setY(loc.getBlockY() + (height - 1) / 2).toBlockVector();
    }
    ProtectedCuboidRegion region = new ProtectedCuboidRegion("ps" + loc.getBlockX() + "x" + loc.getBlockY() + "y" + loc.getBlockZ() + "z", min, max);
    
    DefaultDomain dd = new DefaultDomain();
    dd.addPlayer(playerName);
    region.setOwners(dd);
    return region;
  }
  
  public ProtectedRegion getRealApplicableRegion(Location loc)
  {
    ProtectedRegion region = null;
    for (ProtectedRegion pr : this.wgp.getRegionManager(loc.getWorld()).getApplicableRegions(loc)) {
      if ((region == null) || (region.getPriority() < pr.getPriority())) {
        region = pr;
      }
    }
    return region;
  }
  
  public boolean addMembers(World world, String regionName, String[] playerNames)
  {
    RegionManager rm = this.wgp.getRegionManager(world);
    if (rm == null) {
      return false;
    }
    ProtectedRegion region = rm.getRegion(regionName);
    if (region == null) {
      return false;
    }
    DefaultDomain members = region.getMembers();
    for (String playerName : playerNames) {
      members.addPlayer(playerName);
    }
    region.setMembers(members);
    try
    {
      rm.save();
    }
    catch (StorageException ex)
    {
      this.plugin.getLogger().log(Level.SEVERE, "Unexpected failure saving WorldGuard configuration: {0}", ex.toString());
      return false;
    }
    return true;
  }
  
  public boolean removeMembers(World world, String regionName, String[] playerNames)
  {
    RegionManager rm = this.wgp.getRegionManager(world);
    if (rm == null) {
      return false;
    }
    ProtectedRegion region = rm.getRegion(regionName);
    if (region == null) {
      return false;
    }
    DefaultDomain members = region.getMembers();
    for (String playerName : playerNames) {
      members.removePlayer(playerName);
    }
    region.setMembers(members);
    try
    {
      rm.save();
    }
    catch (StorageException ex)
    {
      this.plugin.getLogger().log(Level.SEVERE, "Unexpected failure saving WorldGuard configuration: {0}", ex.toString());
      return false;
    }
    return true;
  }
  
  public boolean removeProtection(World world, ProtectedRegion pr)
  {
    RegionManager rm = this.wgp.getRegionManager(world);
    rm.removeRegion(pr.getId());
    try
    {
      rm.save();
    }
    catch (StorageException ex)
    {
      this.plugin.getLogger().log(Level.SEVERE, "Unexpected failure saving WorldGuard configuration: {0}", ex.toString());
      return false;
    }
    return true;
  }
}
