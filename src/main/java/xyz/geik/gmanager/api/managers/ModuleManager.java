package xyz.geik.gmanager.api.managers;

import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.permissions.DefaultPermissions;
import xyz.geik.gmanager.GManager;
import xyz.geik.gmanager.api.modules.ModuleClassLoader;
import xyz.geik.gmanager.api.modules.Pladdon;
import xyz.geik.gmanager.api.modules.exceptions.InvalidModuleDescriptionException;
import xyz.geik.gmanager.api.modules.exceptions.InvalidModuleFormatException;
import xyz.geik.gmanager.api.modules.exceptions.InvalidModuleInheritException;
import xyz.geik.gmanager.modules.Module;
import xyz.geik.gmanager.modules.events.ModuleEvent;

import javax.annotation.Nullable;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Module Manager Class
 */
public class ModuleManager {
    private static final String DEFAULT = ".default";
    @NonNull
    private final List<Module> modules;
    @NonNull
    private final Map<@NonNull Module, ModuleClassLoader> loaders;
    @NonNull
    private final Map<@NonNull Module, Plugin> pladdons;
    @NonNull
    private final Map<String, Class<?>> classes;
    private final GManager plugin;
    @NonNull
    private final Map<@NonNull Module, @NonNull List<Listener>> listeners;

    private final PluginLoader pluginLoader;

    public ModuleManager(@NonNull GManager plugin) {
        this.plugin = plugin;
        modules = new ArrayList<>();
        loaders = new HashMap<>();
        pladdons = new HashMap<>();
        classes = new HashMap<>();
        listeners = new HashMap<>();
        pluginLoader = plugin.getPluginLoader();
    }

