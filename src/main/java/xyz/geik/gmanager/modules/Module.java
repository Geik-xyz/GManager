package xyz.geik.gmanager.modules;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import xyz.geik.glib.shades.okaeri.configs.ConfigManager;
import xyz.geik.glib.shades.okaeri.configs.OkaeriConfig;
import xyz.geik.glib.shades.okaeri.configs.yaml.bukkit.YamlBukkitConfigurer;
import xyz.geik.glib.shades.triumphteam.cmd.core.BaseCommand;
import xyz.geik.gmanager.GManager;
import xyz.geik.gmanager.api.modules.ModuleDescription;
import xyz.geik.gmanager.api.modules.requests.ModuleRequestHandler;
import xyz.geik.gmanager.configuration.ConfigFile;

/**
 * Add-on class for BentoBox. Extend this to create an add-on. The operation
 * and methods are very similar to Bukkit's JavaPlugin.
 *
 * @author poyraz.inan
 */
@Getter
public abstract class Module {

    /**
     * Command list of module
     */
    private final List<BaseCommand> commandList = new ArrayList<>();

    @Setter
    @Getter
    private State state;

    @Setter
    private ModuleDescription description;

    @Setter
    private File dataFolder;

    @Setter
    private File file;
    private final Map<String, ModuleRequestHandler> requestHandlers = new HashMap<>();

    protected Module() {
        state = State.DISABLED;
    }

    /**
     * Executes code when enabling the addon.
     * This is called after {@link #onLoad()}.
     * <br/>
     * Note that commands and worlds registration <b>must</b> be done in {@link #onLoad()}, if need be.
     * Failure to do so <b>will</b> result in issues such as tab-completion not working for commands.
     */
    public abstract void onEnable();

    /**
     * Executes code when disabling the addon.
     */
    public abstract void onDisable();

    /**
     * Executes code when loading the addon.
     * This is called before {@link #onEnable()}.
     * This <b>must</b> be used to setup configuration, worlds and commands.
     */
    public void onLoad() {}

    /**
     * Executes code when reloading the addon.
     */
    public void onReload() {}

    public GManager getPlugin() {
        return GManager.getInstance();
    }

    /**
     * Represents the current run-time state of a {@link Module}.
     *
     * @author Poslovitch
     */
    public enum State {
        /**
         * The addon has been correctly loaded.
         */
        LOADED,

        /**
         * The addon has been correctly enabled and is now fully working.
         */
        ENABLED,

        /**
         * The addon is fully disabled.
         */
        DISABLED,

        /**
         * The addon has not been loaded because it requires a different version of BentoBox or of the server software.
         */
        INCOMPATIBLE,

        /**
         * The addon has not been enabled because a dependency is missing.
         */
        MISSING_DEPENDENCY,

        /**
         * The addon loading or enabling process has been interrupted by an unhandled error.
         */
        ERROR
    }

    /**
     * @return Logger
     */
    public Logger getLogger() {
        return getPlugin().getLogger();
    }

    /**
     * Convenience method to obtain the server
     *
     * @return the server object
     */
    public Server getServer() {
        return Bukkit.getServer();
    }

    public boolean isEnabled() {
        return state == State.ENABLED;
    }

    /**
     * Register a listener for this addon. This MUST be used in order for the addon to be reloadable
     *
     * @param listener - listener
     */
    public void registerListener(Listener listener) {
        GManager.getModuleManager().registerListener(this, listener);
    }

    /**
     * Registers command of addon
     *
     * @param command - base command class
     */
    public void registerCommand(BaseCommand command) {
        GManager.getCommandManager().registerCommand(command);
        commandList.add(command);
    }

    /**
     * Removes registered commands
     */
    public void unloadAllCommands() {
        if (!commandList.isEmpty())
            commandList.forEach(cmd -> GManager.getCommandManager().registerCommand(cmd));
    }

    /**
     * Reloads config file
     * @since release
     */
    public void reloadConfig(OkaeriConfig config) {
        if (config != null)
            config.load(true);
    }

