package pl.fishingTimer;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TimerCommand implements CommandExecutor {
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cTa komenda może być używana tylko przez graczy!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (command.getName().equalsIgnoreCase("stoptimer")) {
            FishingTimer.getInstance().getFishingListener().stopTimer(player);
            player.sendMessage("§cTimer wędkowania zatrzymany.");
            return true;
        }
        
        return false;
    }
}
