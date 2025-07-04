package pl.fishingTimer;

import org.bukkit.entity.Entity;
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
        PlayerFishEvent.State state = event.getState();
        
        if (state == PlayerFishEvent.State.FISHING) {
            // Rozpoczęcie wędkowania
            Entity entity = event.getEntity();
            if (entity instanceof FishHook) {
                FishHook hook = (FishHook) entity;
                startTimer(player, hook);
            }
        } else {
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
        
        // Sprawdź czy hook jest prawidłowy
        if (hook == null || !hook.isValid()) {
            return;
        }
        
        // Stwórz dane timera
        TimerData timerData = new TimerData(hook);
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
                    // Wyświetl czas w action bar lub chat
                    double seconds = timeLeft / 10.0;
                    String timeDisplay = String.format("§e⏰ Czas: %.1fs", seconds);
                    
                    // Próbuj użyć action bar, jeśli nie ma to zwykły chat
                    try {
                        // Spróbuj wysłać action bar (może nie działać w 1.8)
                        player.sendMessage(timeDisplay);
                    } catch (Exception e) {
                        // Fallback do zwykłej wiadomości
                        player.sendMessage(timeDisplay);
                    }
                    
                    timeLeft--;
                } else {
                    // Timer skończony - symuluj złapanie ryby
                    simulateFishCatch(player);
                    stopTimer(player);
                }
            }
        }.runTaskTimer(plugin, 0L, 2L); // Co 2 ticki (0.1s)
        
        timerData.setTask(task);
    }
    
    private void simulateFishCatch(Player player) {
        // Komunikat
        player.sendMessage("§a✓ Ryba złapana! Wyciągnij wędkę!");
        
        // Prosty efekt dźwiękowy - używamy podstawowego dźwięku
        try {
            player.getWorld().playSound(player.getLocation(), "random.pop", 1.0f, 1.2f);
        } catch (Exception e) {
            // Jeśli nie ma dźwięku, to trudno
        }
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
        private BukkitTask task;
        
        public TimerData(FishHook hook) {
            this.hook = hook;
        }
        
        public void setTask(BukkitTask task) {
            this.task = task;
        }
        
        public void cleanup() {
            if (task != null) {
                task.cancel();
            }
        }
    }
}
