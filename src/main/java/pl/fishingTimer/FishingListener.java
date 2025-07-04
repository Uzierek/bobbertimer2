package pl.fishingTimer;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
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
        
        plugin.getLogger().info("Player " + player.getName() + " fishing state: " + state);
        
        if (state == PlayerFishEvent.State.FISHING) {
            // Rozpoczęcie wędkowania - używamy getEntity() dla Bukkit 1.8
            Entity entity = event.getEntity();
            if (entity instanceof FishHook) {
                FishHook hook = (FishHook) entity;
                startTimer(player, hook);
                plugin.getLogger().info("Started timer for " + player.getName());
            }
        } else {
            // Zakończenie wędkowania - wszystkie inne stany kończą timer
            stopTimer(player);
            plugin.getLogger().info("Stopped timer for " + player.getName());
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
            plugin.getLogger().warning("Invalid hook for player " + player.getName());
            return;
        }
        
        // Stwórz niewidzialny armor stand nad spławikiem
        Location hookLoc = hook.getLocation();
        if (hookLoc == null || hookLoc.getWorld() == null) {
            plugin.getLogger().warning("Invalid hook location for player " + player.getName());
            return;
        }
        
        Location armorStandLoc = hookLoc.clone().add(0, 1.5, 0);
        ArmorStand armorStand;
        
        try {
            armorStand = armorStandLoc.getWorld().spawn(armorStandLoc, ArmorStand.class);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to spawn armor stand: " + e.getMessage());
            return;
        }
        
        // Konfiguracja armor stand - sprawdzenie każdej metody
        try {
            armorStand.setVisible(false);
            armorStand.setGravity(false);
            armorStand.setCanPickupItems(false);
            armorStand.setCustomNameVisible(true);
            
            // Te metody mogą nie istnieć w starszych wersjach
            try {
                armorStand.setMarker(true);
            } catch (Exception e) {
                plugin.getLogger().info("setMarker() not available in this version");
            }
            
            try {
                armorStand.setSmall(true);
            } catch (Exception e) {
                plugin.getLogger().info("setSmall() not available in this version");
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to configure armor stand: " + e.getMessage());
            armorStand.remove();
            return;
        }
        
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
                    try {
                        Location newLoc = hook.getLocation();
                        if (newLoc != null) {
                            newLoc.add(0, 1.5, 0);
                            armorStand.teleport(newLoc);
                            
                            // Oblicz i wyświetl czas
                            double seconds = timeLeft / 10.0;
                            String timeDisplay = String.format("§e⏰ %.1fs", seconds);
                            armorStand.setCustomName(timeDisplay);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error updating timer display: " + e.getMessage());
                    }
                    
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
        // Efekty dźwiękowe - używamy enum Sound dla kompatybilności
        try {
            // Najpierw próbujemy nowszą nazwę
            player.playSound(player.getLocation(), Sound.valueOf("ENTITY_PLAYER_SPLASH"), 1.0f, 1.2f);
        } catch (Exception e) {
            try {
                // Następnie starszą nazwę
                player.playSound(player.getLocation(), Sound.valueOf("SPLASH"), 1.0f, 1.2f);
            } catch (Exception e2) {
                try {
                    // Jeszcze starszą
                    player.playSound(player.getLocation(), Sound.valueOf("WATER"), 1.0f, 1.2f);
                } catch (Exception e3) {
                    plugin.getLogger().info("No compatible splash sound found");
                }
            }
        }
        
        // Komunikat
        player.sendMessage("§a✓ Ryba złapana! Wyciągnij wędkę!");
        
        // Sprawdź czy sendTitle jest dostępne
        try {
            // Użyj refleksji do sprawdzenia czy metoda istnieje
            player.getClass().getMethod("sendTitle", String.class, String.class, int.class, int.class, int.class);
            player.sendTitle("§a🐟 Ryba złapana!", "§fWyciągnij wędkę!", 10, 60, 20);
        } catch (Exception e) {
            // sendTitle nie jest dostępne
            player.sendMessage("§a🐟 Ryba złapana!");
        }
        
        // Dodatkowy efekt po chwili
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    try {
                        player.playSound(player.getLocation(), Sound.valueOf("ENTITY_ITEM_PICKUP"), 0.7f, 0.8f);
                    } catch (Exception e) {
                        try {
                            player.playSound(player.getLocation(), Sound.valueOf("ITEM_PICKUP"), 0.7f, 0.8f);
                        } catch (Exception e2) {
                            try {
                                player.playSound(player.getLocation(), Sound.valueOf("ORB_PICKUP"), 0.7f, 0.8f);
                            } catch (Exception e3) {
                                // Brak kompatybilnego dźwięku
                            }
                        }
                    }
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
