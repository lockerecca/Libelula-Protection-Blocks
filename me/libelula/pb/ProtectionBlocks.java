package me.libelula.pb;

import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.managers.storage.StorageException;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

public class ProtectionBlocks
{
  private final LibelulaProtectionBlocks plugin;
  private TreeMap<Material, Integer> compacPSDB;
  public static String regionIdRegexp = "^ps(-?[0-9]+)x(-?[0-9]+)y(-?[0-9]+)z";
  private TreeMap<Location, PSBlocks> protectionBlockMap;
  private TreeMap<Location, Integer> dropCancellationSet;
  private final Lock _protectionBlock_mutex;
  private final Lock _dropCancellationSet_mutex;
  
  private class RegionManagerComparator
    implements Comparator<RegionManager>
  {
    private RegionManagerComparator() {}
    
    public int compare(RegionManager o1, RegionManager o2)
    {
      return o1.hashCode() - o2.hashCode();
    }
  }
  
  private class LocationComparator
    implements Comparator<Location>
  {
    private LocationComparator() {}
    
    public int compare(Location o1, Location o2)
    {
      int resp = o1.getWorld().getUID().compareTo(o2.getWorld().getUID());
      if (resp == 0)
      {
        resp = o1.getBlockX() - o2.getBlockX();
        if (resp == 0)
        {
          resp = o1.getBlockY() - o2.getBlockY();
          if (resp == 0) {
            resp = o1.getBlockZ() - o2.getBlockZ();
          }
        }
      }
      return resp;
    }
  }
  
  public class PSBlocks
    implements Comparable<PSBlocks>
  {
    Location location;
    ProtectedRegion region;
    Material material;
    boolean hidden;
    String name;
    List<String> lore;
    Byte materialData;
    int secondsFromEpoch;
    
    public PSBlocks() {}
    
    public String toString()
    {
      String resp;
      if (this.location != null) {
        resp = this.location.getWorld().getName() + " " + this.location.getBlockX() + " " + this.location.getBlockY() + " " + this.location.getBlockZ() + " ";
      } else {
        resp = "<NULL Location>";
      }
      if (this.material != null) {
        resp = resp + this.material.name() + " ";
      } else {
        resp = resp + "<NULL Material>";
      }
      String resp = resp + this.hidden;
      
      return resp;
    }
    
    public int compareTo(PSBlocks o)
    {
      return new ProtectionBlocks.LocationComparator(ProtectionBlocks.this, null).compare(o.location, this.location);
    }
  }
  
  public ProtectionBlocks(LibelulaProtectionBlocks plugin)
  {
    this.protectionBlockMap = new TreeMap(new LocationComparator(null));
    this.dropCancellationSet = new TreeMap(new LocationComparator(null));
    this._protectionBlock_mutex = new ReentrantLock(true);
    this._dropCancellationSet_mutex = new ReentrantLock(true);
    this.compacPSDB = new TreeMap();
    this.plugin = plugin;
  }
  
  /* Error */
  public void addDropEventCancellation(Location loc, int quantity)
  {
    // Byte code:
    //   0: aload_0
    //   1: getfield 11	me/libelula/pb/ProtectionBlocks:_dropCancellationSet_mutex	Ljava/util/concurrent/locks/Lock;
    //   4: invokeinterface 15 1 0
    //   9: aload_0
    //   10: getfield 7	me/libelula/pb/ProtectionBlocks:dropCancellationSet	Ljava/util/TreeMap;
    //   13: aload_1
    //   14: iload_2
    //   15: invokestatic 16	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   18: invokevirtual 17	java/util/TreeMap:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   21: pop
    //   22: aload_0
    //   23: getfield 11	me/libelula/pb/ProtectionBlocks:_dropCancellationSet_mutex	Ljava/util/concurrent/locks/Lock;
    //   26: invokeinterface 18 1 0
    //   31: goto +15 -> 46
    //   34: astore_3
    //   35: aload_0
    //   36: getfield 11	me/libelula/pb/ProtectionBlocks:_dropCancellationSet_mutex	Ljava/util/concurrent/locks/Lock;
    //   39: invokeinterface 18 1 0
    //   44: aload_3
    //   45: athrow
    //   46: return
    // Line number table:
    //   Java source line #144	-> byte code offset #0
    //   Java source line #146	-> byte code offset #9
    //   Java source line #148	-> byte code offset #22
    //   Java source line #149	-> byte code offset #31
    //   Java source line #148	-> byte code offset #34
    //   Java source line #150	-> byte code offset #46
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	47	0	this	ProtectionBlocks
    //   0	47	1	loc	Location
    //   0	47	2	quantity	int
    //   34	11	3	localObject	Object
    // Exception table:
    //   from	to	target	type
    //   9	22	34	finally
  }
  
