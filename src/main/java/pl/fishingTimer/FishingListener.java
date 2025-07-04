package pl.fishingTimer;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

public class FishingListener implements Listener {
    
    private final FishingTimer plugin;
    private final Map<Player, TimerData> activeTimers = new HashMap<>();
    
    public FishingListener(FishingTimer plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        
        if (event.getState() == PlayerFishEvent.State.FISHING) {
            // Rozpoczęcie wędkowania
            startTimer(player, event.getHook());
        } else if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH || 
                   event.getState() == PlayerFishEvent.State.REEL_IN) {
            // Zakończenie wędkowania
            stopTimer(player);
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        stopTimer(event.getPlayer());
    }
    
    private void startTimer(Player player, FishHook hook) {
        // Zatrzymaj poprzedni timer jeśli istnieje
        stopTimer(player);
        
        // Stwórz niewidzialny armor stand nad spławikiem
        Location hookLoc = hook.getLocation().add(0, 1.5, 0);
        ArmorStand armorStand = hookLoc.getWorld().spawn(hookLoc, ArmorStand.class);
        
        // Konfiguracja armor stand
        armorStand.setVisible(false);
        armorStand.setGravity(false);
        armorStand.setCanPickupItems(false);
        armorStand.setCustomNameVisible(true);
        armorStand.setMarker(true);
        armorStand.setSmall(true);
        
        // Stwórz dane timera
        TimerData timerData = new TimerData(hook, armorStand);
        activeTimers.put(player, timerData);
        
        // Rozpocznij timer
        BukkitTask task = new BukkitRunnable() {
            private int timeLeft = 150; // 15 sekund * 10 (0.1s precision)
            
            @Override
            public void run() {
                if (!player.isOnline() || !hook.isValid()) {
                    stopTimer(player);
                    return;
                }
                
                if (timeLeft > 0) {
                    // Aktualizuj pozycję armor stand
                    Location newLoc = hook.getLocation().add(0, 1.5, 0);
                    armorStand.teleport(newLoc);
                    
                    // Oblicz i wyświetl czas
                    double seconds = timeLeft / 10.0;
                    String timeDisplay = String.format("§e⏰ %.1fs", seconds);
                    armorStand.setCustomName(timeDisplay);
                    
                    timeLeft--;
                } else {
                    // Timer skończony - symuluj złapanie ryby
                    simulateFishCatch(player, hook);
                    stopTimer(player);
                }
            }
        }.runTaskTimer(plugin, 0L, 2L); // Co 2 ticki (0.1s)
        
        timerData.setTask(task);
    }
    
    private void simulateFishCatch(Player player, FishHook hook) {
        // Efekty dźwiękowe
        player.playSound(player.getLocation(), "entity.player.splash", 1.0f, 1.2f);
        
        // Komunikat
        player.sendMessage("§a✓ Ryba złapana! Wyciągnij wędkę!");
        player.sendTitle("§a🐟 Ryba złapana!", "§fWyciągnij wędkę!", 10, 60, 20);
        
        // Dodatkowy efekt po chwili
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.playSound(player.getLocation(), "entity.item.pickup", 0.7f, 0.8f);
                }
            }
        }.runTaskLater(plugin, 10L);
    }
    
    public void stopTimer(Player player) {
        TimerData timerData = activeTimers.remove(player);
        if (timerData != null) {
            timerData.cleanup();
        }
    }
    
    public void cleanup() {
        for (TimerData timerData : activeTimers.values()) {
            timerData.cleanup();
        }
        activeTimers.clear();
    }
    
    // Klasa do przechowywania danych timera
    private static class TimerData {
        private final FishHook hook;
        private final ArmorStand armorStand;
        private BukkitTask task;
        
        public TimerData(FishHook hook, ArmorStand armorStand) {
            this.hook = hook;
            this.armorStand = armorStand;
        }
        
        public void setTask(BukkitTask task) {
            this.task = task;
        }
        
        public void cleanup() {
            if (task != null) {
                task.cancel();
            }
            if (armorStand != null && armorStand.isValid()) {
                armorStand.remove();
            }
        }
    }
}