    /**
     * Reloads lang file
     * @since release
     */
    public void reloadLang(OkaeriConfig lang) {
        if (lang != null)
            lang.load(true);
    }

    /**
     * Saves the addon's config.yml file to the addon's data folder and loads it. If
     * the file exists already, it will not be replaced.
     */
    public void saveDefaultConfig(OkaeriConfig config) {
        config = ConfigManager.create(ConfigFile.class, (it) -> {
            it.withConfigurer(new YamlBukkitConfigurer());
            it.withBindFile(new File(dataFolder, "config.yml"));
            it.saveDefaults();
            it.load(true);
        });
    }

    /**
     * Loads lang of main plugin to addon.
     * If there is no any lang class
     * then loads default lang file of addon.
     */
    public void saveLang(OkaeriConfig defaultLang, String langPath) {
        String langName = GManager.getConfigFile().getSettings().getLang();
        try {
            Class langClass = Class.forName(langPath + langName);
            Class<OkaeriConfig> languageClass = langClass;
            ConfigManager.create(languageClass, (it) -> {
                it.withConfigurer(new YamlBukkitConfigurer());
                it.withBindFile(new File(getDataFolder() + "/lang", langName + ".yml"));
                it.saveDefaults();
                it.load(true);
            });
        }
        catch (ClassNotFoundException exception) {
            this.getPlugin().logError("Couldn't find the [" + langPath + "] path for " + langName + " lang.");
            this.getPlugin().logError("Loading default..");
            Class<OkaeriConfig> langClass = (Class<OkaeriConfig>) defaultLang.getClass();
            ConfigManager.create(langClass, (it) -> {
                it.withConfigurer(new YamlBukkitConfigurer());
                it.withBindFile(new File(getDataFolder() + "/lang", langName + ".yml"));
                it.saveDefaults();
                it.load(true);
            });
        }
    }

    /**
     * Saves a resource contained in this add-on's jar file to the addon's data
     * folder.
     *
     * @param resourcePath
     *            in jar file
     * @param replace
     *            - if true, will overwrite previous file
     */
    public void saveResource(String resourcePath, boolean replace) {
        saveResource(resourcePath, dataFolder, replace, false);
    }

