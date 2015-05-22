package me.libelula.pb;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class Importer
{
  public static void importFromPS(LibelulaProtectionBlocks plugin)
  {
    File psConfigFile = new File("plugins/ProtectionStones/config.yml");
    String ignoredWorld;
    if (psConfigFile.exists())
    {
      FileConfiguration psConfig = YamlConfiguration.loadConfiguration(psConfigFile);
      for (Iterator localIterator1 = psConfig.getStringList("Blocks").iterator(); localIterator1.hasNext();)
      {
        blockLine = (String)localIterator1.next();
        try
        {
          Material material = Material.getMaterial(blockLine.split(" ")[0]);
          int size = Integer.parseInt(blockLine.split(" ")[1]);
          plugin.pbs.addOldPsBlock(material, size);
        }
        catch (Exception ex)
        {
          plugin.getLogger().log(Level.WARNING, "Error importing old PS Block list: {0}", ex.toString());
        }
      }
      Object flags = new HashMap();
      for (String blockLine = psConfig.getStringList("Flags").iterator(); blockLine.hasNext();)
      {
        flagLine = (String)blockLine.next();
        try
        {
          String flagname = flagLine.split(" ")[0];
          String value = flagLine.substring(flagLine.indexOf(" ") + 1);
          ((HashMap)flags).put(flagname, value);
        }
        catch (Exception ex)
        {
          plugin.getLogger().log(Level.WARNING, "Error importing old PS Flag list: {0}", ex.toString());
        }
      }
      plugin.config.setFlags((HashMap)flags);
      
      blockLine = psConfig.getString("Exclusion.WORLDS").split(" ");flagLine = blockLine.length;
      for (ex = 0; ex < flagLine; ex++)
      {
        ignoredWorld = blockLine[ex];
        try
        {
          World world = plugin.getServer().getWorld(ignoredWorld);
          if (world != null) {
            plugin.config.addIgnoredWorld(world);
          }
        }
        catch (Exception ex)
        {
          plugin.getLogger().log(Level.WARNING, "Error importing old PS World list: {0}", ex.toString());
        }
      }
      plugin.config.setOldPsUseFullYaxis(psConfig.getBoolean("Region.SKYBEDROCK"));
      plugin.config.setOldPsAutoHide(psConfig.getBoolean("Region.AUTOHIDE"));
      plugin.config.setOldPsNoDrop(psConfig.getBoolean("Region.NODROP"));
    }
    String rex = ProtectionBlocks.regionIdRegexp;
    Pattern p = Pattern.compile("-?\\d+");
    int inc = 0;
    for (String flagLine = plugin.getServer().getWorlds().iterator(); flagLine.hasNext();)
    {
      world = (World)flagLine.next();
      for (Map.Entry<String, ProtectedRegion> regionSet : plugin.wgm.getRegions(world).entrySet()) {
        if (((String)regionSet.getKey()).matches(rex))
        {
          Matcher m = p.matcher((CharSequence)regionSet.getKey());
          m.find();
          int x = Integer.parseInt(m.group());
          m.find();
          int y = Integer.parseInt(m.group());
          m.find();
          int z = Integer.parseInt(m.group());
          
          int regionSize = ((ProtectedRegion)regionSet.getValue()).getMaximumPoint().getBlockX() - ((ProtectedRegion)regionSet.getValue()).getMinimumPoint().getBlockX();
          
          Material material = Material.SPONGE;
          Byte materialData = Byte.valueOf((byte)0);
          for (Map.Entry<Material, Integer> e : plugin.pbs.getoldPSs().entrySet()) {
            if (((Integer)e.getValue()).intValue() * 2 + 1 == regionSize) {
              material = (Material)e.getKey();
            }
          }
          Location location = new Location(world, x, y, z);
          Boolean hidden = Boolean.valueOf(true);
          if (plugin.pbs.oldPScontainsBlock(location.getBlock().getType()))
          {
            hidden = Boolean.valueOf(false);
            material = location.getBlock().getType();
          }
          regionSize++;
          String name = plugin.i18n.getText("protection") + ": " + regionSize + " x " + regionSize + " x " + regionSize + " (" + plugin.i18n.getText("blocks") + ")";
          List<String> lore = new ArrayList();
          lore.add("Old protection stone");
          lore.add("Imported from old plugin");
          lore.add(ProtectionController.getFullHashFromValues(regionSize, regionSize, regionSize, material.getId(), inc));
          int secondsFromEpoch = 0;
          if (((ProtectedRegion)regionSet.getValue()).getOwners().size() != 0)
          {
            String playerName = (String)((ProtectedRegion)regionSet.getValue()).getOwners().getPlayers().iterator().next();
            OfflinePlayer player = plugin.getServer().getOfflinePlayer(playerName);
            if (player != null) {
              secondsFromEpoch = (int)(player.getLastPlayed() / 1000L);
            }
          }
          if (secondsFromEpoch == 0) {
            secondsFromEpoch = (int)(new Date().getTime() / 1000L);
          }
          plugin.pbs.addProtectionBlock(location, (ProtectedRegion)regionSet.getValue(), material, hidden.booleanValue(), name, lore, materialData, secondsFromEpoch);
          inc++;
        }
      }
    }
    World world;
    plugin.getLogger().log(Level.INFO, "{0} Protection Stones has been imported.", Integer.valueOf(plugin.pbs.size()));
    if (plugin.pbs.size() != 0)
    {
      plugin.config.setOldPsImported(true);
      plugin.config.persist();
    }
  }
}