    /**
     * Register a plugin as an addon
     * @param parent - parent plugin
     * @param module - addon class
     */
    public void registerAddon(Plugin parent, Module module) {
        plugin.log("Registering " + parent.getDescription().getName());

        // Get description in the addon.yml file
        InputStream resource = parent.getResource("addon.yml");
        if (resource == null) {
            plugin.logError("Failed to register addon: no addon.yml found");
            return;
        }
        // Open a reader to the jar
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource))) {
            setAddonFile(parent, module);
            // Grab the description in the addon.yml file
            YamlConfiguration data = new YamlConfiguration();
            data.load(reader);
            // Description
            module.setDescription(ModuleClassLoader.asDescription(data));
            // Set various files
            module.setDataFolder(parent.getDataFolder());
            // Initialize
            initializeAddon(module);
            sortAddons();

        } catch (Exception e) {
            plugin.logError("Failed to register addon: " + e);
        }

    }

    private void setAddonFile(Plugin parent, Module module) throws NoSuchMethodException, SecurityException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Method getFileMethod = JavaPlugin.class.getDeclaredMethod("getFile");
        getFileMethod.setAccessible(true);
        module.setFile((File) getFileMethod.invoke(parent));
    }

    /**
     * Loads all the addons from the addons folder
     */
    public void loadAddons() {
        plugin.log("Loading addons...");
        File f = new File(plugin.getDataFolder(), "addons");
        if (!f.exists() && !f.mkdirs()) {
            plugin.logError("Cannot create addons folder!");
            return;
        }
        Arrays.stream(Objects.requireNonNull(f.listFiles()))
                .filter(x -> !x.isDirectory() && x.getName().endsWith(".jar")).forEach(this::loadAddon);
        plugin.log("Loaded " + getLoadedAddons().size() + " addons.");

        if (!getLoadedAddons().isEmpty()) {
            sortAddons();
        }
    }

    private record PladdonData(Module module, boolean success) {
    }

    private void loadAddon(@NonNull File f) {
        PladdonData result = new PladdonData(null, false);
        try (JarFile jar = new JarFile(f)) {
            // try loading the addon
            // Get description in the addon.yml file
            YamlConfiguration data = addonDescription(jar);
            // Check if the addon is already loaded (duplicate version?)
            String main = data.getString("main");
            if (main != null && this.getAddonByMainClassName(main).isPresent()) {
                getAddonByMainClassName(main).ifPresent(a -> {
                    plugin.logError("Duplicate addon! Addon " + a.getDescription().getName() + " "
                            + a.getDescription().getVersion() + " has already been loaded!");
                    plugin.logError("Remove the duplicate and restart!");
                });
                return;
            }
            // Load the pladdon or addon if it isn't a pladdon
            result = loadPladdon(data, f);
        } catch (Exception e) {
            // We couldn't load the addon, aborting.
            plugin.logError("Could not load addon '" + f.getName() + "'. Error is: " + e.getMessage());
            plugin.logStacktrace(e);
            return;
        }
        // Success
        if (result.success) {
            // Initialize some settings
            result.module.setDataFolder(new File(f.getParent(), result.module.getDescription().getName()));
            result.module.setFile(f);
            // Initialize addon
            initializeAddon(result.module);
        }
    }

    private PladdonData loadPladdon(YamlConfiguration data, @NonNull File f) throws InvalidModuleInheritException,
            MalformedURLException, InvalidModuleDescriptionException, InstantiationException, IllegalAccessException,
            InvocationTargetException, NoSuchMethodException, InvalidDescriptionException {
        Module module;
        try {
            Plugin pladdon = Bukkit.getPluginManager().loadPlugin(f);
            if (pladdon != null && pladdon instanceof Pladdon pl) {
                module = pl.getAddon();
                module.setDescription(ModuleClassLoader.asDescription(data));
                // Mark pladdon as enabled.
                pl.setEnabled();
                pladdons.put(module, pladdon);
            } else {
                // Try to load it as an addon
                GManager.getInstance()
                        .log("Failed to load " + f.getName() + ", trying to load it as a GManager addon");
                // Addon not pladdon
                ModuleClassLoader moduleClassLoader = new ModuleClassLoader(this, data, f,
                        this.getClass().getClassLoader());
                // Get the addon itself
                module = moduleClassLoader.getModule();
                // Add to the list of loaders
                loaders.put(module, moduleClassLoader);
            }
        } catch (Exception ex) {
            // Addon not pladdon
            ModuleClassLoader moduleClassLoader = new ModuleClassLoader(this, data, f, this.getClass().getClassLoader());
            // Get the addon itself
            module = moduleClassLoader.getModule();
            // Add to the list of loaders
            loaders.put(module, moduleClassLoader);
        }
        return new PladdonData(module, true);
    }

    private void initializeAddon(Module module) {
        // Locales
       // plugin.getLocalesManager().copyLocalesFromAddonJar(addon);
       // plugin.getLocalesManager().loadLocalesFromFile(addon.getDescription().getName());

        // Fire the load event
        new ModuleEvent().builder().addon(module).reason(ModuleEvent.Reason.LOAD).build();

        // Add it to the list of addons
        modules.remove(module);
        modules.add(module);
        // Checks if this addon is compatible with the current GManager version.
        if (!isAddonCompatibleWithGManager(module)) {
            // It is not, abort.
            plugin.logError("Cannot load " + module.getDescription().getName() + " because it requires GManager version "
                    + module.getDescription().getApiVersion() + " or greater.");
            plugin.logError("NOTE: Please update GManager.");
            module.setState(Module.State.INCOMPATIBLE);
            return;
        }

        try {
            module.setState(Module.State.LOADED);
            // Run the onLoad.
            module.onLoad();
        } catch (NoClassDefFoundError | NoSuchMethodError | NoSuchFieldError e) {
            // Looks like the addon is incompatible, because it tries to refer to missing classes...
            handleAddonIncompatibility(module, e);
        } catch (Exception e) {
            // Unhandled exception. We'll give a bit of debug here.
            handleAddonError(module, e);
        }

    }

    /**
     * Enables all the addons
     */
    public void enableAddons() {
        if (getLoadedAddons().isEmpty())
            return;
        // Enable GameModes first, then other addons
        getLoadedAddons().stream().filter(a -> !a.getState().equals(Module.State.DISABLED)).forEach(this::enableAddon);
        plugin.log("Enabling addons...");
        getLoadedAddons().stream().filter(a -> !a.getState().equals(Module.State.DISABLED)).forEach(this::enableAddon);
        // Set perms for enabled addons
        this.getEnabledAddons().forEach(this::setPerms);
        plugin.log("Addons successfully enabled.");
    }

    boolean setPerms(Module module) {
        ConfigurationSection perms = module.getDescription().getPermissions();
        if (perms == null)
            return false;
        for (String perm : perms.getKeys(true)) {
            // Only try to register perms for end nodes
            if (perms.contains(perm + DEFAULT) && perms.contains(perm + ".description")) {
                try {
                    registerPermission(perms, perm);
                } catch (InvalidModuleDescriptionException e) {
                    plugin.logError("Addon " + module.getDescription().getName() + ": " + e.getMessage());
                }
            }
        }
        return true;
    }

    void registerPermission(ConfigurationSection perms, String perm) throws InvalidModuleDescriptionException {
        String name = perms.getString(perm + DEFAULT);
        if (name == null) {
            throw new InvalidModuleDescriptionException("Permission default is invalid in addon.yml: " + perm + DEFAULT);
        }
        PermissionDefault pd = PermissionDefault.getByName(name);
        if (pd == null) {
            throw new InvalidModuleDescriptionException("Permission default is invalid in addon.yml: " + perm + DEFAULT);
        }
        String desc = perms.getString(perm + ".description");
        // Replace placeholders for Game Mode Addon names
        DefaultPermissions.registerPermission(perm, desc, pd);
    }

    /**
     * Enables an addon
     * @param module addon
     */
    private void enableAddon(Module module) {
        plugin.log(
                "Enabling " + module.getDescription().getName() + " (" + module.getDescription().getVersion() + ")...");
        try {
            module.onEnable();
            if (module.getState().equals(Module.State.DISABLED)) {
                plugin.log(module.getDescription().getName() + " is disabled.");
                return;
            }
            new ModuleEvent().builder().addon(module).reason(ModuleEvent.Reason.ENABLE).build();
            module.setState(Module.State.ENABLED);
        } catch (NoClassDefFoundError | NoSuchMethodError | NoSuchFieldError e) {
            // Looks like the addon is incompatible, because it tries to refer to missing classes...
            handleAddonIncompatibility(module, e);
        } catch (Exception e) {
            // Unhandled exception. We'll give a bit of debug here.
            handleAddonError(module, e);
        }
    }

    /**
     * Handles an addon which failed to load due to an incompatibility (missing class, missing method).
     * @param module instance of the Addon.
     * @param e - linkage exception
     * @since 1.1
     */
    private void handleAddonIncompatibility(@NonNull Module module, LinkageError e) {
        // Set the AddonState as "INCOMPATIBLE".
        module.setState(Module.State.INCOMPATIBLE);
        plugin.logWarning("Skipping " + module.getDescription().getName()
                + " as it is incompatible with the current version of GManager or of server software...");
        plugin.logWarning("NOTE: The addon is referring to no longer existing classes.");
        plugin.logWarning("NOTE: DO NOT report this as a bug from GManager.");
        StringBuilder a = new StringBuilder();
        module.getDescription().getAuthors().forEach(author -> a.append(author).append(" "));
        plugin.logError("Please report this stack trace to the addon's author(s): " + a);
        plugin.logStacktrace(e);
    }

    private boolean isAddonCompatibleWithGManager(@NonNull Module module) {
        return true;
    }

    /**
     * Handles an addon which failed to load due to an error.
     * @param module instance of the Addon.
     * @param throwable Throwable that was thrown and which led to the error.
     * @since 1.1
     */
    private void handleAddonError(@NonNull Module module, @NonNull Throwable throwable) {
        // Set the AddonState as "ERROR".
        module.setState(Module.State.ERROR);
        plugin.logError("Skipping " + module.getDescription().getName() + " due to an unhandled exception...");
        // Send stacktrace, required for addon development
        plugin.logStacktrace(throwable);
    }

    /**
     * Reloads all the enabled addons
     */
    public void reloadAddons() {
        disableAddons();
        loadAddons();
        enableAddons();
    }

    /**
     * Disable all the enabled addons
     */
    public void disableAddons() {
        if (!getEnabledAddons().isEmpty()) {
            plugin.log("Disabling addons...");
            // Disable addons - pladdons are disabled by the server
            getEnabledAddons().stream().filter(addon -> !pladdons.containsKey(addon)).forEach(this::disable);
            plugin.log("Addons successfully disabled.");
        }
        // Unregister all commands
        GManager.getCommandManager().unregisterCommands();
        // Clear all maps
        listeners.clear();
        pladdons.clear();
        modules.clear();
        loaders.clear();
        classes.clear();
    }

    /**
     * Gets the addon by name
     * @param name addon name, not null
     * @return Optional addon object
     */
    @NonNull
    @SuppressWarnings("unchecked")
    public <T extends Module> Optional<T> getAddonByName(@NonNull String name) {
        return modules.stream().filter(a -> a.getDescription().getName().equalsIgnoreCase(name)).map(a -> (T) a)
                .findFirst();
    }

    /**
     * Gets the addon by main class name
     * @param name - main class name
     * @return Optional addon object
     */
    @NonNull
    @SuppressWarnings("unchecked")
    public <T extends Module> Optional<T> getAddonByMainClassName(@NonNull String name) {
        return modules.stream().filter(a -> a.getDescription().getMain().equalsIgnoreCase(name)).map(a -> (T) a)
                .findFirst();
    }

    @NonNull
    private YamlConfiguration addonDescription(@NonNull JarFile jar)
            throws InvalidModuleFormatException, IOException, InvalidConfigurationException {
        // Obtain the addon.yml file
        JarEntry entry = jar.getJarEntry("addon.yml");
        if (entry == null) {
            throw new InvalidModuleFormatException("Addon '" + jar.getName() + "' doesn't contains addon.yml file");
        }
        // Open a reader to the jar
        BufferedReader reader = new BufferedReader(new InputStreamReader(jar.getInputStream(entry)));
        // Grab the description in the addon.yml file
        YamlConfiguration data = new YamlConfiguration();
        data.load(reader);
        reader.close();
        return data;
    }

    @NonNull
    public List<Module> getAddons() {
        return modules;
    }

    /**
     * Gets an unmodifiable list of Addons that are loaded.
     * @return list of loaded Addons.
     * @since 1.1
     */
    @NonNull
    public List<Module> getLoadedAddons() {
        return modules.stream().filter(addon -> addon.getState().equals(Module.State.LOADED)).toList();
    }

    /**
     * Gets an unmodifiable list of Addons that are enabled.
     * @return list of enabled Addons.
     * @since 1.1
     */
    @NonNull
    public List<Module> getEnabledAddons() {
        return modules.stream().filter(addon -> addon.getState().equals(Module.State.ENABLED)).toList();
    }

    @Nullable
    public ModuleClassLoader getLoader(@NonNull final Module module) {
        return loaders.get(module);
    }

    /**
     * Finds a class by name that has been loaded by this loader
     * @param name name of the class, not null
     * @return Class the class or null if not found
     */
    @Nullable
    public Class<?> getClassByName(@NonNull final String name) {
        try {
            return classes.getOrDefault(name, loaders.values().stream().filter(Objects::nonNull)
                    .map(l -> l.findClass(name, false)).filter(Objects::nonNull).findFirst().orElse(null));
        } catch (Exception ignored) {
            // Ignored.
        }
        return null;
    }

    /**
     * Sets a class that this loader should know about
     *
     * @param name name of the class, not null
     * @param clazz the class, not null
     */
    public void setClass(@NonNull final String name, @NonNull final Class<?> clazz) {
        classes.putIfAbsent(name, clazz);
    }

    /**
     * Sorts the addons into loading order taking into account dependencies
     */
    private void sortAddons() {
        // Lists all available addons as names.
        List<String> names = modules.stream().map(a -> a.getDescription().getName()).toList();

        // Check that any dependencies exist
        Iterator<Module> addonsIterator = modules.iterator();
        while (addonsIterator.hasNext()) {
            Module a = addonsIterator.next();
            for (String dependency : a.getDescription().getDependencies()) {
                if (!names.contains(dependency)) {
                    plugin.logError(a.getDescription().getName() + " has dependency on " + dependency
                            + " that does not exist. Addon will not load!");
                    addonsIterator.remove();
                    break;
                }
            }
        }

        // Load dependencies or soft dependencies
        Map<String, Module> sortedAddons = new LinkedHashMap<>();
        // Start with nodes with no dependencies
        modules.stream()
                .filter(a -> a.getDescription().getDependencies().isEmpty()
                        && a.getDescription().getSoftDependencies().isEmpty())
                .forEach(a -> sortedAddons.put(a.getDescription().getName(), a));
        // Fill remaining
        List<Module> remaining = modules.stream().filter(a -> !sortedAddons.containsKey(a.getDescription().getName()))
                .toList();

        // Run through remaining addons
        remaining.forEach(addon -> {
            // Get the addon's dependencies.
            List<String> dependencies = new ArrayList<>(addon.getDescription().getDependencies());
            dependencies.addAll(addon.getDescription().getSoftDependencies());

            // Remove already sorted addons (dependencies) from the list
            dependencies.removeIf(sortedAddons::containsKey);

            if (dependencies.stream()
                    .noneMatch(dependency -> addon.getDescription().getDependencies().contains(dependency))) {
                sortedAddons.put(addon.getDescription().getName(), addon);
            }
        });

        modules.clear();
        modules.addAll(sortedAddons.values());
    }

    /**
     * Register a listener
     * @param module - the addon registering
     * @param listener - listener
     */
    public void registerListener(@NonNull Module module, @NonNull Listener listener) {
        Bukkit.getPluginManager().registerEvents(listener, GManager.getInstance());
        listeners.computeIfAbsent(module, k -> new ArrayList<>()).add(listener);
    }

    /**
     * Disables an addon
     * @param module - addon
     */
    private void disable(@NonNull Module module) {
        // Clear listeners
        if (listeners.containsKey(module)) {
            listeners.get(module).forEach(HandlerList::unregisterAll);
            listeners.remove(module);
        }
        // Unload all commands
        module.unloadAllCommands();
        // Unregister flags
        //plugin.getFlagsManager().unregister(addon);
        // Disable
        if (module.isEnabled()) {
            plugin.log("Disabling " + module.getDescription().getName() + "...");
            try {
                module.onDisable();
            } catch (Exception e) {
                plugin.logError("Error occurred when disabling addon " + module.getDescription().getName());
                plugin.logError("Report this to the addon's author(s)");
                module.getDescription().getAuthors().forEach(plugin::logError);
                plugin.logStacktrace(e);
            }
            new ModuleEvent().builder().addon(module).reason(ModuleEvent.Reason.DISABLE).build();
        }
        // Clear loaders
        if (loaders.containsKey(module)) {
            Set<String> unmodifiableSet = Collections.unmodifiableSet(loaders.get(module).getClasses());
            for (String className : unmodifiableSet) {
                classes.remove(className);
            }
            module.setState(Module.State.DISABLED);
            loaders.remove(module);
        }
        // Disable pladdons
        if (pladdons.containsKey(module)) {
            this.pluginLoader.disablePlugin(Objects.requireNonNull(this.pladdons.get(module)));
            pladdons.remove(module);
        }
        // Remove it from the addons list
        modules.remove(module);
    }


    /**
     * Notifies all addons that GManager has loaded all addons
     * @since 1.8.0
     */
    public void allLoaded() {
        this.getEnabledAddons().forEach(this::allLoaded);
    }

    /**
     * This method calls Addon#allLoaded in safe manner. If for some reason addon crashes on Addon#allLoaded, then
     * it will disable itself without harming other addons.
     * @param module Addon that should trigger Addon#allLoaded method.
     */
    private void allLoaded(@NonNull Module module) {
        try {
            module.allLoaded();
        } catch (NoClassDefFoundError | NoSuchMethodError | NoSuchFieldError e) {
            // Looks like the addon is incompatible, because it tries to refer to missing classes...
            this.handleAddonIncompatibility(module, e);
            // Disable addon.
            this.disable(module);
        } catch (Exception e) {
            // Unhandled exception. We'll give a bit of debug here.
            this.handleAddonError(module, e);
            // Disable addon.
            this.disable(module);
        }
    }
}
