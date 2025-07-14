package com.lucyplugin;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;

public class MyPlugin extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getLogger().info("=== JointMaker 3.0 STARTING (PERSISTENT DATA SYSTEM) ===");
        getLogger().info("JointMaker plugin has been enabled!");
        getLogger().info("Registering events...");
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Events registered successfully!");
        
        // Convert all existing rotten flesh to joints for online players
        getLogger().info("Checking for online players to convert items...");
        for (Player player : getServer().getOnlinePlayers()) {
            getLogger().info("Found online player: " + player.getName());
            convertRottenFleshToJoints(player);
        }
        
        // Start a repeating task to continuously convert rotten flesh
        getLogger().info("Starting repeating conversion task...");
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : getServer().getOnlinePlayers()) {
                    convertRottenFleshToJoints(player);
                }
            }
        }.runTaskTimer(this, 20L, 20L); // Run every second (20 ticks)
        
        // Start a repeating task for continuous smoke particles while high
        getLogger().info("Starting continuous smoke particle task...");
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : getServer().getOnlinePlayers()) {
                    // Check if player has any of the high effects
                    if (player.hasPotionEffect(PotionEffectType.HUNGER) || 
                        player.hasPotionEffect(PotionEffectType.SLOW_FALLING) ||
                        player.hasPotionEffect(PotionEffectType.GLOWING) ||
                        player.hasPotionEffect(PotionEffectType.SLOW)) {
                        
                        // Spawn subtle smoke particles around the player
                        player.getWorld().spawnParticle(Particle.SMOKE_NORMAL, 
                                                       player.getLocation().add(0, 1.8, 0), // At head level
                                                       3, // Fewer particles for continuous effect
                                                       0.2, 0.2, 0.2, // Smaller spread
                                                       0.01); // Slower speed
                    }
                }
            }
        }.runTaskTimer(this, 20L, 10L); // Run every 0.5 seconds (10 ticks)
        
        getLogger().info("=== JointMaker 3.0 STARTUP COMPLETE - READY TO CONVERT! ===");
    }

    @Override
    public void onDisable() {
        getLogger().info("JointMaker 3.0 has been disabled!");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        getLogger().info("Player interact: " + player.getName() + " with item: " + 
                        (item != null ? item.getType() + " (" + (isJoint(item) ? "JOINT" : "NOT JOINT") + ")" : "null"));
        
        // First convert any rotten flesh to joint
        if (item != null && item.getType() == Material.ROTTEN_FLESH && !isJoint(item)) {
            getLogger().info("Converting rotten flesh in hand to joint");
            ItemStack joint = createJoint();
            joint.setAmount(item.getAmount());
            player.getInventory().setItemInMainHand(joint);
            item = joint; // Update the reference
            player.sendMessage(ChatColor.YELLOW + "Converted rotten flesh to joint!");
        }
        
        // Then check if it's a joint to smoke
        if (isJoint(item)) {
            getLogger().info("Player is smoking a joint!");
            event.setCancelled(true);
            smokeJoint(player, item);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.ROTTEN_FLESH && !isJoint(event.getCurrentItem())) {
            // Convert rotten flesh to joint when clicked
            ItemStack joint = createJoint();
            joint.setAmount(event.getCurrentItem().getAmount());
            event.setCurrentItem(joint);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Convert rotten flesh when player joins
        Player player = event.getPlayer();
        new BukkitRunnable() {
            @Override
            public void run() {
                convertRottenFleshToJoints(player);
            }
        }.runTaskLater(this, 1L); // Run 1 tick after join
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            new BukkitRunnable() {
                @Override
                public void run() {
                    convertRottenFleshToJoints(player);
                }
            }.runTaskLater(this, 1L);
        }
    }

    private void convertRottenFleshToJoints(Player player) {
        getLogger().info("Converting rotten flesh for player: " + player.getName());
        int convertedCount = 0;
        
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null) {
                getLogger().info("Slot " + i + ": " + item.getType() + " x" + item.getAmount());
                
                if (item.getType() == Material.ROTTEN_FLESH) {
                    if (isJoint(item)) {
                        getLogger().info("  -> Already a joint, skipping");
                    } else {
                        getLogger().info("  -> Converting rotten flesh to joint");
                        ItemStack joint = createJoint();
                        joint.setAmount(item.getAmount());
                        player.getInventory().setItem(i, joint);
                        convertedCount++;
                    }
                }
            }
        }
        
        getLogger().info("Converted " + convertedCount + " items for " + player.getName());
        if (convertedCount > 0) {
            player.sendMessage(ChatColor.GREEN + "Converted " + convertedCount + " rotten flesh to joints!");
        }
    }

    private ItemStack createJoint() {
        ItemStack joint = new ItemStack(Material.ROTTEN_FLESH);
        ItemMeta meta = joint.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + "Joint");
            meta.setLore(Arrays.asList(
                ChatColor.GRAY + "A rolled cannabis cigarette",
                ChatColor.YELLOW + "Right-click to smoke",
                ChatColor.RED + "Warning: May cause drowsiness"
            ));
            
            // Add a custom tag to identify this as a joint
            meta.getPersistentDataContainer().set(
                new NamespacedKey(this, "joint"), 
                PersistentDataType.BYTE, 
                (byte) 1
            );
            
            joint.setItemMeta(meta);
        }
        
        return joint;
    }

    private boolean isJoint(ItemStack item) {
        if (item == null) {
            return false;
        }
        
        if (item.getType() != Material.ROTTEN_FLESH) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            getLogger().info("Item has no meta data");
            return false;
        }
        
        // Check for the joint tag in persistent data
        NamespacedKey key = new NamespacedKey(this, "joint");
        boolean hasJointTag = meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
        
        getLogger().info("Checking if joint - Has joint tag: " + hasJointTag);
        
        return hasJointTag;
    }

    private void smokeJoint(Player player, ItemStack joint) {
        // Remove one joint from inventory
        if (joint.getAmount() > 1) {
            joint.setAmount(joint.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }
        
        // Apply effects
        player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 600, 1)); // Hunger for 30 seconds
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 600, 0)); // Slow falling for 30 seconds
        player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 600, 0)); // Glowing for 30 seconds
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 600, 0)); // Slowness for 30 seconds
        
        // Send message
        player.sendMessage(ChatColor.GREEN + "You smoke the joint... " + ChatColor.GRAY + "*cough*");
        player.sendMessage(ChatColor.DARK_GRAY + "Smoke will follow you while you're high...");
        
        // Play fire charge sound effect
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 0.5f, 1.5f);
        
        // Spawn initial smoke burst around the player's head
        player.getWorld().spawnParticle(Particle.SMOKE_NORMAL, 
                                       player.getLocation().add(0, 1.8, 0), // At head level
                                       20, // Reduced from 30 since we have continuous particles
                                       0.3, 0.3, 0.3, // Spread (x, y, z)
                                       0.02); // Speed
        
        // Additional puff of smoke
        player.getWorld().spawnParticle(Particle.SMOKE_LARGE, 
                                       player.getLocation().add(0, 1.8, 0), // At head level
                                       8, // Reduced from 10
                                       0.2, 0.2, 0.2, // Spread (x, y, z)
                                       0.01); // Speed
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("testjoints")) {
            // Basic connectivity test
            getLogger().info("=== TESTJOINTS COMMAND RECEIVED ===");
            getLogger().info("Sender: " + sender.getName());
            getLogger().info("Args length: " + args.length);
            if (args.length > 0) {
                getLogger().info("First arg: " + args[0]);
            }
            
            if (args.length == 0) {
                sender.sendMessage(ChatColor.GREEN + "LucyPlugin is working! Server can see your commands.");
                getLogger().info("Sent basic response to " + sender.getName());
                return true;
            }
            
            if (args.length > 0 && args[0].equalsIgnoreCase("convert")) {
                // Manual conversion test
                getLogger().info("=== MANUAL CONVERSION TEST ===");
                for (Player player : getServer().getOnlinePlayers()) {
                    getLogger().info("Testing conversion for player: " + player.getName());
                    convertRottenFleshToJoints(player);
                }
                sender.sendMessage(ChatColor.GREEN + "Manual conversion test completed. Check console for details.");
                return true;
            } else if (args.length > 0 && args[0].equalsIgnoreCase("give")) {
                // Give rotten flesh test
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    ItemStack rottenFlesh = new ItemStack(Material.ROTTEN_FLESH, 5);
                    player.getInventory().addItem(rottenFlesh);
                    getLogger().info("Gave 5 rotten flesh to " + player.getName());
                    sender.sendMessage(ChatColor.YELLOW + "Gave you 5 rotten flesh. They should convert automatically.");
                } else {
                    sender.sendMessage("This command can only be used by players.");
                }
                return true;
            } else {
                sender.sendMessage(ChatColor.YELLOW + "Usage:");
                sender.sendMessage(ChatColor.YELLOW + "/testjoints - Basic test");
                sender.sendMessage(ChatColor.YELLOW + "/testjoints convert - Force conversion test");
                sender.sendMessage(ChatColor.YELLOW + "/testjoints give - Give yourself rotten flesh");
                return true;
            }
        }
        return false;
    }
}
