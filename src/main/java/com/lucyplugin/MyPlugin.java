package com.lucyplugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
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
        getLogger().info("=== JointMaker 4.8 STARTING (NO COMMANDS) ===");
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
                        player.hasPotionEffect(PotionEffectType.SLOWNESS)) {
                        
                        // Spawn subtle smoke particles around the player
                        player.getWorld().spawnParticle(Particle.SMOKE, 
                                                       player.getLocation().add(0, 1.8, 0), // At head level
                                                       3, // Fewer particles for continuous effect
                                                       0.2, 0.2, 0.2, // Smaller spread
                                                       0.01); // Slower speed
                        
                        // Apply passive contact high to very close players (within 2 blocks)
                        MyPlugin.this.applyPassiveContactHigh(player);
                    }
                }
            }
        }.runTaskTimer(this, 20L, 10L); // Run every 0.5 seconds (10 ticks)
        
        getLogger().info("=== JointMaker 4.8 STARTUP COMPLETE - SPREAD THE LOVE! ===");
    }

    @Override
    public void onDisable() {
        getLogger().info("JointMaker 4.8 has been disabled!");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        // First convert any rotten flesh to joint
        if (item != null && item.getType() == Material.ROTTEN_FLESH && !isJoint(item)) {
            ItemStack joint = createJoint();
            joint.setAmount(item.getAmount());
            player.getInventory().setItemInMainHand(joint);
            item = joint; // Update the reference
            player.sendMessage(ChatColor.YELLOW + "Converted rotten flesh to joint!");
        }
        
        // Then check if it's a joint to smoke
        if (isJoint(item)) {
            event.setCancelled(true);
            lightAndSmokeJoint(player, item);
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
        int convertedCount = 0;
        
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null) {
                
                if (item.getType() == Material.ROTTEN_FLESH) {
                    if (isJoint(item)) {
                        // Check if it's an old joint without durability data and update it
                        ItemMeta meta = item.getItemMeta();
                        if (meta != null && !meta.getPersistentDataContainer().has(new NamespacedKey(this, "joint_hits"), PersistentDataType.INTEGER)) {
                            updateJointHits(item, 1); // Give old joints 1 hit
                            convertedCount++;
                        }
                    } else {
                        ItemStack joint = createJoint();
                        joint.setAmount(item.getAmount());
                        player.getInventory().setItem(i, joint);
                        convertedCount++;
                    }
                }
            }
        }
        
        if (convertedCount > 0) {
            player.sendMessage(ChatColor.GREEN + "Converted " + convertedCount + " rotten flesh to joints!");
        }
    }

    private ItemStack createJoint() {
        return createJoint(1); // Default 1 hit per joint
    }
    
    private ItemStack createJoint(int hits) {
        ItemStack joint = new ItemStack(Material.ROTTEN_FLESH);
        ItemMeta meta = joint.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + "Joint");
            meta.setLore(Arrays.asList(
                ChatColor.GRAY + "A rolled cannabis cigarette",
                ChatColor.YELLOW + "Right-click to smoke"
            ));
            
            // Add a custom tag to identify this as a joint
            meta.getPersistentDataContainer().set(
                new NamespacedKey(this, "joint"), 
                PersistentDataType.BYTE, 
                (byte) 1
            );
            
            // Store the number of hits remaining
            meta.getPersistentDataContainer().set(
                new NamespacedKey(this, "joint_hits"), 
                PersistentDataType.INTEGER, 
                hits
            );
            
            joint.setItemMeta(meta);
        }
        
        return joint;
    }

    private int getJointHits(ItemStack item) {
        if (!isJoint(item)) {
            return 0;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return 0;
        }
        
        NamespacedKey hitsKey = new NamespacedKey(this, "joint_hits");
        if (meta.getPersistentDataContainer().has(hitsKey, PersistentDataType.INTEGER)) {
            return meta.getPersistentDataContainer().get(hitsKey, PersistentDataType.INTEGER);
        }
        
        // If no hits data found, assume it's an old joint and give it 1 hit
        return 1;
    }
    
    private void updateJointHits(ItemStack joint, int newHits) {
        ItemMeta meta = joint.getItemMeta();
        if (meta != null) {
            // Update display name (simplified for 1-hit joints)
            meta.setDisplayName(ChatColor.GREEN + "Joint");
            
            // Update lore (simplified for 1-hit joints)
            meta.setLore(Arrays.asList(
                ChatColor.GRAY + "A rolled cannabis cigarette",
                ChatColor.YELLOW + "Right-click to smoke"
            ));
            
            // Update hits data
            meta.getPersistentDataContainer().set(
                new NamespacedKey(this, "joint_hits"), 
                PersistentDataType.INTEGER, 
                newHits
            );
            
            joint.setItemMeta(meta);
        }
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
            return false;
        }
        
        // Check for the joint tag in persistent data
        NamespacedKey key = new NamespacedKey(this, "joint");
        boolean hasJointTag = meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
        
        return hasJointTag;
    }

    private void lightAndSmokeJoint(Player player, ItemStack joint) {
        // Get current hits remaining
        int hitsLeft = getJointHits(joint);
        
        if (hitsLeft <= 0) {
            return;
        }
        
        // IMMEDIATE EFFECTS - NO DELAYS
        
        // Play lighter sound
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_FLINTANDSTEEL_USE, 0.8f, 1.2f);
        
        // Play ignition sound
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 0.5f, 1.5f);
        
        // Play smoking sound
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 0.5f, 1.2f);
        
        // Spawn all particles immediately
        player.getWorld().spawnParticle(Particle.FLAME, 
                                       player.getLocation().add(0, 1.6, 0),
                                       7, // Combined flame particles
                                       0.08, 0.08, 0.08,
                                       0.01);
        
        player.getWorld().spawnParticle(Particle.CRIT, 
                                       player.getLocation().add(0, 1.6, 0),
                                       5, // Combined crit particles
                                       0.15, 0.15, 0.15,
                                       0.03);
        
        player.getWorld().spawnParticle(Particle.SMOKE, 
                                       player.getLocation().add(0, 1.8, 0),
                                       13, // Combined smoke particles
                                       0.3, 0.3, 0.3,
                                       0.02);
        
        player.getWorld().spawnParticle(Particle.LARGE_SMOKE, 
                                       player.getLocation().add(0, 1.8, 0),
                                       4,
                                       0.2, 0.2, 0.2,
                                       0.01);
        
        // Apply effects immediately
        ItemStack currentItem = player.getInventory().getItemInMainHand();
        if (isJoint(currentItem)) {
            // Apply random high type effects
            this.applyRandomHighEffects(player);
            
            // Apply contact high to nearby players
            this.applyContactHigh(player);
            
            // Remove the joint since it's consumed (1 hit only)
            if (currentItem.getAmount() > 1) {
                currentItem.setAmount(currentItem.getAmount() - 1);
                player.getInventory().setItemInMainHand(currentItem);
            } else {
                player.getInventory().setItemInMainHand(null);
            }
        }
    }

    private void applyContactHigh(Player smoker) {
        // Define contact high range (blocks)
        double contactRange = 5.0; // 5 block radius
        
        // Get all players in the same world
        for (Player nearbyPlayer : smoker.getWorld().getPlayers()) {
            // Skip the smoker themselves
            if (nearbyPlayer.equals(smoker)) {
                continue;
            }
            
            // Check if player is within range
            double distance = smoker.getLocation().distance(nearbyPlayer.getLocation());
            if (distance <= contactRange) {
                // Calculate effect strength based on distance (simplified for 1-hit joints)
                double effectStrength = Math.max(0.1, 1.0 - (distance / contactRange)); // 10% minimum, 100% maximum
                int contactDuration = (int) (effectStrength * 100 + 100); // 100-200 ticks (5-10 seconds)
                int contactAmplifier = Math.max(0, (int) (effectStrength * 0.5)); // Simplified weak amplifier
                
                // Apply weaker contact high effects - all with same duration
                nearbyPlayer.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, contactDuration, Math.min(contactAmplifier, 1))); // Max level 1
                nearbyPlayer.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, contactDuration, 0));
                
                // Send contact high message with distance info
                String distanceDesc = distance < 2.0 ? "very close" : distance < 3.5 ? "close" : "nearby";
                nearbyPlayer.sendMessage(ChatColor.LIGHT_PURPLE + "You got a contact high from " + smoker.getName() + " (" + distanceDesc + ")!");
                
                // Spawn some smoke particles around the affected player
                nearbyPlayer.getWorld().spawnParticle(Particle.SMOKE, 
                                                     nearbyPlayer.getLocation().add(0, 1.5, 0),
                                                     (int) (effectStrength * 3 + 1), // 1-4 particles based on strength
                                                     0.3, 0.3, 0.3,
                                                     0.01);
                
                // Play a subtle sound for contact high
                nearbyPlayer.getWorld().playSound(nearbyPlayer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.3f, 0.7f);
                
                // Notify the smoker about affecting others
                smoker.sendMessage(ChatColor.YELLOW + nearbyPlayer.getName() + " got a contact high from your smoke!");
            }
        }
    }

    private void applyPassiveContactHigh(Player highPlayer) {
        // Only apply passive contact high occasionally (every 10 seconds) to prevent spam
        if (Math.random() > 0.05) { // 5% chance per check (every 0.5 seconds = ~10 second average)
            return;
        }
        
        double passiveRange = 2.0; // Very close range for passive effect
        
        for (Player nearbyPlayer : highPlayer.getWorld().getPlayers()) {
            if (nearbyPlayer.equals(highPlayer)) {
                continue;
            }
            
            double distance = highPlayer.getLocation().distance(nearbyPlayer.getLocation());
            if (distance <= passiveRange) {
                // Very weak passive effects
                nearbyPlayer.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 60, 0)); // 3 seconds, level 0
                
                nearbyPlayer.sendMessage(ChatColor.DARK_GRAY + "You notice a faint smell of smoke from " + highPlayer.getName() + "...");
                
                // Very subtle particles
                nearbyPlayer.getWorld().spawnParticle(Particle.SMOKE, 
                                                     nearbyPlayer.getLocation().add(0, 1.5, 0),
                                                     1, // Just 1 particle
                                                     0.1, 0.1, 0.1,
                                                     0.005);
            }
        }
    }

    private void applyRandomHighEffects(Player player) {
        // Check for existing effects to determine stacking behavior
        int baseDuration = 300; // 15 seconds base duration
        int baseAmplifier = 1; // Level 1 effects
        
        // Check if player already has high effects and increase accordingly
        if (player.hasPotionEffect(PotionEffectType.HUNGER) || 
            player.hasPotionEffect(PotionEffectType.SLOW_FALLING) ||
            player.hasPotionEffect(PotionEffectType.SLOWNESS)) {
            
            // Player is already high - extend duration and potentially increase amplifier
            baseDuration = 450; // Extend to 22.5 seconds
            
            // Check current amplifier levels and increase if possible
            PotionEffect existingHunger = player.getPotionEffect(PotionEffectType.HUNGER);
            if (existingHunger != null && existingHunger.getAmplifier() < 3) {
                baseAmplifier = existingHunger.getAmplifier() + 1; // Increase amplifier up to level 3
            }
        }
        
        // Generate random number to determine high type
        double randomChance = Math.random(); // 0.0 to 1.0
        
        if (randomChance < 0.10) {
            // 10% chance for BAD HIGH (greening out)
            applyBadHigh(player, baseDuration, baseAmplifier);
        } else if (randomChance < 0.30) {
            // 20% chance for GOOD HIGH (chiefed hard) - 0.10 to 0.30 = 20%
            applyGoodHigh(player, baseDuration, baseAmplifier);
        } else {
            // 70% chance for NORMAL HIGH - 0.30 to 1.0 = 70%
            applyNormalHigh(player, baseDuration, baseAmplifier);
        }
    }
    
    private void applyBadHigh(Player player, int baseDuration, int baseAmplifier) {
        // BAD HIGH: Greening out effects
        player.sendMessage(ChatColor.DARK_RED + "BAD HIGH: you're greening out!");
        
        // Apply all effects with same duration (15+ seconds = 300+ ticks)
        player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, baseDuration, baseAmplifier));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, baseDuration, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, baseDuration, baseAmplifier));
        
        // Add bad high specific effects (same duration as base effects)
        player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, baseDuration, Math.min(baseAmplifier, 2))); // Cap nausea at level 2
        player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, baseDuration, 0));
        
        // Play a disturbing sound
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GHAST_SCREAM, 0.3f, 1.5f);
        
        // Spawn some sickly green particles
        player.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, 
                                       player.getLocation().add(0, 1.8, 0),
                                       8,
                                       0.5, 0.5, 0.5,
                                       0.02);
    }
    
    private void applyGoodHigh(Player player, int baseDuration, int baseAmplifier) {
        // GOOD HIGH: Chiefed hard effects
        player.sendMessage(ChatColor.GOLD + "GOOD HIGH: You chiefed hard!");
        
        // Apply all effects with same duration (15+ seconds = 300+ ticks)
        player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, baseDuration, baseAmplifier));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, baseDuration, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, baseDuration, baseAmplifier));
        
        // Add good high specific effects (same duration as base effects)
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, baseDuration, Math.min(baseAmplifier + 1, 4))); // Scale jump boost, cap at level 4
        player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, baseDuration, Math.min(baseAmplifier, 3))); // Scale luck, cap at level 3
        
        // Play a positive sound
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);
        
        // Spawn some golden/happy particles
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, 
                                       player.getLocation().add(0, 1.8, 0),
                                       8,
                                       0.5, 0.5, 0.5,
                                       0.02);
        
        // Additional enchanted particles for extra sparkle
        player.getWorld().spawnParticle(Particle.ENCHANT, 
                                       player.getLocation().add(0, 1.8, 0),
                                       12,
                                       0.7, 0.7, 0.7,
                                       0.03);
    }
    
    private void applyNormalHigh(Player player, int baseDuration, int baseAmplifier) {
        // NORMAL HIGH: Standard effects (hunger, slowness, slow falling)
        player.sendMessage(ChatColor.GOLD + "NORMAL HIGH: You're feeling it!");

        // Apply all effects with same duration (15+ seconds = 300+ ticks)
        player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, baseDuration, baseAmplifier));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, baseDuration, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, baseDuration, baseAmplifier));
        
    }
}