  public boolean removeDropEventCancellation(Location loc)
  {
    this._dropCancellationSet_mutex.lock();
    boolean ret;
    try
    {
      ret = this.dropCancellationSet.containsKey(loc);
      if (ret)
      {
        int remains = ((Integer)this.dropCancellationSet.remove(loc)).intValue();
        if (remains > 0) {
          this.dropCancellationSet.put(loc, Integer.valueOf(remains));
        }
      }
    }
    finally
    {
      this._dropCancellationSet_mutex.unlock();
    }
    return ret;
  }
  
  public void addProtectionBlock(Location location, ProtectedRegion region, Material material, boolean hidden, String name, List<String> lore, byte materialData, int secondsFromEpoch, boolean insert)
  {
    PSBlocks psb = new PSBlocks();
    psb.location = location;
    psb.region = region;
    psb.material = material;
    psb.hidden = hidden;
    psb.name = name;
    psb.lore = lore;
    psb.materialData = Byte.valueOf(materialData);
    psb.secondsFromEpoch = secondsFromEpoch;
    this._protectionBlock_mutex.lock();
    try
    {
      this.protectionBlockMap.put(location, psb);
    }
    finally
    {
      this._protectionBlock_mutex.unlock();
    }
    if (insert) {
      TaskManager.addPSBlock(psb, this.plugin);
    }
  }
  
  public boolean removeProtectionBlock(Location location)
  {
    PSBlocks pbs = (PSBlocks)this.protectionBlockMap.get(location);
    this._protectionBlock_mutex.lock();
    boolean resp;
    try
    {
      boolean resp;
      if (this.protectionBlockMap.remove(location) != null)
      {
        TaskManager.removeProtectionBlock(location, this.plugin);
        this.plugin.wgm.removeProtection(location.getWorld(), pbs.region);
        resp = true;
      }
      else
      {
        resp = false;
      }
    }
    finally
    {
      this._protectionBlock_mutex.unlock();
    }
    return resp;
  }
  
  public boolean removeProtectionBlock(Location location, Player player)
  {
    PSBlocks pbs = (PSBlocks)this.protectionBlockMap.get(location);
    boolean resp = removeProtectionBlock(location);
    if (resp) {
      addDropEventCancellation(location, location.getBlock().getDrops().size());
    }
    ItemStack protectionBlock = this.plugin.pc.getItemStack(pbs);
    this.plugin.pc.addAvailableId((String)protectionBlock.getItemMeta().getLore().get(2));
    if (!player.getInventory().addItem(new ItemStack[] { protectionBlock }).isEmpty()) {
      location.getWorld().dropItem(location, protectionBlock);
    }
    return resp;
  }
  
