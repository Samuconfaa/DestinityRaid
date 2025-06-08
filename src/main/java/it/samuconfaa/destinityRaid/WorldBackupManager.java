package it.samuconfaa.destinityRaid;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class WorldBackupManager {
    private final DestinityRaid plugin;
    private final Map<String, String> activeBackups = new HashMap<>(); // worldKey -> backupPath

    public WorldBackupManager(DestinityRaid plugin) {
        this.plugin = plugin;

        // Crea la cartella per i backup se non esiste
        File backupDir = new File(plugin.getDataFolder(), "world-backups");
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }
    }

    /**
     * Crea un backup completo del mondo prima dell'inizio del raid
     */
    public boolean createWorldBackup(String worldKey) {
        ConfigurationManager.WorldInfo worldInfo = ConfigurationManager.getWorlds().get(worldKey);
        if (worldInfo == null) {
            plugin.getLogger().warning("WorldInfo non trovata per il mondo: " + worldKey);
            return false;
        }

        String worldName = worldInfo.getWorldName();
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            plugin.getLogger().warning("Mondo non trovato: " + worldName);
            return false;
        }

        // Genera un nome unico per il backup
        String backupName = worldKey + "_backup_" + System.currentTimeMillis();
        File backupDir = new File(plugin.getDataFolder(), "world-backups" + File.separator + backupName);

        try {
            // Salva il mondo prima del backup
            plugin.getLogger().info("Salvando il mondo " + worldName + " prima del backup...");
            world.save();

            // Aspetta un momento per assicurarsi che il salvataggio sia completato
            Thread.sleep(1000);

            // Crea il backup
            plugin.getLogger().info("Creando backup del mondo " + worldName + "...");
            File worldFolder = world.getWorldFolder();

            if (!worldFolder.exists()) {
                plugin.getLogger().severe("Cartella del mondo non trovata: " + worldFolder.getAbsolutePath());
                return false;
            }

            // Copia ricorsivamente la cartella del mondo
            copyDirectory(worldFolder.toPath(), backupDir.toPath());

            // Memorizza il percorso del backup
            activeBackups.put(worldKey, backupDir.getAbsolutePath());

            plugin.getLogger().info("Backup creato con successo per il mondo " + worldName +
                    " in: " + backupDir.getAbsolutePath());
            return true;

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Errore durante la creazione del backup per il mondo " + worldName, e);

            // Pulisci il backup parziale in caso di errore
            if (backupDir.exists()) {
                deleteDirectory(backupDir);
            }
            activeBackups.remove(worldKey);

            return false;
        }
    }

    /**
     * Ripristina il mondo dal backup dopo la fine del raid
     */
    public boolean restoreWorldFromBackup(String worldKey) {
        String backupPath = activeBackups.get(worldKey);
        if (backupPath == null) {
            plugin.getLogger().warning("Nessun backup trovato per il mondo: " + worldKey);
            return false;
        }

        ConfigurationManager.WorldInfo worldInfo = ConfigurationManager.getWorlds().get(worldKey);
        if (worldInfo == null) {
            plugin.getLogger().warning("WorldInfo non trovata per il mondo: " + worldKey);
            return false;
        }

        String worldName = worldInfo.getWorldName();
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            plugin.getLogger().warning("Mondo non trovato durante il ripristino: " + worldName);
            return false;
        }

        File backupDir = new File(backupPath);
        if (!backupDir.exists()) {
            plugin.getLogger().severe("Directory di backup non trovata: " + backupPath);
            return false;
        }

        try {
            plugin.getLogger().info("Iniziando il ripristino del mondo " + worldName + "...");

            // FASE 1: Preparazione (thread principale)
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    // Salva il mondo corrente
                    world.save();

                    // Teletrasporta tutti i giocatori nel mondo hub
                    teleportAllPlayersToHub(world);

                    // FASE 2: Aspetta e poi procedi con unload/restore (thread principale)
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        try {
                            // Unload del mondo (DEVE essere nel thread principale)
                            plugin.getLogger().info("Scaricando il mondo " + worldName + "...");
                            if (!Bukkit.unloadWorld(world, false)) {
                                plugin.getLogger().warning("Impossibile scaricare il mondo " + worldName);
                                return;
                            }

                            // FASE 3: Operazioni su file system (thread asincrono)
                            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                                try {
                                    // Elimina la cartella del mondo corrente
                                    File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
                                    if (worldFolder.exists()) {
                                        plugin.getLogger().info("Eliminando la cartella del mondo corrente...");
                                        deleteDirectory(worldFolder);
                                    }

                                    // Ripristina dal backup
                                    plugin.getLogger().info("Ripristinando dal backup...");
                                    copyDirectory(backupDir.toPath(), worldFolder.toPath());

                                    // FASE 4: Ricarica mondo (thread principale)
                                    Bukkit.getScheduler().runTask(plugin, () -> {
                                        try {
                                            // Ricarica il mondo (DEVE essere nel thread principale)
                                            plugin.getLogger().info("Ricaricando il mondo " + worldName + "...");
                                            Bukkit.createWorld(new org.bukkit.WorldCreator(worldName));

                                            // Pulisci il backup
                                            deleteDirectory(backupDir);
                                            activeBackups.remove(worldKey);

                                            plugin.getLogger().info("Mondo " + worldName + " ripristinato con successo!");

                                        } catch (Exception e) {
                                            plugin.getLogger().log(Level.SEVERE, "Errore durante il ricaricamento del mondo " + worldName, e);
                                        }
                                    });

                                } catch (Exception e) {
                                    plugin.getLogger().log(Level.SEVERE, "Errore durante le operazioni su file system per " + worldName, e);
                                }
                            });

                        } catch (Exception e) {
                            plugin.getLogger().log(Level.SEVERE, "Errore durante l'unload del mondo " + worldName, e);
                        }
                    }, 40L); // Aspetta 2 secondi (40 tick) prima di procedere

                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Errore durante la preparazione del ripristino per " + worldName, e);
                }
            });

            return true; // Restituisce true perché il processo è iniziato con successo

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Errore durante il ripristino del mondo " + worldName, e);
            return false;
        }
    }

    /**
     * Pulisce i backup quando un raid viene interrotto
     */
    public void cleanupBackup(String worldKey) {
        String backupPath = activeBackups.get(worldKey);
        if (backupPath != null) {
            File backupDir = new File(backupPath);
            if (backupDir.exists()) {
                deleteDirectory(backupDir);
                plugin.getLogger().info("Backup eliminato per il mondo: " + worldKey);
            }
            activeBackups.remove(worldKey);
        }
    }

    /**
     * Teletrasporta tutti i giocatori nel mondo al mondo hub
     */
    private void teleportAllPlayersToHub(World world) {
        String hubWorldName = ConfigurationManager.getHubWorldName();
        World hubWorld = Bukkit.getWorld(hubWorldName);

        if (hubWorld == null) {
            plugin.getLogger().severe("Mondo hub non trovato: " + hubWorldName);
            return;
        }

        for (Player player : world.getPlayers()) {
            player.teleport(hubWorld.getSpawnLocation());
            player.sendMessage(ChatColor.YELLOW + "Sei stato teletrasportato al mondo hub per il ripristino del mondo raid.");
        }
    }

    /**
     * Copia ricorsivamente una directory
     */
    private void copyDirectory(Path source, Path target) throws IOException {
        Files.walk(source).forEach(sourcePath -> {
            try {
                Path targetPath = target.resolve(source.relativize(sourcePath));
                if (Files.isDirectory(sourcePath)) {
                    Files.createDirectories(targetPath);
                } else {
                    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Errore durante la copia di " + sourcePath, e);
            }
        });
    }

    /**
     * Elimina ricorsivamente una directory
     */
    private void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }

    /**
     * Ottieni la lista dei backup attivi
     */
    public Map<String, String> getActiveBackups() {
        return new HashMap<>(activeBackups);
    }

    /**
     * Controlla se esiste un backup per il mondo specificato
     */
    public boolean hasBackup(String worldKey) {
        return activeBackups.containsKey(worldKey);
    }
}