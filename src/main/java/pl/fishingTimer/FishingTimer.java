package pl.fishingTimer;

import org.bukkit.plugin.java.JavaPlugin;

public class FishingTimer extends JavaPlugin {
    
    private static FishingTimer instance;
    private FishingListener fishingListener;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Rejestracja event listenera
        fishingListener = new FishingListener(this);
        getServer().getPluginManager().registerEvents(fishingListener, this);
        
        // Rejestracja komend
        getCommand("stoptimer").setExecutor(new TimerCommand());
        
        getLogger().info("FishingTimer włączony! Wersja: " + getDescription().getVersion());
    }
    
    @Override
    public void onDisable() {
        if (fishingListener != null) {
            fishingListener.cleanup();
        }
        getLogger().info("FishingTimer wyłączony!");
    }
    
    public static FishingTimer getInstance() {
        return instance;
    }
    
    public FishingListener getFishingListener() {
        return fishingListener;
    }
}