  /* Error */
  public void addProtectionBlock(PSBlocks psb)
  {
    // Byte code:
    //   0: aload_0
    //   1: getfield 10	me/libelula/pb/ProtectionBlocks:_protectionBlock_mutex	Ljava/util/concurrent/locks/Lock;
    //   4: invokeinterface 15 1 0
    //   9: aload_0
    //   10: getfield 6	me/libelula/pb/ProtectionBlocks:protectionBlockMap	Ljava/util/TreeMap;
    //   13: aload_1
    //   14: getfield 25	me/libelula/pb/ProtectionBlocks$PSBlocks:location	Lorg/bukkit/Location;
    //   17: aload_1
    //   18: invokevirtual 17	java/util/TreeMap:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   21: pop
    //   22: aload_0
    //   23: getfield 10	me/libelula/pb/ProtectionBlocks:_protectionBlock_mutex	Ljava/util/concurrent/locks/Lock;
    //   26: invokeinterface 18 1 0
    //   31: goto +15 -> 46
    //   34: astore_2
    //   35: aload_0
    //   36: getfield 10	me/libelula/pb/ProtectionBlocks:_protectionBlock_mutex	Ljava/util/concurrent/locks/Lock;
    //   39: invokeinterface 18 1 0
    //   44: aload_2
    //   45: athrow
    //   46: aload_1
    //   47: aload_0
    //   48: getfield 14	me/libelula/pb/ProtectionBlocks:plugin	Lme/libelula/pb/LibelulaProtectionBlocks;
    //   51: invokestatic 34	me/libelula/pb/TaskManager:addPSBlock	(Lme/libelula/pb/ProtectionBlocks$PSBlocks;Lme/libelula/pb/LibelulaProtectionBlocks;)V
    //   54: return
    // Line number table:
    //   Java source line #232	-> byte code offset #0
    //   Java source line #234	-> byte code offset #9
    //   Java source line #236	-> byte code offset #22
    //   Java source line #237	-> byte code offset #31
    //   Java source line #236	-> byte code offset #34
    //   Java source line #238	-> byte code offset #46
    //   Java source line #239	-> byte code offset #54
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	55	0	this	ProtectionBlocks
    //   0	55	1	psb	PSBlocks
    //   34	11	2	localObject	Object
    // Exception table:
    //   from	to	target	type
    //   9	22	34	finally
  }
  
  public void addProtectionBlock(Location location, ProtectedRegion region, Material material, boolean hidden, String name, List<String> lore, Byte materialData, int secondsFromEpoch)
  {
    addProtectionBlock(location, region, material, hidden, name, lore, materialData.byteValue(), secondsFromEpoch, true);
  }
  
  public void load()
  {
    TaskManager.loadProtectionBlocks(this.plugin);
  }
  
  public boolean contains(Location location)
  {
    return this.protectionBlockMap.containsKey(location);
  }
  
  public int size()
  {
    return this.protectionBlockMap.size();
  }
  
  public PSBlocks get(Location location)
  {
    return (PSBlocks)this.protectionBlockMap.get(location);
  }
  
  public boolean matches(Block block)
  {
    PSBlocks psb = get(block.getLocation());
    if (psb == null) {
      return false;
    }
    if (block.getType() != psb.material)
    {
      switch (block.getType())
      {
      case REDSTONE_LAMP_ON: 
        if (psb.material == Material.REDSTONE_LAMP_OFF) {
          return true;
        }
      case GLOWING_REDSTONE_ORE: 
        if (psb.material == Material.REDSTONE_ORE) {
          return true;
        }
        break;
      }
    }
    else
    {
      switch (block.getType())
      {
      case PUMPKIN: 
      case JACK_O_LANTERN: 
        return true;
      }
      if (psb.materialData.byteValue() == block.getData()) {
        return true;
      }
    }
    return false;
  }
  
  public void addOldPsBlock(Material material, int size)
  {
    this.compacPSDB.put(material, Integer.valueOf(size));
  }
  
  public TreeMap<Material, Integer> getoldPSs()
  {
    return this.compacPSDB;
  }
  
  public boolean oldPScontainsBlock(Material material)
  {
    return this.compacPSDB.containsKey(material);
  }
  
  public int oldPSgetSizeFor(Material material)
  {
    return ((Integer)this.compacPSDB.get(material)).intValue();
  }
  
  public void oldPSPlace(BlockPlaceEvent e) {}
  
