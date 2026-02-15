package fr.volcastaff;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.ConsoleCommandSender;
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
        getLogger().info("[VOLCALYPSE] v1.0.7 - STABLE - Console Support + Tab Completion");
        getServer().getPluginManager().registerEvents(this, this);

        interceptorKey = new NamespacedKey(this, "interceptor_level");
        placerKey = new NamespacedKey(this, "placer_uuid");

        // Enregistrement des commandes
        MissileCommand missileCmd = new MissileCommand();
        getCommand("missile").setExecutor(missileCmd);
        getCommand("missile").setTabCompleter(missileCmd);
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
                                    Particle.SOUL.builder().location(pLoc).count(2).offset(0.2, 0.2, 0.2).extra(0).allPlayers().spawn();
                                    Particle.WITCH.builder().location(pLoc).count(1).offset(0.1, 0.1, 0.1).extra(0).allPlayers().spawn();
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
                                    Particle.PORTAL.builder().location(pLoc).count(3).offset(0.3, 0.3, 0.3).extra(0.5).allPlayers().spawn();
                                    Particle.REVERSE_PORTAL.builder().location(pLoc).count(1).offset(0.1, 0.1, 0.1).extra(0.2).allPlayers().spawn();
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
                                    Particle.SMOKE.builder().location(pLoc).count(2).offset(0.2, 0.2, 0.2).extra(0.02).allPlayers().spawn();
                                    Particle.FLAME.builder().location(pLoc).count(1).offset(0.1, 0.1, 0.1).extra(0).allPlayers().spawn();
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
                                    Particle.CAMPFIRE_SIGNAL_SMOKE.builder().location(pLoc).count(1).offset(0.1, 0.3, 0.1).extra(0.01).allPlayers().spawn();
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
                    Particle.TOTEM_OF_UNDYING.builder().location(loc).count(50).offset(0.5, 1, 0.5).extra(0.1).allPlayers().spawn();
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
                Particle.HAPPY_VILLAGER.builder().location(loc).count(30).offset(0.5, 1, 0.5).extra(0).allPlayers().spawn();
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

    public class MissileCommand implements CommandExecutor, TabCompleter {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            // Vérifier les permissions
            if (sender instanceof Player player) {
                if (!player.isOp()) {
                    player.sendMessage("§cVous devez être opérateur pour utiliser cette commande !");
                    return true;
                }
            } else if (!(sender instanceof ConsoleCommandSender)) {
                sender.sendMessage("§cCette commande ne peut être exécutée que par un joueur ou la console !");
                return true;
            }

            if (args.length < 3) {
                sender.sendMessage("§cUsage: /missile <type> <x> <z> [monde]");
                sender.sendMessage("§cTypes disponibles: small, medium, large, nuclear, antimaterial, incendiary");
                return true;
            }

            try {
                String missileType = args[0].toLowerCase();
                int x = Integer.parseInt(args[1]);
                int z = Integer.parseInt(args[2]);
                
                // Déterminer le monde
                World targetWorld;
                if (args.length >= 4) {
                    // Monde spécifié
                    targetWorld = getServer().getWorld(args[3]);
                    if (targetWorld == null) {
                        sender.sendMessage("§cMonde introuvable: " + args[3]);
                        return true;
                    }
                } else if (sender instanceof Player player) {
                    // Joueur: utiliser son monde
                    targetWorld = player.getWorld();
                } else {
                    // Console sans monde spécifié: utiliser le monde par défaut
                    targetWorld = getServer().getWorlds().get(0);
                    sender.sendMessage("§eAucun monde spécifié, utilisation de: " + targetWorld.getName());
                }

                launchMissile(missileType, x, z, targetWorld);
                sender.sendMessage("§aMissile " + missileType + " lancé à X:" + x + " Z:" + z + " dans " + targetWorld.getName());

            } catch (NumberFormatException e) {
                sender.sendMessage("§cLes coordonnées doivent être des nombres entiers !");
                return true;
            }

            return true;
        }
        
        @Override
        public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
            List<String> completions = new ArrayList<>();
            
            if (args.length == 1) {
                // Autocomplétion des types de missiles
                List<String> missileTypes = Arrays.asList("small", "medium", "large", "nuclear", "antimaterial", "incendiary");
                String partial = args[0].toLowerCase();
                for (String type : missileTypes) {
                    if (type.startsWith(partial)) {
                        completions.add(type);
                    }
                }
            } else if (args.length == 2 || args.length == 3) {
                // Pour X et Z, suggérer la position du joueur si c'est un joueur
                if (sender instanceof Player player) {
                    if (args.length == 2) {
                        completions.add(String.valueOf((int) player.getLocation().getX()));
                    } else {
                        completions.add(String.valueOf((int) player.getLocation().getZ()));
                    }
                }
            } else if (args.length == 4) {
                // Autocomplétion des mondes
                String partial = args[3].toLowerCase();
                for (World world : getServer().getWorlds()) {
                    String worldName = world.getName();
                    if (worldName.toLowerCase().startsWith(partial)) {
                        completions.add(worldName);
                    }
                }
            }
            
            return completions;
        }
    }

    public void launchMissile(String missileType, int x, int z, World world) {
        int startY = 300;
        Location startLoc = new Location(world, x + 0.5, startY, z + 0.5);

        TNTPrimed missile = (TNTPrimed) world.spawnEntity(startLoc, EntityType.TNT);
        missile.setFuseTicks(10000);
        missile.setGravity(false);

        Particle.EXPLOSION.builder().location(startLoc).count(10).offset(1.0, 1.0, 1.0).extra(0.0).allPlayers().spawn();
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
                        Particle.FLAME.builder().location(loc).count(20).offset(0.3, 0.3, 0.3).extra(0.05).allPlayers().spawn();
                        Particle.SOUL_FIRE_FLAME.builder().location(loc).count(10).offset(0.2, 0.2, 0.2).extra(0.02).allPlayers().spawn();
                        Particle.ELECTRIC_SPARK.builder().location(loc).count(15).offset(0.4, 0.4, 0.4).extra(0.1).allPlayers().spawn();
                        Particle.GLOW.builder().location(loc).count(5).offset(0.3, 0.3, 0.3).extra(0).allPlayers().spawn();
                        if (currentTick % 10 == 0) {
                            world.playSound(loc, Sound.BLOCK_BEACON_AMBIENT, 0.5f, 2.0f);
                        }
                    }
                    case "antimaterial" -> {
                        Particle.PORTAL.builder().location(loc).count(30).offset(0.5, 0.5, 0.5).extra(1).allPlayers().spawn();
                        Particle.REVERSE_PORTAL.builder().location(loc).count(20).offset(0.3, 0.3, 0.3).extra(0.5).allPlayers().spawn();
                        Particle.ENCHANT.builder().location(loc).count(10).offset(0.4, 0.4, 0.4).extra(2).allPlayers().spawn();
                        Particle.WITCH.builder().location(loc).count(5).offset(0.2, 0.2, 0.2).extra(0).allPlayers().spawn();
                    }
                    case "incendiary" -> {
                        Particle.FLAME.builder().location(loc).count(25).offset(0.4, 0.4, 0.4).extra(0.1).allPlayers().spawn();
                        Particle.LAVA.builder().location(loc).count(8).offset(0.3, 0.3, 0.3).extra(0).allPlayers().spawn();
                        Particle.DRIPPING_LAVA.builder().location(loc).count(15).offset(0.3, 0.3, 0.3).extra(0).allPlayers().spawn();
                        Particle.SMOKE.builder().location(loc).count(10).offset(0.2, 0.2, 0.2).extra(0.05).allPlayers().spawn();
                    }
                    default -> {
                        Particle.FLAME.builder().location(loc).count(15).offset(0.2, 0.2, 0.2).extra(0.1).allPlayers().spawn();
                        Particle.CAMPFIRE_SIGNAL_SMOKE.builder().location(loc).count(8).offset(0.15, 0.15, 0.15).extra(0.05).allPlayers().spawn();
                        Particle.FIREWORK.builder().location(loc).count(3).offset(0.1, 0.1, 0.1).extra(0.05).allPlayers().spawn();
                    }
                }

                // Particules de traînée en spirale - SANS Particle.DUST
                for (int i = 0; i < 3; i++) {
                    double radius = 0.8;
                    double offsetX = Math.cos(spiralAngle + i * Math.PI * 2 / 3) * radius;
                    double offsetZ = Math.sin(spiralAngle + i * Math.PI * 2 / 3) * radius;
                    Location spiralLoc = loc.clone().add(offsetX, 0, offsetZ);
                    // Remplacé DUST par FLAME pour éviter les problèmes
                    Particle.FLAME.builder().location(spiralLoc).count(1).offset(0.0, 0.0, 0.0).extra(0.0).allPlayers().spawn();
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
                                Particle.ELECTRIC_SPARK.builder().location(pLoc).count(3).offset(0.1, 0.1, 0.1).extra(0).allPlayers().spawn();
                                Particle.END_ROD.builder().location(pLoc).count(1).offset(0.0, 0.0, 0.0).extra(0.0).allPlayers().spawn();
                            }

                            world.playSound(interceptorLoc, Sound.ENTITY_WARDEN_SONIC_BOOM, 2.0f, 1.0f);
                            world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1.5f);

                            Particle.EXPLOSION.builder().location(loc).count(20).offset(1.0, 1.0, 1.0).extra(0.0).allPlayers().spawn();
                            Particle.FIREWORK.builder().location(loc).count(50).offset(1, 1, 1).extra(0.2).allPlayers().spawn();

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
                Particle.EXPLOSION.builder().location(center).count(30).offset(2.0, 2.0, 2.0).extra(0.0).allPlayers().spawn();
                Particle.FLAME.builder().location(center).count(50).offset(2.0, 2.0, 2.0).extra(0.05).allPlayers().spawn();
                Particle.SMOKE.builder().location(center).count(40).offset(2.0, 2.0, 2.0).extra(0.1).allPlayers().spawn();
                world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1.0f);
            }

            case "medium" -> {
                world.createExplosion(center, 20.0f, true, true);

                // Effets moyens avec ondes de choc
                Particle.EXPLOSION.builder().location(center).count(80).offset(5.0, 5.0, 5.0).extra(0.0).allPlayers().spawn();
                Particle.FLAME.builder().location(center).count(150).offset(4.0, 4.0, 4.0).extra(0.15).allPlayers().spawn();
                Particle.LARGE_SMOKE.builder().location(center).count(100).offset(5.0, 5.0, 5.0).extra(0.2).allPlayers().spawn();
                Particle.FIREWORK.builder().location(center).count(60).offset(5.0, 5.0, 5.0).extra(0.3).allPlayers().spawn();

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
                                Particle.SWEEP_ATTACK.builder().location(shockLoc).count(1).allPlayers().spawn();
                                Particle.CLOUD.builder().location(shockLoc).count(3).offset(0.2, 0.5, 0.2).extra(0).allPlayers().spawn();
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
                Particle.EXPLOSION.builder().location(center).count(150).offset(10.0, 10.0, 10.0).extra(0.0).allPlayers().spawn();
                Particle.FLAME.builder().location(center).count(300).offset(8.0, 8.0, 8.0).extra(0.25).allPlayers().spawn();
                Particle.LARGE_SMOKE.builder().location(center).count(200).offset(10.0, 10.0, 10.0).extra(0.3).allPlayers().spawn();
                Particle.CLOUD.builder().location(center).count(180).offset(10.0, 10.0, 10.0).extra(0.4).allPlayers().spawn();
                Particle.LAVA.builder().location(center).count(40).offset(6.0, 6.0, 6.0).extra(0.0).allPlayers().spawn();

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
                                Particle.SWEEP_ATTACK.builder().location(shockLoc).count(2).allPlayers().spawn();
                                Particle.EXPLOSION.builder().location(shockLoc).count(1).allPlayers().spawn();
                                Particle.CLOUD.builder().location(shockLoc).count(5).offset(0.3, 0.8, 0.3).extra(0).allPlayers().spawn();
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
                        Particle.LARGE_SMOKE.builder().location(smokeLoc).count(10).offset(3, 1, 3).extra(0.05).allPlayers().spawn();
                        Particle.CAMPFIRE_SIGNAL_SMOKE.builder().location(smokeLoc).count(5).offset(2, 1, 2).extra(0.02).allPlayers().spawn();
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
                Particle.EXPLOSION.builder().location(center).count(500).offset(20.0, 20.0, 20.0).extra(0.0).allPlayers().spawn();

                // Boule de feu centrale
                Particle.FLAME.builder().location(center).count(800).offset(15.0, 15.0, 15.0).extra(0.8).allPlayers().spawn();
                Particle.SOUL_FIRE_FLAME.builder().location(center).count(400).offset(12.0, 12.0, 12.0).extra(0.5).allPlayers().spawn();
                Particle.LAVA.builder().location(center).count(150).offset(10.0, 10.0, 10.0).extra(0.0).allPlayers().spawn();

                // Particules radioactives vertes
                Particle.GLOW.builder().location(center).count(300).offset(15.0, 15.0, 15.0).extra(0.5).allPlayers().spawn();
                Particle.ELECTRIC_SPARK.builder().location(center).count(250).offset(15.0, 15.0, 15.0).extra(0.6).allPlayers().spawn();
                Particle.SOUL.builder().location(center).count(200).offset(12.0, 12.0, 12.0).extra(0.3).allPlayers().spawn();
                Particle.WITCH.builder().location(center).count(150).offset(10.0, 10.0, 10.0).extra(0.4).allPlayers().spawn();

                // Fumée dense
                Particle.LARGE_SMOKE.builder().location(center).count(400).offset(18.0, 18.0, 18.0).extra(0.4).allPlayers().spawn();
                Particle.CAMPFIRE_SIGNAL_SMOKE.builder().location(center).count(300).offset(15.0, 15.0, 15.0).extra(0.3).allPlayers().spawn();

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
                                Particle.SWEEP_ATTACK.builder().location(shockLoc).count(3).allPlayers().spawn();
                                Particle.EXPLOSION.builder().location(shockLoc).count(2).allPlayers().spawn();
                                Particle.SOUL_FIRE_FLAME.builder().location(shockLoc).count(5).offset(0.5, 0.5, 0.5).extra(0).allPlayers().spawn();
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
                            Particle.LARGE_SMOKE.builder().location(particleLoc).count(3).offset(1, 1, 1).extra(0.02).allPlayers().spawn();
                            Particle.CAMPFIRE_SIGNAL_SMOKE.builder().location(particleLoc).count(2).offset(0.5, 0.5, 0.5).extra(0.01).allPlayers().spawn();
                            Particle.FLAME.builder().location(particleLoc).count(1).offset(0.3, 0.3, 0.3).extra(0).allPlayers().spawn();
                        }

                        // Chapeau du champignon (commence à ~100 blocs de hauteur)
                        if (height > 100) {
                            double capRadius = 15 + (height - 100) / 5;
                            if (capRadius > 30) capRadius = 30;

                            for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 12) {
                                double dx = Math.cos(angle) * capRadius;
                                double dz = Math.sin(angle) * capRadius;
                                Location capLoc = stemLoc.clone().add(dx, 5, dz);
                                Particle.LARGE_SMOKE.builder().location(capLoc).count(5).offset(2, 2, 2).extra(0.05).allPlayers().spawn();
                                Particle.EXPLOSION.builder().location(capLoc).count(1).allPlayers().spawn();
                                Particle.SOUL.builder().location(capLoc).count(2).offset(1.0, 1.0, 1.0).extra(0.0).allPlayers().spawn();
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
                Particle.REVERSE_PORTAL.builder().location(center).count(2000).offset(25.0, 25.0, 25.0).extra(10.0).allPlayers().spawn();
                Particle.PORTAL.builder().location(center).count(1500).offset(20.0, 20.0, 20.0).extra(8.0).allPlayers().spawn();

                // Flash dimensionnel
                Particle.EXPLOSION.builder().location(center).count(400).offset(20.0, 20.0, 20.0).extra(0.0).allPlayers().spawn();

                // Particules mystiques
                Particle.ENCHANT.builder().location(center).count(500).offset(20.0, 20.0, 20.0).extra(5.0).allPlayers().spawn();
                Particle.WITCH.builder().location(center).count(400).offset(18.0, 18.0, 18.0).extra(0.8).allPlayers().spawn();
                Particle.END_ROD.builder().location(center).count(200).offset(12.0, 12.0, 12.0).extra(0.3).allPlayers().spawn();
                Particle.GLOW.builder().location(center).count(250).offset(15.0, 15.0, 15.0).extra(0.4).allPlayers().spawn();

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
                            Particle.PORTAL.builder().location(vortexLoc).count(5).offset(0.1, 0.1, 0.1).extra(1).allPlayers().spawn();
                            Particle.REVERSE_PORTAL.builder().location(vortexLoc).count(3).offset(0.1, 0.1, 0.1).extra(0.5).allPlayers().spawn();
                            Particle.WITCH.builder().location(vortexLoc).count(1).allPlayers().spawn();
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
                            Particle.PORTAL.builder().location(particleLoc).count(8).offset(1.0, 1.0, 1.0).extra(2.0).allPlayers().spawn();
                            Particle.REVERSE_PORTAL.builder().location(particleLoc).count(5).offset(0.8, 0.8, 0.8).extra(1).allPlayers().spawn();
                            Particle.WITCH.builder().location(particleLoc).count(2).offset(0.5, 0.5, 0.5).extra(0).allPlayers().spawn();
                            Particle.ENCHANT.builder().location(particleLoc).count(3).offset(0.5, 0.5, 0.5).extra(1).allPlayers().spawn();
                        }

                        // Chapeau dimensionnel massif
                        if (height > 120) {
                            double capRadius = 20 + (height - 120) / 4;
                            if (capRadius > 45) capRadius = 45;

                            for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 16) {
                                double dx = Math.cos(angle) * capRadius;
                                double dz = Math.sin(angle) * capRadius;
                                Location capLoc = stemLoc.clone().add(dx, 8, dz);
                                Particle.PORTAL.builder().location(capLoc).count(10).offset(2.0, 2.0, 2.0).extra(3.0).allPlayers().spawn();
                                Particle.REVERSE_PORTAL.builder().location(capLoc).count(6).offset(1.5, 1.5, 1.5).extra(2).allPlayers().spawn();
                                Particle.END_ROD.builder().location(capLoc).count(2).allPlayers().spawn();

                                // Éclairs dimensionnels
                                if (Math.random() < 0.1) {
                                    Particle.ELECTRIC_SPARK.builder().location(capLoc).count(5).offset(1.0, 1.0, 1.0).extra(0.0).allPlayers().spawn();
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
                                Particle.PORTAL.builder().location(shockLoc).count(10).offset(0.5, 2, 0.5).extra(2).allPlayers().spawn();
                                Particle.REVERSE_PORTAL.builder().location(shockLoc).count(5).offset(0.3, 1, 0.3).extra(1).allPlayers().spawn();
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
                                            Particle.PORTAL.builder().location(blockLoc).count(5).offset(0.5, 0.5, 0.5).extra(0.5).allPlayers().spawn();
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
                    Particle.SOUL_FIRE_FLAME.builder().location(fireLoc).count(20).offset(2, 2, 2).extra(0.1).allPlayers().spawn();
                }

                // ZONE DIMENSIONNELLE PERSISTANTE (30 minutes)
                explosionZones.put(center.clone(), new ZoneData(System.currentTimeMillis() + ZONE_DURATION, "antimaterial", radius));
            }

            case "incendiary" -> {
                double fireRadius = 20.0;
                int fireRadiusInt = (int) fireRadius;

                // Effets pyrotechniques massifs
                Particle.EXPLOSION.builder().location(center).count(100).offset(12.0, 12.0, 12.0).extra(0.0).allPlayers().spawn();
                Particle.FLAME.builder().location(center).count(800).offset(18.0, 18.0, 18.0).extra(0.5).allPlayers().spawn();
                Particle.LAVA.builder().location(center).count(250).offset(12.0, 12.0, 12.0).extra(0.0).allPlayers().spawn();
                Particle.DRIPPING_LAVA.builder().location(center).count(150).offset(10.0, 10.0, 10.0).extra(0.0).allPlayers().spawn();
                Particle.LARGE_SMOKE.builder().location(center).count(300).offset(15.0, 15.0, 15.0).extra(0.3).allPlayers().spawn();
                Particle.CAMPFIRE_SIGNAL_SMOKE.builder().location(center).count(200).offset(12.0, 12.0, 12.0).extra(0.2).allPlayers().spawn();

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
                                Particle.FLAME.builder().location(flameLoc).count(15).offset(0.5, 1, 0.5).extra(0.1).allPlayers().spawn();
                                Particle.LAVA.builder().location(flameLoc).count(3).allPlayers().spawn();
                                Particle.DRIPPING_LAVA.builder().location(flameLoc).count(5).offset(0.3, 0.5, 0.3).extra(0).allPlayers().spawn();
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
                        Particle.FLAME.builder().location(flameLoc).count(15).offset(2, 1, 2).extra(0.1).allPlayers().spawn();
                        Particle.LAVA.builder().location(flameLoc).count(3).offset(1, 0.5, 1).extra(0).allPlayers().spawn();
                        Particle.SMOKE.builder().location(flameLoc).count(8).offset(1.5, 1, 1.5).extra(0.05).allPlayers().spawn();
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