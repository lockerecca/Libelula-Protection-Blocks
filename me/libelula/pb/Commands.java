package me.libelula.pb;

import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class Commands
  implements CommandExecutor
{
  private final LibelulaProtectionBlocks plugin;
  
  public Commands(LibelulaProtectionBlocks plugin)
  {
    this.plugin = plugin;
  }
  
  public boolean onCommand(CommandSender cs, Command cmnd, String string, String[] args)
  {
    Player player = getPlayer(cs);
    if (args.length == 0)
    {
      switch (cmnd.getName())
      {
      case "ps": 
        if ((player == null) || (player.hasPermission("pb.version"))) {
          cs.sendMessage(ChatColor.YELLOW + this.plugin.getPluginVersion());
        }
        showCommnandsHelp(cs);
        break;
      default: 
        cs.sendMessage(this.plugin.i18n.getText("unknown_command"));
      }
    }
    else if (cmnd.getName().equals("ps"))
    {
      ItemMeta dtMeta;
      switch (args[0].toLowerCase())
      {
      case "help": 
        showCommnandsHelp(cs);
        break;
      case "version": 
        if (args.length != 1)
        {
          cs.sendMessage(ChatColor.RED + this.plugin.i18n.getText("incorrect_parameters"));
          return true;
        }
        if ((player == null) || (player.hasPermission("pb.version"))) {
          cs.sendMessage(ChatColor.YELLOW + this.plugin.getPluginVersion());
        } else {
          cs.sendMessage(ChatColor.RED + this.plugin.i18n.getText("you_dont_have_permissions"));
        }
        break;
      case "create": 
        if (player == null)
        {
          cs.sendMessage(ChatColor.RED + this.plugin.i18n.getText("in_game"));
          return true;
        }
        if (!player.hasPermission("pb.create"))
        {
          cs.sendMessage(ChatColor.RED + this.plugin.i18n.getText("you_dont_have_permissions"));
          return true;
        }
        if ((args.length != 2) && (args.length != 4))
        {
          cs.sendMessage(ChatColor.RED + this.plugin.i18n.getText("incorrect_parameters"));
          cs.sendMessage(ChatColor.YELLOW + this.plugin.i18n.getText("ps_create_command"));
          cs.sendMessage(ChatColor.YELLOW + this.plugin.i18n.getText("ps_create_command2"));
          cs.sendMessage(ChatColor.YELLOW + this.plugin.i18n.getText("ps_create_command3"));
          return true;
        }
        try
        {
          int length = Integer.parseInt(args[1]);
          int width;
          if (args.length == 4)
          {
            int height = Integer.parseInt(args[2]);
            width = Integer.parseInt(args[3]);
          }
          else
          {
            int height = length;
            width = length;
          }
        }
        catch (NumberFormatException ex)
        {
          int width;
          cs.sendMessage(ChatColor.RED + this.plugin.i18n.getText("incorrect_parameters"));
          cs.sendMessage(ChatColor.YELLOW + this.plugin.i18n.getText("ps_create_command"));
          cs.sendMessage(ChatColor.YELLOW + this.plugin.i18n.getText("ps_create_command2"));
          cs.sendMessage(ChatColor.YELLOW + this.plugin.i18n.getText("ps_create_command3"));
          return true;
        }
        int width;
        int height;
        int length;
        if ((length <= 0) || (width <= 0))
        {
          cs.sendMessage(ChatColor.RED + this.plugin.i18n.getText("values_must_greater"));
          cs.sendMessage(ChatColor.YELLOW + this.plugin.i18n.getText("ps_create_command"));
          cs.sendMessage(ChatColor.YELLOW + this.plugin.i18n.getText("ps_create_command2"));
          cs.sendMessage(ChatColor.YELLOW + this.plugin.i18n.getText("ps_create_command3"));
          return true;
        }
        if (((length & 0x1) == 0) || ((width & 0x1) == 0) || ((height != 0) && ((height & 0x1) == 0)))
        {
          cs.sendMessage(ChatColor.RED + this.plugin.i18n.getText("values_must_be_odd"));
          cs.sendMessage(ChatColor.YELLOW + this.plugin.i18n.getText("ps_create_command"));
          cs.sendMessage(ChatColor.YELLOW + this.plugin.i18n.getText("ps_create_command2"));
          cs.sendMessage(ChatColor.YELLOW + this.plugin.i18n.getText("ps_create_command3"));
          return true;
        }
        if (this.plugin.pc.createPBFromItemsInHand(player, length, height, width))
        {
          cs.sendMessage(ChatColor.GREEN + this.plugin.i18n.getText("pbs_has_been_created"));
          return true;
        }
        break;
      case "hide": 
        if (player == null)
        {
          cs.sendMessage(ChatColor.RED + this.plugin.i18n.getText("in_game"));
          return true;
        }
        if (args.length == 1)
        {
          this.plugin.pbs.hide(player);
        }
        else
        {
          cs.sendMessage(ChatColor.RED + this.plugin.i18n.getText("incorrect_parameters"));
          cs.sendMessage(ChatColor.YELLOW + this.plugin.i18n.getText("ps_hide_command"));
        }
        break;
      case "unhide": 
        if (player == null)
        {
          cs.sendMessage(ChatColor.RED + this.plugin.i18n.getText("in_game"));
          return true;
        }
        if (args.length == 1) {
          this.plugin.pbs.unhide(player, false);
        } else if ((args.length == 2) && (args[1].toLowerCase().equalsIgnoreCase("force")))
        {
          if (player.hasPermission("pb.unhide.force")) {
            this.plugin.pbs.unhide(player, true);
          } else {
            cs.sendMessage(ChatColor.RED + this.plugin.i18n.getText("you_dont_have_permissions"));
          }
        }
        else if (player.hasPermission("pb.unhide.force")) {
          cs.sendMessage(ChatColor.YELLOW + this.plugin.i18n.getText("ps_unhide_force_command"));
        } else {
          cs.sendMessage(ChatColor.YELLOW + this.plugin.i18n.getText("ps_unhide_command"));
        }
        break;
      case "add": 
        if (player == null)
        {
          cs.sendMessage(ChatColor.RED + this.plugin.i18n.getText("in_game"));
          return true;
        }
        if (args.length == 1)
        {
          cs.sendMessage(ChatColor.RED + this.plugin.i18n.getText("incorrect_parameters"));
          cs.sendMessage(ChatColor.YELLOW + this.plugin.i18n.getText("ps_add_command"));
          return true;
        }
        ProtectionBlocks.PSBlocks psb = this.plugin.pbs.addMember(player, 
          (String[])Arrays.copyOfRange(args, 1, args.length));
        if (psb != null) {
          showMemberList(cs, psb);
        }
        break;
      case "del": 
      case "delete": 
      case "remove": 
        if (player == null)
        {
          cs.sendMessage(ChatColor.RED + this.plugin.i18n.getText("in_game"));
          return true;
        }
        if (args.length == 1)
        {
          cs.sendMessage(ChatColor.RED + this.plugin.i18n.getText("incorrect_parameters"));
          cs.sendMessage(ChatColor.YELLOW + this.plugin.i18n.getText("ps_del_command"));
          return true;
        }
        ProtectionBlocks.PSBlocks psb = this.plugin.pbs.removeMember(player, 
          (String[])Arrays.copyOfRange(args, 1, args.length));
        if (psb != null) {
          showMemberList(cs, psb);
        }
        break;
      case "flag": 
        if ((args.length == 1) || ((args.length == 2) && (args[1].equalsIgnoreCase("list")))) {
          cs.sendMessage(ChatColor.YELLOW + this.plugin.i18n.getText("available_flags_are") + " " + ChatColor.YELLOW + this.plugin.config
            .getPlayerConfigurableFlags().toString());
        } else {
          cs.sendMessage(ChatColor.RED + this.plugin.i18n.getText("flag_deprecated"));
        }
        break;
      case "info": 
        if (player == null)
        {
          cs.sendMessage(ChatColor.RED + this.plugin.i18n.getText("in_game"));
          return true;
        }
        if (args.length != 1)
        {
          cs.sendMessage(ChatColor.RED + this.plugin.i18n.getText("incorrect_parameters"));
          cs.sendMessage(ChatColor.YELLOW + this.plugin.i18n.getText("ps_info_command"));
          return true;
        }
        ProtectionBlocks.PSBlocks psb = this.plugin.pbs.getPs(player.getLocation());
        if (psb == null)
        {
          player.sendMessage(ChatColor.RED + this.plugin.i18n.getText("not_in_ps_area"));
          return true;
        }
        if ((!psb.region.isMember(player.getName())) && (!player.hasPermission("pb.addmember.others")))
        {
          player.sendMessage(ChatColor.RED + this.plugin.i18n.getText("not_owned_by_you"));
          return true;
        }
        cs.sendMessage(ChatColor.YELLOW + psb.name + " - " + psb.region.getId());
        cs.sendMessage(ChatColor.YELLOW + this.plugin.i18n.getText("priority") + ": " + psb.region.getPriority());
        showMemberList(cs, psb);
        cs.sendMessage(ChatColor.YELLOW + "" + ChatColor.BOLD + "Flags:");
        for (Map.Entry<Flag<?>, Object> flag : psb.region.getFlags().entrySet()) {
          cs.sendMessage(ChatColor.YELLOW + "  * " + ((Flag)flag.getKey()).getName() + ": " + flag.getValue().toString());
        }
        break;
      case "remove-all-ps": 
        if (args.length != 2)
        {
          cs.sendMessage(ChatColor.RED + this.plugin.i18n.getText("incorrect_parameters"));
          cs.sendMessage(ChatColor.YELLOW + this.plugin.i18n.getText("ps_removeall_command"));
          return true;
        }
        if ((player != null) && (!player.hasPermission("pb.remove.all")))
        {
          cs.sendMessage(ChatColor.RED + this.plugin.i18n.getText("you_dont_have_permissions"));
          return true;
        }
        this.plugin.pbs.removeAllPB(args[1]);
        break;
      case "reload": 
        if (args.length != 1)
        {
          cs.sendMessage(ChatColor.RED + this.plugin.i18n.getText("incorrect_parameters"));
          cs.sendMessage(ChatColor.YELLOW + this.plugin.i18n.getText("ps_reload_command"));
          return true;
        }
        if ((player != null) && (!player.hasPermission("pb.reload")))
        {
          cs.sendMessage(ChatColor.RED + this.plugin.i18n.getText("you_dont_have_permissions"));
          return true;
        }
        this.plugin.config.reload();
        cs.sendMessage(ChatColor.YELLOW + this.plugin.i18n.getText("config_reloaded"));
        break;
      case "+fence": 
        if (player == null)
        {
          cs.sendMessage(ChatColor.RED + this.plugin.i18n.getText("in_game"));
          return true;
        }
        if ((player != null) && (!player.hasPermission("pb.modifyflags")))
        {
          cs.sendMessage(ChatColor.RED + this.plugin.i18n.getText("you_dont_have_permissions"));
          return true;
        }
        if (args.length != 1)
        {
          cs.sendMessage(ChatColor.RED + this.plugin.i18n.getText("incorrect_parameters"));
          return true;
        }
        if ((player.getItemInHand().getItemMeta().getLore() != null) && 
          (player.getItemInHand().getItemMeta().getLore().size() == 3) && 
          (player.getItemInHand().getItemMeta().getDisplayName() != null))
        {
          cs.sendMessage(ChatColor.GREEN + this.plugin.i18n.getText("fence_added"));
          List<String> lore = player.getItemInHand().getItemMeta().getLore();
          lore.set(0, "+Fence");
          dtMeta = player.getItemInHand().getItemMeta();
          dtMeta.setLore(lore);
          player.getItemInHand().setItemMeta(dtMeta);
        }
        else
        {
          cs.sendMessage(ChatColor.RED + this.plugin.i18n.getText("not_a_protection_block"));
          return true;
        }
        break;
      default: 
        if (this.plugin.config.isPlayerFlag(args[0]))
        {
          if (player == null)
          {
            cs.sendMessage(ChatColor.RED + this.plugin.i18n.getText("in_game"));
            return true;
          }
          String value = "";
          if (args.length > 1)
          {
            for (String part : (String[])Arrays.copyOfRange(args, 1, args.length)) {
              value = value.concat(part) + " ";
            }
            value = value.substring(0, value.length() - 1);
          }
          else
          {
            value = null;
          }
          this.plugin.pbs.setFlag(player, DefaultFlag.fuzzyMatchFlag(args[0].toLowerCase()), value);
        }
        else
        {
          cs.sendMessage(ChatColor.RED + this.plugin.i18n.getText("incorrect_parameters"));
        }
        break;
      }
    }
    return true;
  }
  
  private Player getPlayer(CommandSender cs)
  {
    Player player = null;
    if ((cs instanceof Player)) {
      player = (Player)cs;
    }
    return player;
  }
  
  private void showCommnandsHelp(CommandSender cs)
  {
    Player player = getPlayer(cs);
    
    cs.sendMessage(ChatColor.YELLOW + this.plugin.i18n.getText("use_ps_help_command"));
    cs.sendMessage(ChatColor.YELLOW + this.plugin.i18n.getText("list_only_allowed_commands"));
    cs.sendMessage(ChatColor.YELLOW + this.plugin.i18n.getText("ps_add_command"));
    cs.sendMessage(ChatColor.YELLOW + this.plugin.i18n.getText("ps_del_command"));
    cs.sendMessage(ChatColor.YELLOW + this.plugin.i18n.getText("ps_hide_command"));
    if ((player == null) || (player.hasPermission("pb.unhide.force"))) {
      cs.sendMessage(ChatColor.YELLOW + this.plugin.i18n.getText("ps_unhide_force_command"));
    } else {
      cs.sendMessage(ChatColor.YELLOW + this.plugin.i18n.getText("ps_unhide_command"));
    }
    cs.sendMessage(ChatColor.YELLOW + this.plugin.i18n.getText("ps_flag_list_command"));
    cs.sendMessage(ChatColor.YELLOW + this.plugin.i18n.getText("ps_flag_command"));
    cs.sendMessage(ChatColor.YELLOW + this.plugin.i18n.getText("ps_info_command"));
    if ((player == null) || (player.hasPermission("pb.reload"))) {
      cs.sendMessage(ChatColor.YELLOW + this.plugin.i18n.getText("ps_reload_command"));
    }
    if ((player == null) || (player.hasPermission("pb.create")))
    {
      cs.sendMessage(ChatColor.YELLOW + this.plugin.i18n.getText("ps_create_command"));
      cs.sendMessage(ChatColor.YELLOW + "- " + this.plugin.i18n.getText("ps_create_command2"));
      cs.sendMessage(ChatColor.YELLOW + "- " + this.plugin.i18n.getText("ps_create_command3"));
    }
    if ((player == null) || (player.hasPermission("pb.version"))) {
      cs.sendMessage(ChatColor.YELLOW + this.plugin.i18n.getText("ps_version_command"));
    }
    if ((player == null) || (player.hasPermission("pb.remove.all"))) {
      cs.sendMessage(ChatColor.YELLOW + this.plugin.i18n.getText("ps_removeall_command"));
    }
  }
  
  private void showMemberList(CommandSender cs, ProtectionBlocks.PSBlocks psb)
  {
    cs.sendMessage(ChatColor.YELLOW + this.plugin.i18n.getText("member_list_title"));
    cs.sendMessage(ChatColor.YELLOW + "" + ChatColor.BOLD + this.plugin.i18n.getText("Owners") + ":");
    for (String owner : psb.region.getOwners().getPlayers()) {
      cs.sendMessage(ChatColor.YELLOW + "  * " + owner);
    }
    cs.sendMessage(ChatColor.YELLOW + "" + ChatColor.BOLD + this.plugin.i18n.getText("Members") + ":");
    for (String member : psb.region.getMembers().getPlayers()) {
      cs.sendMessage(ChatColor.YELLOW + "  * " + member);
    }
  }
}
