package fr.volcastaff;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.ArmorStand;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class Volcalypse extends JavaPlugin implements Listener {

    private NamespacedKey interceptorKey;
    private NamespacedKey placerKey;

    // Zones radioactives (centre -> temps d'expiration) - toutes les explosions sauf small
    private final Map<Location, ZoneData> explosionZones = new ConcurrentHashMap<>();
    private static final long ZONE_DURATION = 30 * 60 * 1000; // 30 minutes en ms

    // Classe pour stocker les données d'une zone
    static class ZoneData {
        final long expirationTime;
        final String type; // "nuclear", "antimaterial", "incendiary", "medium", "large"
        final double radius;

        ZoneData(long expirationTime, String type, double radius) {
            this.expirationTime = expirationTime;
            this.type = type;
            this.radius = radius;
        }
    }

    @Override
    public void onEnable() {
        getLogger().info("[VOLCALYPSE] Starting plugin - Version 1.21.11 Compatible");
        getServer().getPluginManager().registerEvents(this, this);

        interceptorKey = new NamespacedKey(this, "interceptor_level");
        placerKey = new NamespacedKey(this, "placer_uuid");

        // Enregistrement des commandes
        getCommand("missile").setExecutor(new MissileCommand());
        getCommand("interceptor").setExecutor(new InterceptorCommand());
        getCommand("removeinterceptor").setExecutor(new RemoveInterceptorCommand());

        // Nettoyage des zones expirées + effets de particules
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                explosionZones.entrySet().removeIf(entry -> {
                    if (now > entry.getValue().expirationTime) {
                        return true;
                    }

                    // Particules persistantes selon le type de zone
                    Location loc = entry.getKey();
                    ZoneData data = entry.getValue();

                    if (loc.getWorld() != null) {
                        switch (data.type) {
                            case "nuclear" -> {
                                // Particules radioactives vertes
                                for (int i = 0; i < 5; i++) {
                                    double angle = Math.random() * Math.PI * 2;
                                    double radius = Math.random() * data.radius;
                                    double x = loc.getX() + Math.cos(angle) * radius;
                                    double z = loc.getZ() + Math.sin(angle) * radius;
                                    double y = loc.getY() + Math.random() * 8;
                                    Location pLoc = new Location(loc.getWorld(), x, y, z);
                                    loc.getWorld().spawnParticle(Particle.SOUL, pLoc, 2, 0.2, 0.2, 0.2, 0);
                                    loc.getWorld().spawnParticle(Particle.WITCH, pLoc, 1, 0.1, 0.1, 0.1, 0);
                                }
                            }
                            case "antimaterial" -> {
                                // Particules de portail violet
                                for (int i = 0; i < 4; i++) {
                                    double angle = Math.random() * Math.PI * 2;
                                    double radius = Math.random() * data.radius;
                                    double x = loc.getX() + Math.cos(angle) * radius;
                                    double z = loc.getZ() + Math.sin(angle) * radius;
                                    double y = loc.getY() + Math.random() * 10;
                                    Location pLoc = new Location(loc.getWorld(), x, y, z);
                                    loc.getWorld().spawnParticle(Particle.PORTAL, pLoc, 3, 0.3, 0.3, 0.3, 0.5);
                                    loc.getWorld().spawnParticle(Particle.REVERSE_PORTAL, pLoc, 1, 0.1, 0.1, 0.1, 0.2);
                                }
                            }
                            case "incendiary" -> {
                                // Particules de fumée et braises
                                for (int i = 0; i < 6; i++) {
                                    double angle = Math.random() * Math.PI * 2;
                                    double radius = Math.random() * data.radius;
                                    double x = loc.getX() + Math.cos(angle) * radius;
                                    double z = loc.getZ() + Math.sin(angle) * radius;
                                    double y = loc.getY() + Math.random() * 5;
                                    Location pLoc = new Location(loc.getWorld(), x, y, z);
                                    loc.getWorld().spawnParticle(Particle.SMOKE, pLoc, 2, 0.2, 0.2, 0.2, 0.02);
                                    loc.getWorld().spawnParticle(Particle.FLAME, pLoc, 1, 0.1, 0.1, 0.1, 0);
                                }
                            }
                            case "medium", "large" -> {
                                // Fumée résiduelle
                                for (int i = 0; i < 3; i++) {
                                    double angle = Math.random() * Math.PI * 2;
                                    double radius = Math.random() * data.radius;
                                    double x = loc.getX() + Math.cos(angle) * radius;
                                    double z = loc.getZ() + Math.sin(angle) * radius;
                                    double y = loc.getY() + Math.random() * 6;
                                    Location pLoc = new Location(loc.getWorld(), x, y, z);
                                    loc.getWorld().spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, pLoc, 1, 0.1, 0.3, 0.1, 0.01);
                                }
                            }
                        }
                    }
                    return false;
                });
            }
        }.runTaskTimer(this, 0, 20);
    }

    @Override
    public void onDisable() {
        getLogger().info("[VOLCALYPSE] Disabling plugin");
        explosionZones.clear();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction().isRightClick() && event.hasItem() && event.hasBlock()) {
            ItemStack item = event.getItem();

            if (item.getType() == Material.ARMOR_STAND && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();

                if (meta.getPersistentDataContainer().has(interceptorKey, PersistentDataType.INTEGER)) {
                    Integer level = meta.getPersistentDataContainer().get(interceptorKey, PersistentDataType.INTEGER);
                    Location loc = event.getClickedBlock().getLocation().add(0.5, 1, 0.5);

                    ArmorStand armorStand = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
                    armorStand.setCustomName("§e§lInterceptor Level " + level);
                    armorStand.setCustomNameVisible(true);
                    armorStand.setGravity(false);
                    armorStand.setBasePlate(false);
                    armorStand.setArms(true);
                    armorStand.getPersistentDataContainer().set(interceptorKey, PersistentDataType.INTEGER, level);
                    armorStand.getPersistentDataContainer().set(placerKey, PersistentDataType.STRING, event.getPlayer().getUniqueId().toString());

                    // Effet de placement
                    loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 50, 0.5, 1, 0.5, 0.1);
                    loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);

                    event.setCancelled(true);
                    item.setAmount(item.getAmount() - 1);
                }
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof ArmorStand armorStand && event.getDamager() instanceof Player player) {
            if (armorStand.getPersistentDataContainer().has(interceptorKey, PersistentDataType.INTEGER)) {
                Integer level = armorStand.getPersistentDataContainer().get(interceptorKey, PersistentDataType.INTEGER);

                ItemStack item = new ItemStack(Material.ARMOR_STAND);
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName("§r§eInterceptor level " + level);
                meta.getPersistentDataContainer().set(interceptorKey, PersistentDataType.INTEGER, level);
                item.setItemMeta(meta);

                player.getInventory().addItem(item);
                player.sendMessage("§aInterceptor level " + level + " récupéré !");

                Location loc = armorStand.getLocation();
                loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 30, 0.5, 1, 0.5, 0);
                loc.getWorld().playSound(loc, Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);

                armorStand.remove();
                event.setCancelled(true);
            }
        }
    }

    // Vérification des zones d'explosion
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location loc = player.getLocation();

        for (Map.Entry<Location, ZoneData> zone : explosionZones.entrySet()) {
            if (zone.getKey().getWorld().equals(loc.getWorld())) {
                if (zone.getKey().distance(loc) <= zone.getValue().radius) {
                    // Le joueur est dans une zone contaminée
                    String type = zone.getValue().type;

                    switch (type) {
                        case "nuclear" -> {
                            player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 80, 3, false, true));
                            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 120, 2, false, true));
                            player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 2, false, true));
                            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 1, false, true));
                            player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 100, 3, false, true));
                        }
                        case "antimaterial" -> {
                            player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 4, false, true));
                            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 140, 3, false, true));
                            player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 80, 3, false, true));
                            player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 100, 1, false, true));
                        }
                        case "incendiary" -> {
                            player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 1, false, true));
                            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 80, 1, false, true));
                            player.setFireTicks(40);
                        }
                        case "medium" -> {
                            player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 40, 1, false, true));
                            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 60, 0, false, true));
                        }
                        case "large" -> {
                            player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 2, false, true));
                            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 80, 1, false, true));
                            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1, false, true));
                        }
                    }
                    break;
                }
            }
        }
    }

    public class InterceptorCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cCette commande est réservée aux joueurs !");
                return true;
            }

            if (!player.isOp()) {
                player.sendMessage("§cVous devez être opérateur pour utiliser cette commande !");
                return true;
            }

            if (args.length != 1) {
                player.sendMessage("§cUsage: /interceptor <niveau>");
                player.sendMessage("§cNiveaux disponibles: 1 à 4");
                return true;
            }

            try {
                int level = Integer.parseInt(args[0]);

                if (level < 1 || level > 4) {
                    player.sendMessage("§cLe niveau doit être entre 1 et 4 !");
                    return true;
                }

                ItemStack item = new ItemStack(Material.ARMOR_STAND);
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName("§r§eInterceptor level " + level);
                meta.getPersistentDataContainer().set(interceptorKey, PersistentDataType.INTEGER, level);
                item.setItemMeta(meta);

                player.getInventory().addItem(item);
                player.sendMessage("§aInterceptor level " + level + " ajouté à votre inventaire !");

            } catch (NumberFormatException e) {
                player.sendMessage("§cLe niveau doit être un nombre entier !");
                return true;
            }

            return true;
        }
    }

    public class RemoveInterceptorCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cCette commande est réservée aux joueurs !");
                return true;
            }

            if (!player.isOp()) {
                player.sendMessage("§cVous devez être opérateur pour utiliser cette commande !");
                return true;
            }

            if (args.length != 1) {
                player.sendMessage("§cUsage: /removeinterceptor <rayon>");
                return true;
            }

            try {
                double radius = Double.parseDouble(args[0]);

                if (radius < 0) {
                    player.sendMessage("§cLe rayon doit être positif !");
                    return true;
                }

                Location playerLoc = player.getLocation();
                World world = playerLoc.getWorld();
                int removed = 0;

                List<ArmorStand> toRemove = new ArrayList<>();

                for (Entity entity : world.getNearbyEntities(playerLoc, radius, radius, radius)) {
                    if (entity instanceof ArmorStand armorStand) {
                        if (armorStand.getPersistentDataContainer().has(interceptorKey, PersistentDataType.INTEGER)) {
                            toRemove.add(armorStand);
                        }
                    }
                }

                for (ArmorStand stand : toRemove) {
                    stand.getWorld().spawnParticle(Particle.CLOUD, stand.getLocation(), 20, 0.3, 0.5, 0.3, 0);
                    stand.remove();
                    removed++;
                }

                player.sendMessage("§a" + removed + " interceptor(s) supprimé(s) dans un rayon de " + radius + " blocs.");

            } catch (NumberFormatException e) {
                player.sendMessage("§cLe rayon doit être un nombre !");
                return true;
            }

            return true;
        }
    }

    public class MissileCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cCette commande est réservée aux joueurs !");
                return true;
            }

            if (!player.isOp()) {
                player.sendMessage("§cVous devez être opérateur pour utiliser cette commande !");
                return true;
            }

            if (args.length < 3) {
                player.sendMessage("§cUsage: /missile <type> <x> <z>");
                player.sendMessage("§cTypes disponibles: small, medium, large, nuclear, antimaterial, incendiary");
                return true;
            }

            try {
                String missileType = args[0].toLowerCase();
                int x = Integer.parseInt(args[1]);
                int z = Integer.parseInt(args[2]);

                launchMissile(missileType, x, z, player.getWorld());
                player.sendMessage("§aMissile " + missileType + " lancé à X:" + x + " Z:" + z);

            } catch (NumberFormatException e) {
                player.sendMessage("§cLes coordonnées doivent être des nombres entiers !");
                return true;
            }

            return true;
        }
    }

    public void launchMissile(String missileType, int x, int z, World world) {
        int startY = 300;
        Location startLoc = new Location(world, x + 0.5, startY, z + 0.5);

        TNTPrimed missile = (TNTPrimed) world.spawnEntity(startLoc, EntityType.TNT);
        missile.setFuseTicks(10000);
        missile.setGravity(false);

        world.spawnParticle(Particle.EXPLOSION, startLoc, 10, 1, 1, 1, 0);
        world.playSound(startLoc, Sound.ENTITY_WITHER_SHOOT, 2.0f, 0.5f);

        new BukkitRunnable() {
            int currentTick = 0;
            double spiralAngle = 0;

            @Override
            public void run() {
                if (missile.isDead()) {
                    cancel();
                    return;
                }

                Location loc = missile.getLocation();

                switch (missileType.toLowerCase()) {
                    case "nuclear" -> {
                        world.spawnParticle(Particle.FLAME, loc, 20, 0.3, 0.3, 0.3, 0.05);
                        world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 10, 0.2, 0.2, 0.2, 0.02);
                        world.spawnParticle(Particle.ELECTRIC_SPARK, loc, 15, 0.4, 0.4, 0.4, 0.1);
                        world.spawnParticle(Particle.GLOW, loc, 5, 0.3, 0.3, 0.3, 0);
                        if (currentTick % 10 == 0) {
                            world.playSound(loc, Sound.BLOCK_BEACON_AMBIENT, 0.5f, 2.0f);
                        }
                    }
                    case "antimaterial" -> {
                        world.spawnParticle(Particle.PORTAL, loc, 30, 0.5, 0.5, 0.5, 1);
                        world.spawnParticle(Particle.REVERSE_PORTAL, loc, 20, 0.3, 0.3, 0.3, 0.5);
                        world.spawnParticle(Particle.ENCHANT, loc, 10, 0.4, 0.4, 0.4, 2);
                        world.spawnParticle(Particle.WITCH, loc, 5, 0.2, 0.2, 0.2, 0);
                    }
                    case "incendiary" -> {
                        world.spawnParticle(Particle.FLAME, loc, 25, 0.4, 0.4, 0.4, 0.1);
                        world.spawnParticle(Particle.LAVA, loc, 8, 0.3, 0.3, 0.3, 0);
                        world.spawnParticle(Particle.DRIPPING_LAVA, loc, 15, 0.3, 0.3, 0.3, 0);
                        world.spawnParticle(Particle.SMOKE, loc, 10, 0.2, 0.2, 0.2, 0.05);
                    }
                    default -> {
                        world.spawnParticle(Particle.FLAME, loc, 15, 0.2, 0.2, 0.2, 0.1);
                        world.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, loc, 8, 0.15, 0.15, 0.15, 0.05);
                        world.spawnParticle(Particle.FIREWORK, loc, 3, 0.1, 0.1, 0.1, 0.05);
                    }
                }

                // Particules de traînée en spirale - SANS Particle.DUST
                for (int i = 0; i < 3; i++) {
                    double radius = 0.8;
                    double offsetX = Math.cos(spiralAngle + i * Math.PI * 2 / 3) * radius;
                    double offsetZ = Math.sin(spiralAngle + i * Math.PI * 2 / 3) * radius;
                    Location spiralLoc = loc.clone().add(offsetX, 0, offsetZ);
                    // Remplacé DUST par FLAME pour éviter les problèmes
                    world.spawnParticle(Particle.FLAME, spiralLoc, 1, 0, 0, 0, 0);
                }
                spiralAngle += 0.3;

                if (currentTick % 5 == 0) {
                    world.playSound(loc, Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 0.8f, 1.2f);
                }

                missile.setVelocity(new Vector(0, -0.5, 0));
                currentTick++;

                if (currentTick % 58 == 0 && !missileType.equals("antimaterial")) {
                    Integer maxLevel = 0;
                    ArmorStand bestInterceptor = null;

                    for (Entity entity : world.getNearbyEntities(loc, 30, 30, 30)) {
                        if (entity instanceof ArmorStand armorStand) {
                            if (armorStand.getPersistentDataContainer().has(interceptorKey, PersistentDataType.INTEGER)) {
                                Integer level = armorStand.getPersistentDataContainer().get(interceptorKey, PersistentDataType.INTEGER);

                                if (level > maxLevel) {
                                    maxLevel = level;
                                    bestInterceptor = armorStand;
                                }
                            }
                        }
                    }

                    if (maxLevel > 0 && bestInterceptor != null) {
                        double chance = switch (maxLevel) {
                            case 1 -> 0.1;
                            case 2 -> 0.3;
                            case 3 -> 0.5;
                            case 4 -> 0.8;
                            default -> 0.0;
                        };

                        if (Math.random() < chance) {
                            Location interceptorLoc = bestInterceptor.getLocation();

                            Vector from = interceptorLoc.toVector();
                            Vector to = loc.toVector();
                            Vector dir = to.clone().subtract(from).normalize();
                            double distance = from.distance(to);

                            for (double d = 0; d < distance; d += 0.3) {
                                Location pLoc = interceptorLoc.clone().add(dir.clone().multiply(d));
                                world.spawnParticle(Particle.FLASH, pLoc, 1);
                                world.spawnParticle(Particle.ELECTRIC_SPARK, pLoc, 3, 0.1, 0.1, 0.1, 0);
                                world.spawnParticle(Particle.END_ROD, pLoc, 1, 0, 0, 0, 0);
                            }

                            world.playSound(interceptorLoc, Sound.ENTITY_WARDEN_SONIC_BOOM, 2.0f, 1.0f);
                            world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1.5f);

                            world.spawnParticle(Particle.EXPLOSION, loc, 20, 1, 1, 1, 0);
                            world.spawnParticle(Particle.FLASH, loc, 5);
                            world.spawnParticle(Particle.FIREWORK, loc, 50, 1, 1, 1, 0.2);

                            missile.remove();
                            cancel();
                            return;
                        }
                    }
                }

                Location below = loc.clone().subtract(0, 0.5, 0);
                if (below.getBlock().getType().isSolid() || loc.getY() <= world.getMinHeight()) {
                    bomb(missileType, (int) loc.getX(), (int) loc.getY(), (int) loc.getZ(), world);
                    missile.remove();
                    cancel();
                }
            }
        }.runTaskTimer(this, 0, 1);
    }

    public void bomb(String missileType, int x, int y, int z, World world) {
        Location center = new Location(world, x, y, z);
        getLogger().info("[VOLCALYPSE] Explosion " + missileType + " at " + x + "," + y + "," + z);

        switch (missileType.toLowerCase()) {
            case "small" -> {
                world.createExplosion(center, 4.0f, false, true);

                // Effets simples
                world.spawnParticle(Particle.EXPLOSION, center, 30, 2, 2, 2, 0);
                world.spawnParticle(Particle.FLAME, center, 50, 2, 2, 2, 0.05);
                world.spawnParticle(Particle.SMOKE, center, 40, 2, 2, 2, 0.1);
                world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1.0f);
            }

            case "medium" -> {
                world.createExplosion(center, 20.0f, true, true);

                // Effets moyens avec ondes de choc
                world.spawnParticle(Particle.EXPLOSION, center, 80, 5, 5, 5, 0);
                world.spawnParticle(Particle.FLAME, center, 150, 4, 4, 4, 0.15);
                world.spawnParticle(Particle.LARGE_SMOKE, center, 100, 5, 5, 5, 0.2);
                world.spawnParticle(Particle.FIREWORK, center, 60, 5, 5, 5, 0.3);

                // Onde de choc
                for (int i = 1; i <= 3; i++) {
                    int finalI = i;
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            double radius = finalI * 7;
                            for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 16) {
                                double dx = Math.cos(angle) * radius;
                                double dz = Math.sin(angle) * radius;
                                Location shockLoc = center.clone().add(dx, 0, dz);
                                world.spawnParticle(Particle.SWEEP_ATTACK, shockLoc, 1);
                                world.spawnParticle(Particle.CLOUD, shockLoc, 3, 0.2, 0.5, 0.2, 0);
                            }
                        }
                    }.runTaskLater(this, i * 3);
                }

                world.playSound(center, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 2.0f, 0.8f);

                // Zone contaminée 30 min
                explosionZones.put(center.clone(), new ZoneData(System.currentTimeMillis() + ZONE_DURATION, "medium", 20.0));
            }

            case "large" -> {
                world.createExplosion(center, 40.0f, true, true);

                // Effets massifs avec multiples couches
                world.spawnParticle(Particle.EXPLOSION, center, 150, 10, 10, 10, 0);
                world.spawnParticle(Particle.FLASH, center, 20);
                world.spawnParticle(Particle.FLAME, center, 300, 8, 8, 8, 0.25);
                world.spawnParticle(Particle.LARGE_SMOKE, center, 200, 10, 10, 10, 0.3);
                world.spawnParticle(Particle.CLOUD, center, 180, 10, 10, 10, 0.4);
                world.spawnParticle(Particle.LAVA, center, 40, 6, 6, 6, 0);

                // Ondes de choc multiples
                for (int wave = 1; wave <= 5; wave++) {
                    int finalWave = wave;
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            double radius = finalWave * 10;
                            for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 24) {
                                double dx = Math.cos(angle) * radius;
                                double dz = Math.sin(angle) * radius;
                                Location shockLoc = center.clone().add(dx, 0, dz);
                                world.spawnParticle(Particle.SWEEP_ATTACK, shockLoc, 2);
                                world.spawnParticle(Particle.EXPLOSION, shockLoc, 1);
                                world.spawnParticle(Particle.CLOUD, shockLoc, 5, 0.3, 0.8, 0.3, 0);
                            }
                            world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);
                        }
                    }.runTaskLater(this, wave * 4);
                }

                // Colonne de fumée ascendante
                new BukkitRunnable() {
                    int tick = 0;
                    @Override
                    public void run() {
                        if (tick > 40) {
                            cancel();
                            return;
                        }
                        Location smokeLoc = center.clone().add(0, tick * 2, 0);
                        world.spawnParticle(Particle.LARGE_SMOKE, smokeLoc, 10, 3, 1, 3, 0.05);
                        world.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, smokeLoc, 5, 2, 1, 2, 0.02);
                        tick++;
                    }
                }.runTaskTimer(this, 5, 2);

                world.playSound(center, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 2.5f, 0.5f);
                world.playSound(center, Sound.ENTITY_WITHER_DEATH, 1.5f, 0.7f);

                // Zone contaminée 30 min
                explosionZones.put(center.clone(), new ZoneData(System.currentTimeMillis() + ZONE_DURATION, "large", 40.0));
            }

            case "nuclear" -> {
                double radiusNuclear = 20.0;
                int explosionCount = 50;
                double power = 40.0f;

                // Explosions sphériques
                for (int i = 0; i < explosionCount; i++) {
                    double theta = Math.random() * 2 * Math.PI;
                    double phi = Math.acos(2 * Math.random() - 1);

                    double dx = radiusNuclear * Math.sin(phi) * Math.cos(theta);
                    double dy = radiusNuclear * Math.sin(phi) * Math.sin(theta);
                    double dz = radiusNuclear * Math.cos(phi);

                    Location explosionLoc = center.clone().add(dx, dy, dz);
                    world.createExplosion(explosionLoc, (float) (power * 0.5), true, true);
                }

                world.createExplosion(center, (float) power, true, true);

                // ═══════════════════════════════════════
                // EFFETS NUCLÉAIRES ULTRA GRANDIOSES
                // ═══════════════════════════════════════

                // Flash initial aveuglant
                world.spawnParticle(Particle.FLASH, center, 100);
                world.spawnParticle(Particle.EXPLOSION, center, 500, 20, 20, 20, 0);

                // Boule de feu centrale
                world.spawnParticle(Particle.FLAME, center, 800, 15, 15, 15, 0.8);
                world.spawnParticle(Particle.SOUL_FIRE_FLAME, center, 400, 12, 12, 12, 0.5);
                world.spawnParticle(Particle.LAVA, center, 150, 10, 10, 10, 0);

                // Particules radioactives vertes
                world.spawnParticle(Particle.GLOW, center, 300, 15, 15, 15, 0.5);
                world.spawnParticle(Particle.ELECTRIC_SPARK, center, 250, 15, 15, 15, 0.6);
                world.spawnParticle(Particle.SOUL, center, 200, 12, 12, 12, 0.3);
                world.spawnParticle(Particle.WITCH, center, 150, 10, 10, 10, 0.4);

                // Fumée dense
                world.spawnParticle(Particle.LARGE_SMOKE, center, 400, 18, 18, 18, 0.4);
                world.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, center, 300, 15, 15, 15, 0.3);

                // Sons apocalyptiques
                world.playSound(center, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 5.0f, 0.3f);
                world.playSound(center, Sound.ENTITY_WITHER_DEATH, 4.0f, 0.5f);
                world.playSound(center, Sound.ENTITY_ENDER_DRAGON_DEATH, 3.0f, 0.4f);
                world.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 3.0f, 0.6f);

                // Ondes de choc massives (8 vagues)
                for (int wave = 1; wave <= 8; wave++) {
                    int finalWave = wave;
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            double radius = finalWave * 8;
                            for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 32) {
                                double dx = Math.cos(angle) * radius;
                                double dz = Math.sin(angle) * radius;
                                Location shockLoc = center.clone().add(dx, 0, dz);
                                world.spawnParticle(Particle.SWEEP_ATTACK, shockLoc, 3);
                                world.spawnParticle(Particle.EXPLOSION, shockLoc, 2);
                                world.spawnParticle(Particle.FLASH, shockLoc, 1);
                                world.spawnParticle(Particle.SOUL_FIRE_FLAME, shockLoc, 5, 0.5, 0.5, 0.5, 0);
                            }
                            world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.4f);
                        }
                    }.runTaskLater(this, wave * 3);
                }

                // Colonne de fumée géante (champignon nucléaire)
                new BukkitRunnable() {
                    int tick = 0;
                    @Override
                    public void run() {
                        if (tick > 80) {
                            cancel();
                            return;
                        }

                        double height = tick * 2.5;
                        Location stemLoc = center.clone().add(0, height, 0);

                        // Tige du champignon
                        double stemRadius = 8 - (height / 30);
                        if (stemRadius < 3) stemRadius = 3;

                        for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 8) {
                            double dx = Math.cos(angle) * stemRadius;
                            double dz = Math.sin(angle) * stemRadius;
                            Location particleLoc = stemLoc.clone().add(dx, 0, dz);
                            world.spawnParticle(Particle.LARGE_SMOKE, particleLoc, 3, 1, 1, 1, 0.02);
                            world.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, particleLoc, 2, 0.5, 0.5, 0.5, 0.01);
                            world.spawnParticle(Particle.FLAME, particleLoc, 1, 0.3, 0.3, 0.3, 0);
                        }

                        // Chapeau du champignon (commence à ~100 blocs de hauteur)
                        if (height > 100) {
                            double capRadius = 15 + (height - 100) / 5;
                            if (capRadius > 30) capRadius = 30;

                            for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 12) {
                                double dx = Math.cos(angle) * capRadius;
                                double dz = Math.sin(angle) * capRadius;
                                Location capLoc = stemLoc.clone().add(dx, 5, dz);
                                world.spawnParticle(Particle.LARGE_SMOKE, capLoc, 5, 2, 2, 2, 0.05);
                                world.spawnParticle(Particle.EXPLOSION, capLoc, 1);
                                world.spawnParticle(Particle.SOUL, capLoc, 2, 1, 1, 1, 0);
                            }
                        }

                        tick++;
                    }
                }.runTaskTimer(this, 10, 2);

                // ZONE RADIOACTIVE PERSISTANTE (30 minutes)
                explosionZones.put(center.clone(), new ZoneData(System.currentTimeMillis() + ZONE_DURATION, "nuclear", radiusNuclear));

                // Effets sur les entités
                Collection<Entity> entities = world.getNearbyEntities(center, radiusNuclear, radiusNuclear, radiusNuclear);
                for (Entity entity : entities) {
                    if (entity instanceof LivingEntity le) {
                        le.damage(100);
                        if (!le.isDead()) {
                            le.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 600, 4));
                            le.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 600, 3));
                            le.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 400, 2));
                            le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 600, 2));
                        }
                    }
                }
            }

            case "antimaterial" -> {
                double radius = 50.0;
                int r = (int) radius;
                double rSquared = radius * radius;

                // ═══════════════════════════════════════
                // EFFETS ANTIMATERIAL DIMENSIONNELS
                // ═══════════════════════════════════════

                // Implosion initiale
                world.spawnParticle(Particle.REVERSE_PORTAL, center, 2000, 25, 25, 25, 10);
                world.spawnParticle(Particle.PORTAL, center, 1500, 20, 20, 20, 8);

                // Flash dimensionnel
                world.spawnParticle(Particle.FLASH, center, 150);
                world.spawnParticle(Particle.EXPLOSION, center, 400, 20, 20, 20, 0);

                // Particules mystiques
                world.spawnParticle(Particle.ENCHANT, center, 500, 20, 20, 20, 5);
                world.spawnParticle(Particle.WITCH, center, 400, 18, 18, 18, 0.8);
                world.spawnParticle(Particle.DRAGON_BREATH, center, 300, 15, 15, 15, 0.5);
                world.spawnParticle(Particle.END_ROD, center, 200, 12, 12, 12, 0.3);
                world.spawnParticle(Particle.GLOW, center, 250, 15, 15, 15, 0.4);

                // Sons dimensionnels
                world.playSound(center, Sound.ENTITY_WITHER_DEATH, 5.0f, 0.3f);
                world.playSound(center, Sound.ENTITY_ENDER_DRAGON_DEATH, 4.0f, 0.4f);
                world.playSound(center, Sound.BLOCK_PORTAL_TRIGGER, 5.0f, 0.1f);
                world.playSound(center, Sound.BLOCK_END_PORTAL_SPAWN, 3.0f, 0.5f);

                // Vortex spiralé
                new BukkitRunnable() {
                    int tick = 0;
                    double angle = 0;
                    @Override
                    public void run() {
                        if (tick > 60) {
                            cancel();
                            return;
                        }

                        for (double r = 5; r < 40; r += 3) {
                            double x = center.getX() + Math.cos(angle + r / 5) * r;
                            double z = center.getZ() + Math.sin(angle + r / 5) * r;
                            double y = center.getY() + Math.sin(tick / 5.0) * 10;

                            Location vortexLoc = new Location(world, x, y, z);
                            world.spawnParticle(Particle.PORTAL, vortexLoc, 5, 0.1, 0.1, 0.1, 1);
                            world.spawnParticle(Particle.REVERSE_PORTAL, vortexLoc, 3, 0.1, 0.1, 0.1, 0.5);
                            world.spawnParticle(Particle.WITCH, vortexLoc, 1);
                        }

                        angle += 0.3;
                        tick++;
                    }
                }.runTaskTimer(this, 5, 2);

                // CHAMPIGNON DIMENSIONNEL (comme nuclear mais en violet)
                new BukkitRunnable() {
                    int tick = 0;
                    @Override
                    public void run() {
                        if (tick > 100) {
                            cancel();
                            return;
                        }

                        double height = tick * 3;
                        Location stemLoc = center.clone().add(0, height, 0);

                        // Tige du champignon avec portails
                        double stemRadius = 10 - (height / 40);
                        if (stemRadius < 4) stemRadius = 4;

                        for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 10) {
                            double dx = Math.cos(angle) * stemRadius;
                            double dz = Math.sin(angle) * stemRadius;
                            Location particleLoc = stemLoc.clone().add(dx, 0, dz);
                            world.spawnParticle(Particle.PORTAL, particleLoc, 8, 1, 1, 1, 2);
                            world.spawnParticle(Particle.REVERSE_PORTAL, particleLoc, 5, 0.8, 0.8, 0.8, 1);
                            world.spawnParticle(Particle.WITCH, particleLoc, 2, 0.5, 0.5, 0.5, 0);
                            world.spawnParticle(Particle.ENCHANT, particleLoc, 3, 0.5, 0.5, 0.5, 1);
                        }

                        // Chapeau dimensionnel massif
                        if (height > 120) {
                            double capRadius = 20 + (height - 120) / 4;
                            if (capRadius > 45) capRadius = 45;

                            for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 16) {
                                double dx = Math.cos(angle) * capRadius;
                                double dz = Math.sin(angle) * capRadius;
                                Location capLoc = stemLoc.clone().add(dx, 8, dz);
                                world.spawnParticle(Particle.PORTAL, capLoc, 10, 2, 2, 2, 3);
                                world.spawnParticle(Particle.REVERSE_PORTAL, capLoc, 6, 1.5, 1.5, 1.5, 2);
                                world.spawnParticle(Particle.DRAGON_BREATH, capLoc, 3, 1, 1, 1, 0);
                                world.spawnParticle(Particle.END_ROD, capLoc, 2);

                                // Éclairs dimensionnels
                                if (Math.random() < 0.1) {
                                    world.spawnParticle(Particle.FLASH, capLoc, 1);
                                    world.spawnParticle(Particle.ELECTRIC_SPARK, capLoc, 5, 1, 1, 1, 0);
                                }
                            }
                        }

                        tick++;
                    }
                }.runTaskTimer(this, 8, 2);

                // Ondes de choc dimensionnelles
                for (int wave = 1; wave <= 10; wave++) {
                    int finalWave = wave;
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            double waveRadius = finalWave * 12;
                            for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 40) {
                                double dx = Math.cos(angle) * waveRadius;
                                double dz = Math.sin(angle) * waveRadius;
                                Location shockLoc = center.clone().add(dx, 0, dz);
                                world.spawnParticle(Particle.PORTAL, shockLoc, 10, 0.5, 2, 0.5, 2);
                                world.spawnParticle(Particle.REVERSE_PORTAL, shockLoc, 5, 0.3, 1, 0.3, 1);
                                world.spawnParticle(Particle.FLASH, shockLoc, 1);
                            }
                            world.playSound(center, Sound.BLOCK_PORTAL_TRIGGER, 1.5f, 0.5f);
                        }
                    }.runTaskLater(this, wave * 4);
                }

                // Destruction asynchrone optimisée
                new BukkitRunnable() {
                    int currentX = -r;

                    @Override
                    public void run() {
                        int blocksProcessed = 0;
                        final int maxBlocksPerTick = 5000;

                        for (int dy = -r; dy <= r && blocksProcessed < maxBlocksPerTick; dy++) {
                            for (int dz = -r; dz <= r && blocksProcessed < maxBlocksPerTick; dz++) {
                                if (currentX * currentX + dy * dy + dz * dz <= rSquared) {
                                    Location blockLoc = center.clone().add(currentX, dy, dz);
                                    if (blockLoc.getBlock().getType() != Material.BEDROCK &&
                                            blockLoc.getBlock().getType() != Material.AIR) {
                                        blockLoc.getBlock().setType(Material.AIR);
                                        blocksProcessed++;

                                        // Particules lors de la destruction
                                        if (Math.random() < 0.001) {
                                            world.spawnParticle(Particle.PORTAL, blockLoc, 5, 0.5, 0.5, 0.5, 0.5);
                                        }
                                    }
                                }
                            }
                        }

                        currentX++;
                        if (currentX > r) {
                            cancel();
                        }
                    }
                }.runTaskTimer(this, 0, 1);

                // Dégâts et effets aux entités
                Collection<Entity> entities = world.getNearbyEntities(center, radius, radius, radius);
                for (Entity entity : entities) {
                    if (entity instanceof LivingEntity le) {
                        le.damage(125);

                        if (!le.isDead()) {
                            le.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 200, 9));
                            le.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 200, 9));
                            le.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 5));
                        }
                    }
                }

                // Feu résiduel dimensionnel
                int fireSpots = 40;
                for (int i = 0; i < fireSpots; i++) {
                    double theta = Math.random() * 2 * Math.PI;
                    double phi = Math.acos(2 * Math.random() - 1);

                    double dxFire = radius * 0.8 * Math.sin(phi) * Math.cos(theta);
                    double dyFire = radius * 0.8 * Math.sin(phi) * Math.sin(theta);
                    double dzFire = radius * 0.8 * Math.cos(phi);

                    Location fireLoc = center.clone().add(dxFire, dyFire, dzFire);
                    world.createExplosion(fireLoc, 5.0f, true, false);
                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, fireLoc, 20, 2, 2, 2, 0.1);
                }

                // ZONE DIMENSIONNELLE PERSISTANTE (30 minutes)
                explosionZones.put(center.clone(), new ZoneData(System.currentTimeMillis() + ZONE_DURATION, "antimaterial", radius));
            }

            case "incendiary" -> {
                double fireRadius = 20.0;
                int fireRadiusInt = (int) fireRadius;

                // Effets pyrotechniques massifs
                world.spawnParticle(Particle.EXPLOSION, center, 100, 12, 12, 12, 0);
                world.spawnParticle(Particle.FLASH, center, 30);
                world.spawnParticle(Particle.FLAME, center, 800, 18, 18, 18, 0.5);
                world.spawnParticle(Particle.LAVA, center, 250, 12, 12, 12, 0);
                world.spawnParticle(Particle.DRIPPING_LAVA, center, 150, 10, 10, 10, 0);
                world.spawnParticle(Particle.LARGE_SMOKE, center, 300, 15, 15, 15, 0.3);
                world.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, center, 200, 12, 12, 12, 0.2);

                world.playSound(center, Sound.ITEM_FIRECHARGE_USE, 5.0f, 0.6f);
                world.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 3.0f, 0.5f);
                world.playSound(center, Sound.ENTITY_GHAST_SHOOT, 2.0f, 0.7f);

                // Vagues de feu
                for (int wave = 1; wave <= 6; wave++) {
                    int finalWave = wave;
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            double waveRadius = finalWave * 6;
                            for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 20) {
                                double dx = Math.cos(angle) * waveRadius;
                                double dz = Math.sin(angle) * waveRadius;
                                Location flameLoc = center.clone().add(dx, 0, dz);
                                world.spawnParticle(Particle.FLAME, flameLoc, 15, 0.5, 1, 0.5, 0.1);
                                world.spawnParticle(Particle.LAVA, flameLoc, 3);
                                world.spawnParticle(Particle.DRIPPING_LAVA, flameLoc, 5, 0.3, 0.5, 0.3, 0);
                            }
                        }
                    }.runTaskLater(this, wave * 3);
                }

                // Colonne de flammes
                new BukkitRunnable() {
                    int tick = 0;
                    @Override
                    public void run() {
                        if (tick > 50) {
                            cancel();
                            return;
                        }
                        double height = tick * 2;
                        Location flameLoc = center.clone().add(0, height, 0);
                        world.spawnParticle(Particle.FLAME, flameLoc, 15, 2, 1, 2, 0.1);
                        world.spawnParticle(Particle.LAVA, flameLoc, 3, 1, 0.5, 1, 0);
                        world.spawnParticle(Particle.SMOKE, flameLoc, 8, 1.5, 1, 1.5, 0.05);
                        tick++;
                    }
                }.runTaskTimer(this, 5, 2);

                // Feu asynchrone optimisé
                new BukkitRunnable() {
                    int currentDx = -fireRadiusInt;

                    @Override
                    public void run() {
                        int blocksProcessed = 0;
                        final int maxBlocksPerTick = 3000;

                        for (int dy = -fireRadiusInt; dy <= fireRadiusInt && blocksProcessed < maxBlocksPerTick; dy++) {
                            for (int dz = -fireRadiusInt; dz <= fireRadiusInt && blocksProcessed < maxBlocksPerTick; dz++) {
                                if (currentDx * currentDx + dy * dy + dz * dz <= fireRadius * fireRadius) {
                                    Location blockLoc = center.clone().add(currentDx, dy, dz);

                                    if (blockLoc.getBlock().getType().isAir() &&
                                            blockLoc.clone().subtract(0, 1, 0).getBlock().getType().isSolid()) {
                                        blockLoc.getBlock().setType(Material.FIRE);
                                        blocksProcessed++;
                                    }
                                }
                            }
                        }

                        currentDx++;
                        if (currentDx > fireRadiusInt) {
                            cancel();
                        }
                    }
                }.runTaskTimer(this, 0, 1);

                // ZONE INCENDIAIRE PERSISTANTE (30 minutes)
                explosionZones.put(center.clone(), new ZoneData(System.currentTimeMillis() + ZONE_DURATION, "incendiary", fireRadius));
            }

            default -> world.getPlayers().forEach(player ->
                    player.sendMessage("§cType de missile inconnu: " + missileType));
        }
    }
}