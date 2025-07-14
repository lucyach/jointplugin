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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MyPlugin extends JavaPlugin implements Listener {

    private Map<UUID, Long> smokingCooldowns = new HashMap<>();
    private static final long SMOKING_COOLDOWN = 2000; // 2 seconds cooldown

    @Override
    public void onEnable() {
        getLogger().info("=== JointMaker 4.5 STARTING (RANDOM HIGH SYSTEM) ===");
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
        
        getLogger().info("=== JointMaker 4.5 STARTUP COMPLETE - SPREAD THE LOVE! ===");
    }

    @Override
    public void onDisable() {
        getLogger().info("JointMaker 4.5 has been disabled!");
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
            
            // Check cooldown
            UUID playerId = player.getUniqueId();
            long currentTime = System.currentTimeMillis();
            
            if (smokingCooldowns.containsKey(playerId)) {
                long lastSmoke = smokingCooldowns.get(playerId);
                if (currentTime - lastSmoke < SMOKING_COOLDOWN) {
                    long remainingCooldown = (SMOKING_COOLDOWN - (currentTime - lastSmoke)) / 1000;
                    player.sendMessage(ChatColor.RED + "You need to wait " + remainingCooldown + " more seconds before smoking again.");
                    return;
                }
            }
            
            // Set cooldown (extended for lighting + smoking)
            smokingCooldowns.put(playerId, currentTime);
            
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
        
        // PHASE 1: LIGHTING ANIMATION
        
        // Play lighter sound
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_FLINTANDSTEEL_USE, 0.8f, 1.2f);
        
        // Spawn flame particles for lighting
        player.getWorld().spawnParticle(Particle.FLAME, 
                                       player.getLocation().add(0, 1.6, 0), // Slightly lower than head
                                       3,
                                       0.05, 0.05, 0.05, // Very tight spread
                                       0.01); // Slow speed
        
        // Spawn some crit particles for lighter spark effect
        player.getWorld().spawnParticle(Particle.CRIT, 
                                       player.getLocation().add(0, 1.6, 0),
                                       2,
                                       0.1, 0.1, 0.1,
                                       0.02);
        
        // Schedule additional lighting effects
        new BukkitRunnable() {
            @Override
            public void run() {
                // More flame particles during lighting
                player.getWorld().spawnParticle(Particle.FLAME, 
                                               player.getLocation().add(0, 1.6, 0),
                                               4,
                                               0.08, 0.08, 0.08,
                                               0.01);
                
                // Additional crit particles for sparks
                player.getWorld().spawnParticle(Particle.CRIT, 
                                               player.getLocation().add(0, 1.6, 0),
                                               3,
                                               0.15, 0.15, 0.15,
                                               0.03);
                
                
                // Play ignition sound
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 0.5f, 1.5f);
            }
        }.runTaskLater(this, 40L); // 2 seconds into lighting

        // PHASE 2: START SMOKING (after lighting is complete)
        new BukkitRunnable() {
            @Override
            public void run() {
                smokeJoint(player, joint);
            }
        }.runTaskLater(this, 60L); // 3 seconds after lighting started
    }

    private void smokeJoint(Player player, ItemStack joint) {
        // Get current hits remaining
        int hitsLeft = getJointHits(joint);
        
        if (hitsLeft <= 0) {
            return;
        }
        
        // Add some initial smoking particles
        player.getWorld().spawnParticle(Particle.SMOKE, 
                                       player.getLocation().add(0, 1.8, 0), // At head level
                                       5,
                                       0.1, 0.1, 0.1, // Small spread
                                       0.01); // Slow speed

        // Play initial sound
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 0.3f, 0.8f);
        
        // Schedule intermediate effects during smoking
        new BukkitRunnable() {
            @Override
            public void run() {
                // More particles while inhaling
                player.getWorld().spawnParticle(Particle.SMOKE, 
                                               player.getLocation().add(0, 1.8, 0),
                                               3,
                                               0.15, 0.15, 0.15,
                                               0.02);
            }
        }.runTaskLater(this, 30L); // 1.5 seconds into smoking
        
        // Schedule the actual smoking effects after a delay
        new BukkitRunnable() {
            @Override
            public void run() {
                // Check if player still has the joint (might have been dropped/moved)
                ItemStack currentItem = player.getInventory().getItemInMainHand();
                if (!isJoint(currentItem)) {
                    return;
                }
                
                // Apply random high type effects
                MyPlugin.this.applyRandomHighEffects(player);
                
                // Apply contact high to nearby players
                MyPlugin.this.applyContactHigh(player);
                
                // Remove the joint since it's consumed (1 hit only)
                if (currentItem.getAmount() > 1) {
                    currentItem.setAmount(currentItem.getAmount() - 1);
                    player.getInventory().setItemInMainHand(currentItem);
                } else {
                    player.getInventory().setItemInMainHand(null);
                }
                
                // Play fire charge sound effect
                player.getWorld().playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 0.5f, 1.2f);
                
                // Spawn smoke particles
                player.getWorld().spawnParticle(Particle.SMOKE, 
                                               player.getLocation().add(0, 1.8, 0), // At head level
                                               8, // Standard smoke amount
                                               0.3, 0.3, 0.3, // Spread (x, y, z)
                                               0.02); // Speed
                
                // Additional puff of smoke
                player.getWorld().spawnParticle(Particle.LARGE_SMOKE, 
                                               player.getLocation().add(0, 1.8, 0), // At head level
                                               4, // Standard large smoke amount
                                               0.2, 0.2, 0.2, // Spread (x, y, z)
                                               0.01); // Speed
            }
        }.runTaskLater(this, 60L); // 3 second delay for smoking animation
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
        // Simplified for 1-hit joints - standard duration and amplifier
        int baseDuration = 300; // 15 seconds standard duration
        int baseAmplifier = 1; // Level 1 effects
        
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
        
        // Apply all effects with same duration (15 seconds = 300 ticks)
        player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, baseDuration, baseAmplifier));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, baseDuration, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, baseDuration, baseAmplifier));
        
        // Add bad high specific effects (same duration as base effects)
        player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, baseDuration, 1));
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
        player.sendMessage(ChatColor.GOLD + "GOOD HIGH: Damn, you chiefed hard!");
        
        // Apply all effects with same duration (15 seconds = 300 ticks)
        player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, baseDuration, baseAmplifier));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, baseDuration, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, baseDuration, baseAmplifier));
        
        // Add good high specific effects (same duration as base effects)
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, baseDuration, 2));
        player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, baseDuration, 1));
        
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
        
        // Apply all effects with same duration (15 seconds = 300 ticks)
        player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, baseDuration, baseAmplifier));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, baseDuration, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, baseDuration, baseAmplifier));
        
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("testjoints")) {
            // Basic connectivity test
            
            if (args.length == 0) {
                sender.sendMessage(ChatColor.GREEN + "LucyPlugin is working! Server can see your commands.");
                return true;
            }
            
            if (args.length > 0 && args[0].equalsIgnoreCase("convert")) {
                // Manual conversion test
                for (Player player : getServer().getOnlinePlayers()) {
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
                    sender.sendMessage(ChatColor.YELLOW + "Gave you 5 rotten flesh. They should convert automatically.");
                } else {
                    sender.sendMessage("This command can only be used by players.");
                }
                return true;
            } else if (args.length > 0 && args[0].equalsIgnoreCase("joint")) {
                // Give joints directly for testing
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    ItemStack joint = createJoint(1); // Give joint with 1 hit
                    joint.setAmount(3); // Give 3 joints
                    player.getInventory().addItem(joint);
                    sender.sendMessage(ChatColor.GREEN + "Gave you 3 joints (1 hit each)!");
                } else {
                    sender.sendMessage("This command can only be used by players.");
                }
                return true;
            } else if (args.length > 0 && args[0].equalsIgnoreCase("badhigh")) {
                // Force a bad high for testing
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    player.sendMessage(ChatColor.YELLOW + "Forcing a BAD HIGH for testing...");
                    applyBadHigh(player, 300, 1); // 15 second duration, level 1 amplifier
                } else {
                    sender.sendMessage("This command can only be used by players.");
                }
                return true;
            } else if (args.length > 0 && args[0].equalsIgnoreCase("goodhigh")) {
                // Force a good high for testing
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    player.sendMessage(ChatColor.YELLOW + "Forcing a GOOD HIGH for testing...");
                    applyGoodHigh(player, 300, 1); // 15 second duration, level 1 amplifier
                } else {
                    sender.sendMessage("This command can only be used by players.");
                }
                return true;
            } else if (args.length > 0 && args[0].equalsIgnoreCase("normalhigh")) {
                // Force a normal high for testing
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    player.sendMessage(ChatColor.YELLOW + "Forcing a NORMAL HIGH for testing...");
                    applyNormalHigh(player, 300, 1); // 15 second duration, level 1 amplifier
                } else {
                    sender.sendMessage("This command can only be used by players.");
                }
                return true;
            } else if (args.length > 0 && args[0].equalsIgnoreCase("randomtest")) {
                // Test the random high system multiple times
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    player.sendMessage(ChatColor.AQUA + "Testing random high system 10 times...");
                    
                    int badCount = 0, goodCount = 0, normalCount = 0;
                    for (int i = 0; i < 10; i++) {
                        double randomChance = Math.random();
                        if (randomChance < 0.10) {
                            badCount++;
                        } else if (randomChance < 0.30) {
                            goodCount++;
                        } else {
                            normalCount++;
                        }
                    }
                    
                    player.sendMessage(ChatColor.RED + "Bad highs: " + badCount + "/10");
                    player.sendMessage(ChatColor.GOLD + "Good highs: " + goodCount + "/10");
                    player.sendMessage(ChatColor.GREEN + "Normal highs: " + normalCount + "/10");
                } else {
                    sender.sendMessage("This command can only be used by players.");
                }
                return true;
            } else {
                sender.sendMessage(ChatColor.YELLOW + "Usage:");
                sender.sendMessage(ChatColor.YELLOW + "/testjoints - Basic test");
                sender.sendMessage(ChatColor.YELLOW + "/testjoints convert - Force conversion test");
                sender.sendMessage(ChatColor.YELLOW + "/testjoints give - Give yourself rotten flesh");
                sender.sendMessage(ChatColor.YELLOW + "/testjoints joint - Give yourself joints directly");
                sender.sendMessage(ChatColor.GOLD + "=== Random High Testing ===");
                sender.sendMessage(ChatColor.YELLOW + "/testjoints badhigh - Force bad high effects");
                sender.sendMessage(ChatColor.YELLOW + "/testjoints goodhigh - Force good high effects");
                sender.sendMessage(ChatColor.YELLOW + "/testjoints normalhigh - Force normal high effects");
                sender.sendMessage(ChatColor.YELLOW + "/testjoints randomtest - Test random distribution");
                return true;
            }
        }
        return false;
    }
}
