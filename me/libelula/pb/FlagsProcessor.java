package me.libelula.pb;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;

public class FlagsProcessor
  extends BukkitRunnable
{
  private final LibelulaProtectionBlocks plugin;
  private final Location location;
  
  private class BlockVectorComparator
    implements Comparator<BlockVector>
  {
    private BlockVectorComparator() {}
    
    public int compare(BlockVector o1, BlockVector o2)
    {
      int resp = o1.getBlockX() - o2.getBlockX();
      if (resp == 0)
      {
        resp = o1.getBlockY() - o2.getBlockY();
        if (resp == 0) {
          resp = o1.getBlockZ() - o2.getBlockZ();
        }
      }
      return resp;
    }
  }
  
  public FlagsProcessor(LibelulaProtectionBlocks plugin, Location location)
  {
    this.location = location;
    this.plugin = plugin;
  }
  
  public void run()
  {
    ProtectionBlocks.PSBlocks psb = this.plugin.pbs.get(this.location);
    if (psb == null) {
      return;
    }
    String[] flags = ((String)psb.lore.get(0)).split("\\+");
    if (flags.length == 0) {
      return;
    }
    for (String flag : flags) {
      if (flag.equals("Fence"))
      {
        TreeSet<BlockVector> blockVectors = new TreeSet(new BlockVectorComparator(null));
        
        BlockVector bv = psb.region.getMinimumPoint().setY(psb.location.getBlockY()).toBlockVector();
        bv.setY(psb.location.getBlockY());
        for (int x = psb.region.getMinimumPoint().getBlockX(); x <= psb.region.getMaximumPoint().getBlockX(); x++) {
          blockVectors.add(new BlockVector(bv.setX(x)));
        }
        for (int z = psb.region.getMinimumPoint().getBlockZ(); z <= psb.region.getMaximumPoint().getBlockZ(); z++) {
          blockVectors.add(new BlockVector(bv.setZ(z)));
        }
        bv = psb.region.getMaximumPoint().setY(psb.location.getBlockY()).toBlockVector();
        for (int x = psb.region.getMaximumPoint().getBlockX(); x >= psb.region.getMinimumPoint().getBlockX(); x--) {
          blockVectors.add(new BlockVector(bv.setX(x)));
        }
        for (int z = psb.region.getMaximumPoint().getBlockZ(); z >= psb.region.getMinimumPoint().getBlockZ(); z--) {
          blockVectors.add(new BlockVector(bv.setZ(z)));
        }
        TaskManager.putFence(this.plugin, psb.location.getWorld(), blockVectors);
        psb.lore.set(0, "Libelula Protection Blocks");
        try
        {
          this.plugin.sql.updatePSBlockInfo(psb);
        }
        catch (SQLException ex)
        {
          this.plugin.getLogger().severe(ex.toString());
        }
      }
    }
  }
  
  public static void putFence(LibelulaProtectionBlocks plugin, World world, TreeSet<BlockVector> blockVectors)
  {
    for (BlockVector bv : blockVectors)
    {
      Location loc = new Location(world, bv.getBlockX(), bv.getBlockY(), bv.getBlockZ());
      if (loc.getBlock().getType() == Material.AIR) {
        loc.getBlock().setType(Material.FENCE);
      }
    }
  }
}