  public void newBlock(ProtectedCuboidRegion regionToProtect, Location location, Player player, ItemMeta dtMeta, Material material, Byte materialData)
  {
    RegionManager rm = this.plugin.wgm.getRegionManager(location.getWorld());
    if (rm.overlapsUnownedRegion(regionToProtect, this.plugin.wgm.wrapPlayer(player)))
    {
      returnBlock(location, player, dtMeta);
      player.sendMessage(ChatColor.RED + this.plugin.i18n.getText("overlaps"));
      return;
    }
    if (this.plugin.config.ignoredWorldContains(location.getWorld()))
    {
      returnBlock(location, player, dtMeta);
      player.sendMessage(ChatColor.RED + this.plugin.i18n.getText("ignored_world"));
      return;
    }
    if (this.protectionBlockMap.containsKey(location))
    {
      returnBlock(location, player, dtMeta);
      player.sendMessage(ChatColor.RED + this.plugin.i18n.getText("protection_over"));
      return;
    }
    int calculatedPriority = -1;
    for (ProtectedRegion pr : rm.getApplicableRegions(regionToProtect)) {
      if (pr.getPriority() > calculatedPriority) {
        calculatedPriority = pr.getPriority();
      }
    }
    regionToProtect.setPriority(calculatedPriority + 1);
    
    rm.addRegion(regionToProtect);
    player.sendMessage(ChatColor.GREEN + this.plugin.i18n.getText("pb_activated"));
    
    addProtectionBlock(location, regionToProtect, material, false, dtMeta
      .getDisplayName(), dtMeta.getLore(), materialData, 
      (int)new Date().getTime() / 1000);
    try
    {
      rm.save();
    }
    catch (StorageException ex)
    {
      Logger.getLogger(ProtectionBlocks.class.getName()).log(Level.SEVERE, null, ex);
    }
    if (player.getGameMode() == GameMode.CREATIVE) {
      player.setItemInHand(new ItemStack(Material.AIR));
    }
    this.plugin.pc.removeAvailableId((String)dtMeta.getLore().get(2));
    new FlagsProcessor(this.plugin, location).runTaskAsynchronously(this.plugin);
  }
  
  private void returnBlock(Location location, Player player, ItemMeta dtMeta)
  {
    ItemStack is = new ItemStack(location.getBlock().getType(), 1, (short)location.getBlock().getData());
    is.setItemMeta(dtMeta);
    if (player.getItemInHand().getType() == Material.AIR) {
      player.setItemInHand(is);
    } else if (player.getGameMode() != GameMode.CREATIVE) {
      location.getWorld().dropItem(location, is);
    }
    location.getBlock().setType(Material.AIR);
  }
  
  public PSBlocks getPs(Location loc)
  {
    PSBlocks psb = null;
    ProtectedRegion region = this.plugin.wgm.getRealApplicableRegion(loc);
    if ((region != null) && (region.getId().matches(regionIdRegexp)))
    {
      Pattern p = Pattern.compile("-?\\d+");
      Matcher m = p.matcher(region.getId());
      m.find();
      int x = Integer.parseInt(m.group());
      m.find();
      int y = Integer.parseInt(m.group());
      m.find();
      int z = Integer.parseInt(m.group());
      Location psLoc = new Location(loc.getWorld(), x, y, z);
      if (this.protectionBlockMap.containsKey(psLoc)) {
        psb = (PSBlocks)this.protectionBlockMap.get(psLoc);
      }
    }
    return psb;
  }
  
  public void setFlag(Player player, Flag flag, String value)
  {
    PSBlocks psb = getPs(player.getLocation());
    if (psb == null)
    {
      player.sendMessage(ChatColor.RED + this.plugin.i18n.getText("not_in_ps_area"));
      return;
    }
    if (!validatePlayerPermission(player, psb)) {
      return;
    }
    if (value == null)
    {
      psb.region.setFlag(flag, null);
      return;
    }
    switch (value.toLowerCase())
    {
    case "allow": 
      psb.region.setFlag(flag, StateFlag.State.ALLOW);
      break;
    case "deny": 
      psb.region.setFlag(flag, StateFlag.State.DENY);
      break;
    default: 
      switch (flag.getName().toLowerCase())
      {
      case "farewell": 
      case "greeting": 
        psb.region.setFlag(flag, value);
      }
      return;
    }
  }
  
  private boolean validatePlayerPermission(Player player, PSBlocks psb)
  {
    if ((psb.region.isOwner(player.getName())) || (player.isOp())) {
      return true;
    }
    player.sendMessage(ChatColor.RED + this.plugin.i18n.getText("you_dont_have_permissions"));
    return false;
  }
  
  public void hide(Player player)
  {
    PSBlocks psb = getPs(player.getLocation());
    if (psb == null)
    {
      player.sendMessage(ChatColor.RED + this.plugin.i18n.getText("not_in_ps_area"));
      return;
    }
    if (psb.hidden)
    {
      player.sendMessage(ChatColor.RED + this.plugin.i18n.getText("already_hidden"));
      return;
    }
    if (!validatePlayerPermission(player, psb)) {
      return;
    }
    psb.location.getBlock().setType(Material.AIR);
    psb.hidden = true;
    TaskManager.updatePSBlocks(psb, this.plugin);
  }
  
