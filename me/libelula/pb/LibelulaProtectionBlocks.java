package me.libelula.pb;

import java.util.List;
import java.util.TreeSet;
import java.util.logging.Logger;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;

public class LibelulaProtectionBlocks
  extends JavaPlugin
{
  public Configuration config;
  public Internationalization i18n;
  public WorldGuardManager wgm;
  public ProtectionBlocks pbs;
  public ProtectionController pc;
  public SQLiteManager sql;
  public Economy eco;
  public TreeSet<String> bannedAdvicedPlayers;
  private Commands cs;
  
  public void onEnable()
  {
    Plugin wgPlugin = getServer().getPluginManager().getPlugin("WorldGuard");
    if (wgPlugin == null)
    {
      ConsoleCommandSender cs = getServer().getConsoleSender();
      cs.sendMessage(ChatColor.RED + "CRITICAL: Plugin WorldGuard not found!");
      for (Player player : getServer().getOnlinePlayers()) {
        if (player.isOp()) {
          player.sendMessage(ChatColor.RED + "CRITICAL: Plugin WorldGuard not found! Disabling Libelula Protection Blocks");
        }
      }
      disablePlugin();
      return;
    }
    this.config = new Configuration(this);
    this.i18n = new Internationalization(this.config.getLanguage());
    this.wgm = new WorldGuardManager(this);
    this.sql = new SQLiteManager(this);
    if (!this.sql.isInitialized())
    {
      getLogger().severe(this.i18n.getText("need_db_support"));
      disablePlugin();
      return;
    }
    this.pbs = new ProtectionBlocks(this);
    
    Plugin oldPS = getServer().getPluginManager().getPlugin("ProtectionStones");
    boolean oldPluginConfigImported = false;
    if (oldPS != null)
    {
      TaskManager.disablePSAndLoadCommands(oldPS, this);
      if (!this.config.isOldPsImported())
      {
        getLogger().info(this.i18n.getText("importing_oldps"));
        TaskManager.importFromPSWhenWGIsEnabled(this);
        oldPluginConfigImported = true;
      }
    }
    else
    {
      TaskManager.registerCommands(this);
    }
    getServer().getPluginManager().registerEvents(new Listener(this), this);
    if (!oldPluginConfigImported) {
      this.pbs.load();
    }
    setupEconomy();
    this.pc = new ProtectionController(this);
    this.bannedAdvicedPlayers = new TreeSet();
    this.cs = new Commands(this);
    getCommand("ps").setExecutor(this.cs);
  }
  
  public void disablePlugin()
  {
    if (this.i18n != null) {
      getLogger().info(this.i18n.getText("disabling_plugin"));
    }
    getServer().getPluginManager().disablePlugin(this);
  }
  
  public String getPluginVersion()
  {
    return getDescription().getFullName() + " " + this.i18n.getText("created_by") + " " + (String)getDescription().getAuthors().get(0);
  }
  
  private boolean setupEconomy()
  {
    this.eco = null;
    if (getServer().getPluginManager().getPlugin("Vault") == null) {
      return false;
    }
    RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
    if (rsp == null)
    {
      getLogger().info(this.i18n.getText("vault_not_found"));
      return false;
    }
    this.eco = ((Economy)rsp.getProvider());
    return this.eco != null;
  }
}
