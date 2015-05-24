package me.libelula.pb;

import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class Configuration
{
  private final LibelulaProtectionBlocks plugin;
  private FileConfiguration fc;
  private List<World> ignoredWorldList;
  private TreeSet<String> playerFlags;
  private final Lock _ignoredWorldList_mutex;
  
  public Configuration(LibelulaProtectionBlocks plugin)
  {
    this.plugin = plugin;
    plugin.saveDefaultConfig();
    this.ignoredWorldList = new ArrayList();
    this.playerFlags = new TreeSet();
    this._ignoredWorldList_mutex = new ReentrantLock(true);
    reload();
  }
  
  public boolean isPlayerFlag(String flagName)
  {
    return this.playerFlags.contains(flagName);
  }
  
  public TreeSet<String> getPlayerConfigurableFlags()
  {
    return this.playerFlags;
  }
  
  public void persist()
  {
    List<String> worldNames = new ArrayList();
    this._ignoredWorldList_mutex.lock();
    try
    {
      for (it = this.ignoredWorldList.iterator(); it.hasNext();)
      {
        World world = (World)it.next();
        worldNames.add(world.getName());
      }
    }
    finally
    {
      Iterator<World> it;
      this._ignoredWorldList_mutex.unlock();
    }
    this.fc.set("ignored.worlds", worldNames);
    this.plugin.saveConfig();
  }
  
  public boolean isOldPsImported()
  {
    return this.fc.getBoolean("ps-backward-compatibility.imported");
  }
  
  public void setOldPsImported(boolean state)
  {
    this.fc.set("ps-backward-compatibility.imported", Boolean.valueOf(state));
  }
  
  public int getOldPsMode()
  {
    switch (this.fc.getString("ps-backward-compatibility.mode"))
    {
    case "old": 
      return 1;
    case "new": 
      return 0;
    }
    this.plugin.getLogger().log(Level.WARNING, "Invalid configuration for ps-backward-compatibility.mode: {0}", this.fc
      .getString("ps-backward-compatibility.mode"));
    setOldPsMode(0);
    return 0;
  }
  
  public void setOldPsMode(int mode)
  {
    switch (mode)
    {
    case 1: 
      this.fc.set("ps-backward-compatibility.mode", "old");
      break;
    case 0: 
      this.fc.set("ps-backward-compatibility.mode", "new");
    }
  }
  
  public void setFlags(HashMap<String, String> flags)
  {
    if (flags != null) {
      for (Map.Entry<String, String> flag : flags.entrySet()) {
        this.fc.set("ps-default.flags.".concat((String)flag.getKey()), flag.getValue());
      }
    }
  }
  
  public HashMap<String, String> getStringFlags()
  {
    HashMap<String, String> flags = new HashMap();
    for (String key : this.fc.getConfigurationSection("ps-default.flags").getKeys(false)) {
      flags.put(key, this.fc.getString("ps-default.flags.".concat(key)));
    }
    return flags;
  }
  
  public Map<Flag<?>, Object> getFlags(String player)
  {
    Map<Flag<?>, Object> flags = new TreeMap(new WorldGuardManager.FlagComparator());
    Flag<?> df;
    for (df : DefaultFlag.flagsList) {
      for (String key : this.fc.getConfigurationSection("ps-default.flags").getKeys(false)) {
        if (df.getName().toString().equalsIgnoreCase(key)) {
          if (this.fc.getString("ps-default.flags.".concat(key)).equalsIgnoreCase("deny")) {
            flags.put(df, StateFlag.State.DENY);
          } else if (this.fc.getString("ps-default.flags.".concat(key)).equalsIgnoreCase("allow")) {
            flags.put(df, StateFlag.State.ALLOW);
            } else if (this.fc.getString("ps-default.flags.".concat(key)).equalsIgnoreCase("member")) {
            flags.put(df, StateFlag.State.MEMBERS);
          } else if (this.fc.getString("ps-default.flags.".concat(key)).equalsIgnoreCase("nonmember")) {
            flags.put(df, StateFlag.State.NON_MEMBERS);
          } else {
            flags.put(df, this.fc.getString("ps-default.flags.".concat(key)).replace("%player%", player));
          }
        }
      }
    }
    return flags;
  }
  
  public void setOldPsUseFullYaxis(boolean value)
  {
    this.fc.set("ps-backward-compatibility.full-y-axis", Boolean.valueOf(value));
  }
  
  public boolean getOldPsUseFullYaxis()
  {
    return this.fc.getBoolean("ps-backward-compatibility.full-y-axis");
  }
  
  public void setOldPsAutoHide(boolean value)
  {
    this.fc.set("ps-backward-compatibility.auto-hide", Boolean.valueOf(value));
  }
  
  public boolean getOldPsAutoHide()
  {
    return this.fc.getBoolean("ps-backward-compatibility.auto-hide");
  }
  
  public void setOldPsNoDrop(boolean value)
  {
    this.fc.set("ps-backward-compatibility.no-drops", Boolean.valueOf(value));
  }
  
  public boolean getOldPsNoDrop()
  {
    return this.fc.getBoolean("ps-backward-compatibility.no-drops");
  }
  
  /* Error */
  public void addIgnoredWorld(World world)
  {
    // Byte code:
    //   0: aload_0
    //   1: getfield 12	me/libelula/pb/Configuration:_ignoredWorldList_mutex	Ljava/util/concurrent/locks/Lock;
    //   4: invokeinterface 15 1 0
    //   9: aload_0
    //   10: getfield 6	me/libelula/pb/Configuration:ignoredWorldList	Ljava/util/List;
    //   13: aload_1
    //   14: invokeinterface 21 2 0
    //   19: pop
    //   20: aload_0
    //   21: getfield 12	me/libelula/pb/Configuration:_ignoredWorldList_mutex	Ljava/util/concurrent/locks/Lock;
    //   24: invokeinterface 22 1 0
    //   29: goto +15 -> 44
    //   32: astore_2
    //   33: aload_0
    //   34: getfield 12	me/libelula/pb/Configuration:_ignoredWorldList_mutex	Ljava/util/concurrent/locks/Lock;
    //   37: invokeinterface 22 1 0
    //   42: aload_2
    //   43: athrow
    //   44: return
    // Line number table:
    //   Java source line #176	-> byte code offset #0
    //   Java source line #178	-> byte code offset #9
    //   Java source line #180	-> byte code offset #20
    //   Java source line #181	-> byte code offset #29
    //   Java source line #180	-> byte code offset #32
    //   Java source line #182	-> byte code offset #44
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	45	0	this	Configuration
    //   0	45	1	world	World
    //   32	11	2	localObject	Object
    // Exception table:
    //   from	to	target	type
    //   9	20	32	finally
  }
  
  public boolean ignoredWorldContains(World world)
  {
    return this.ignoredWorldList.contains(world);
  }
  
  public boolean setLenguage(String langName)
  {
    if (langName == null) {
      return false;
    }
    if (langName.length() == 2)
    {
      langName = langName.toLowerCase();
      switch (langName)
      {
      case "en": 
      case "es": 
      case "it": 
      case "pt": 
        this.fc.set("language", langName);
        return true;
      }
      return false;
    }
    if (langName.length() == 4)
    {
      if (langName.equalsIgnoreCase("enUS"))
      {
        this.fc.set("language", "enUS");
        return true;
      }
      if (langName.equalsIgnoreCase("esES"))
      {
        this.fc.set("language", "esES");
        return true;
      }
      if (langName.equalsIgnoreCase("esMX"))
      {
        this.fc.set("language", "esMX");
        return true;
      }
      if (langName.equalsIgnoreCase("itIT"))
      {
        this.fc.set("language", "itIT");
        return true;
      }
      if (langName.equalsIgnoreCase("ptBR"))
      {
        this.fc.set("language", "ptBR");
        return true;
      }
    }
    return false;
  }
  
  public FileConfiguration getLanguage()
  {
    File langFile;
    File langFile;
    File langFile;
    File langFile;
    File langFile;
    File langFile;
    switch (this.fc.getString("language"))
    {
    case "en": 
    case "enUS": 
      langFile = new File(this.plugin.getDataFolder(), "enUS.yml");
      break;
    case "es": 
    case "esES": 
      langFile = new File(this.plugin.getDataFolder(), "esES.yml");
      break;
    case "esMX": 
      langFile = new File(this.plugin.getDataFolder(), "esMX.yml");
      break;
    case "it": 
    case "itIT": 
      langFile = new File(this.plugin.getDataFolder(), "itIT.yml");
      break;
    case "pt": 
    case "ptBR": 
      langFile = new File(this.plugin.getDataFolder(), "ptBR.yml");
      break;
    default: 
      langFile = null;
    }
    if (langFile == null) {
      return null;
    }
    if (langFile.exists()) {
      langFile.delete();
    }
    this.plugin.saveResource(langFile.getName(), false);
    return YamlConfiguration.loadConfiguration(langFile);
  }
  
  public final void reload()
  {
    this.plugin.reloadConfig();
    this.fc = this.plugin.getConfig();
    this.ignoredWorldList.clear();
    for (String worldName : this.fc.getStringList("ignored.worlds"))
    {
      World world = this.plugin.getServer().getWorld(worldName);
      if (world != null) {
        addIgnoredWorld(world);
      } else {
        this.plugin.getLogger().warning("Invalid configured world name in ignored worlds: ".concat(worldName));
      }
    }
    for (String flagName : this.fc.getStringList("player.configurable-flags"))
    {
      flagName = flagName.toLowerCase();
      if (!DefaultFlag.fuzzyMatchFlag(flagName).getName().equals(flagName)) {
        this.plugin.getLogger().warning("Invalid configured player configurable-flags name: ".concat(flagName));
      } else {
        this.playerFlags.add(flagName);
      }
    }
    if (this.plugin.i18n != null) {
      this.plugin.i18n.setLang(getLanguage());
    }
  }
}