  public void unhide(Player player, boolean force)
  {
    PSBlocks psb = getPs(player.getLocation());
    if (psb == null)
    {
      player.sendMessage(ChatColor.RED + this.plugin.i18n.getText("not_in_ps_area"));
      return;
    }
    if ((!psb.hidden) && (!force))
    {
      player.sendMessage(ChatColor.RED + this.plugin.i18n.getText("not_hidden"));
      if (player.hasPermission("pb.unhide.force")) {
        player.sendMessage(ChatColor.YELLOW + this.plugin.i18n.getText("not_hidden_force"));
      }
      return;
    }
    if (!validatePlayerPermission(player, psb)) {
      return;
    }
    psb.location.getBlock().setType(psb.material);
    if (psb.hidden)
    {
      psb.hidden = false;
      TaskManager.updatePSBlocks(psb, this.plugin);
    }
  }
  
  public PSBlocks addMember(Player player, String[] members)
  {
    PSBlocks psb = getPs(player.getLocation());
    if (psb == null)
    {
      player.sendMessage(ChatColor.RED + this.plugin.i18n.getText("not_in_ps_area"));
      return null;
    }
    if ((!psb.region.isOwner(player.getName())) && (!player.hasPermission("pb.addmember.others")))
    {
      player.sendMessage(ChatColor.RED + this.plugin.i18n.getText("not_owned_by_you"));
      return null;
    }
    this.plugin.wgm.addMembers(player.getWorld(), psb.region.getId(), members);
    return psb;
  }
  
  public PSBlocks removeMember(Player player, String[] members)
  {
    PSBlocks psb = getPs(player.getLocation());
    if (psb == null)
    {
      player.sendMessage(ChatColor.RED + this.plugin.i18n.getText("not_in_ps_area"));
      return null;
    }
    if ((!psb.region.isOwner(player.getName())) && (!player.hasPermission("pb.removemember.others")))
    {
      player.sendMessage(ChatColor.RED + this.plugin.i18n.getText("not_owned_by_you"));
      return null;
    }
    this.plugin.wgm.removeMembers(player.getWorld(), psb.region.getId(), members);
    return psb;
  }
  
  public PSBlocks[] getOwnedPSList(String owner)
  {
    TreeSet<PSBlocks> psbResult = new TreeSet();
    this._protectionBlock_mutex.lock();
    try
    {
      for (Map.Entry<Location, PSBlocks> psb : this.protectionBlockMap.entrySet()) {
        if (((PSBlocks)psb.getValue()).region.isOwner(owner.toLowerCase())) {
          psbResult.add(psb.getValue());
        }
      }
    }
    finally
    {
      this._protectionBlock_mutex.unlock();
    }
    return (PSBlocks[])psbResult.toArray(new PSBlocks[0]);
  }
  
  public void removeAllPB(String playerName)
  {
    PSBlocks[] psbs = getOwnedPSList(playerName);
    TreeSet<RegionManager> rmsToSave = new TreeSet(new RegionManagerComparator(null));
    
    int qtty = psbs.length;
    for (PSBlocks psb : psbs)
    {
      RegionManager rm = this.plugin.wgm.getRegionManager(psb.location.getWorld());
      rm.removeRegion(psb.region.getId());
      TaskManager.removeProtectionBlock(psb.location, this.plugin);
      rmsToSave.add(rm);
      this._protectionBlock_mutex.lock();
      try
      {
        this.protectionBlockMap.remove(psb.location);
      }
      finally
      {
        this._protectionBlock_mutex.unlock();
      }
    }
    for (??? = rmsToSave.iterator(); ((Iterator)???).hasNext();)
    {
      RegionManager rm = (RegionManager)((Iterator)???).next();
      try
      {
        rm.save();
      }
      catch (Exception ex)
      {
        this.plugin.getLogger().severe(ex.toString());
      }
    }
    if (qtty > 0) {
      this.plugin.getLogger().log(Level.INFO, "Erased: {0} protection blocks owned by {1}", new Object[] { Integer.valueOf(qtty), playerName });
    } else {
      this.plugin.getLogger().log(Level.INFO, "{0} has no protection blocks.", playerName);
    }
  }
}