    /**
     * Saves a resource contained in this add-on's jar file to the destination
     * folder.
     *
     * @param jarResource
     *            in jar file
     * @param destinationFolder
     *            on file system
     * @param replace
     *            - if true, will overwrite previous file
     * @param noPath
     *            - if true, the resource's path will be ignored when saving
     * @return file written, or null if none
     */
    public File saveResource(String jarResource, File destinationFolder, boolean replace, boolean noPath) {
        if (jarResource == null || jarResource.equals("")) {
            throw new IllegalArgumentException("ResourcePath cannot be null or empty");
        }

        jarResource = jarResource.replace('\\', '/');
        try (JarFile jar = new JarFile(file)) {
            JarEntry jarConfig = jar.getJarEntry(jarResource);
            if (jarConfig != null) {
                try (InputStream in = jar.getInputStream(jarConfig)) {
                    if (in == null) {
                        throw new IllegalArgumentException(
                                "The embedded resource '" + jarResource + "' cannot be found in " + jar.getName());
                    }
                    // There are two options, use the path of the resource or not
                    File outFile = new File(destinationFolder,
                            jarResource.replaceAll("/", Matcher.quoteReplacement(File.separator)));

                    if (noPath) {
                        outFile = new File(destinationFolder, outFile.getName());
                    }
                    // Make any dirs that need to be made
                    outFile.getParentFile().mkdirs();
                    if (!outFile.exists() || replace) {
                        java.nio.file.Files.copy(in, outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                    return outFile;
                }
            } else {
                // No file in the jar
                throw new IllegalArgumentException(
                        "The embedded resource '" + jarResource + "' cannot be found in " + jar.getName());
            }
        } catch (IOException e) {
            GManager.getInstance().logError(
                    "Could not save from jar file. From " + jarResource + " to " + destinationFolder.getAbsolutePath());
        }
        return null;
    }

    /**
     * Tries to load a YAML file from the Jar
     * @param jarResource - YAML file in jar
     * @return YamlConfiguration - may be empty
     * @throws IOException - if the file cannot be found or loaded from the Jar
     * @throws InvalidConfigurationException - if the yaml is malformed
     */
    public YamlConfiguration getYamlFromJar(String jarResource) throws IOException, InvalidConfigurationException {
        if (jarResource == null || jarResource.equals("")) {
            throw new IllegalArgumentException("jarResource cannot be null or empty");
        }
        YamlConfiguration result = new YamlConfiguration();
        jarResource = jarResource.replace('\\', '/');
        try (JarFile jar = new JarFile(file)) {
            JarEntry jarConfig = jar.getJarEntry(jarResource);
            if (jarConfig != null) {
                try (InputStreamReader in = new InputStreamReader(jar.getInputStream(jarConfig))) {
                    result.load(in);
                }
            }
        }
        return result;
    }

    /**
     * Get the resource from Jar file
     * @param jarResource - jar resource filename
     * @return resource or null if there is a problem
     */
    public InputStream getResource(String jarResource) {
        if (jarResource == null || jarResource.equals("")) {
            throw new IllegalArgumentException("ResourcePath cannot be null or empty");
        }

        jarResource = jarResource.replace('\\', '/');
        try (JarFile jar = new JarFile(file)) {
            JarEntry jarConfig = jar.getJarEntry(jarResource);
            if (jarConfig != null) {
                try (InputStream in = jar.getInputStream(jarConfig)) {
                    return in;
                }
            }
        } catch (IOException e) {
            Bukkit.getLogger().severe("Could not open from jar file. " + jarResource);
        }
        return null;
    }

    /**
     * Get the Addon By Name
     * @return Optional Addon
     */
    public Optional<Module> getAddonByName(String name) {
        return GManager.getModuleManager().getAddonByName(name);
    }

    public void log(String string) {
        getPlugin().log(getDescription() != null ? "[" + getDescription().getName() + "] " + string : string);
    }

    public void logWarning(String string) {
        getPlugin().logWarning(getDescription() != null ? "[" + getDescription().getName() + "] " + string : string);
    }

    public void logError(String string) {
        getPlugin().logError(getDescription() != null ? "[" + getDescription().getName() + "] " + string : string);
    }

    /**
     * Returns the permission prefix corresponding to this addon.
     * It contains the addon's name plus a trailing dot.
     * @return Permission prefix string
     */
    public String getPermissionPrefix() {
        return this.getDescription().getName().toLowerCase(Locale.ENGLISH) + ".";
    }

    /**
     * Register request handler to answer requests from plugins.
     * @param handler request handler
     */
    public void registerRequestHandler(ModuleRequestHandler handler) {
        requestHandlers.put(handler.getLabel(), handler);
    }

    /**
     * Send request to addon.
     * @param label label
     * @param metaData meta data
     * @return request response, null if no response.
     */
    public Object request(String label, Map<String, Object> metaData) {
        label = label.toLowerCase(Locale.ENGLISH);
        ModuleRequestHandler handler = requestHandlers.get(label);
        if(handler != null) {
            return handler.handle(metaData);
        } else {
            return null;
        }
    }

    /**
     * Load YAML config file
     *
     * @return Yaml File configuration
     */
    private FileConfiguration loadYamlFile() {
        File yamlFile = new File(dataFolder, "config.yml");

        YamlConfiguration yamlConfig = null;
        if (yamlFile.exists()) {
            try {
                yamlConfig = new YamlConfiguration();
                yamlConfig.load(yamlFile);
            } catch (Exception e) {
                Bukkit.getLogger().severe(() -> "Could not load config.yml: " + e.getMessage());
            }
        }
        return yamlConfig;
    }

    /**
     * Called when all addons have been loaded by BentoBox
     * @since 1.8.0
     */
    public void allLoaded() {}
}