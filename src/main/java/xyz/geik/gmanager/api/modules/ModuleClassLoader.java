package xyz.geik.gmanager.api.modules;

import lombok.Getter;
import lombok.NonNull;
import xyz.geik.gmanager.GManager;
import xyz.geik.gmanager.api.managers.ModuleManager;
import xyz.geik.gmanager.api.modules.exceptions.InvalidModuleDescriptionException;
import xyz.geik.gmanager.api.modules.exceptions.InvalidModuleFormatException;
import xyz.geik.gmanager.api.modules.exceptions.InvalidModuleInheritException;
import xyz.geik.gmanager.modules.Module;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.InvalidDescriptionException;

import javax.annotation.Nullable;

/**
 * Loads addons and sets up permissions
 * @author Tastybento, ComminQ
 */
public class ModuleClassLoader extends URLClassLoader {

    private final Map<String, Class<?>> classes = new HashMap<>();
    /**
     * -- GETTER --
     *
     */
    @Getter
    private final Module module;
    private final ModuleManager loader;

    /**
     * For testing only
     *
     * @param module   addon
     * @param loader  Addons Manager
     * @param jarFile Jar File
     * @throws MalformedURLException exception
     */
    protected ModuleClassLoader(Module module, ModuleManager loader, File jarFile) throws MalformedURLException {
        super(new URL[]{jarFile.toURI().toURL()});
        this.module = module;
        this.loader = loader;
    }

    public ModuleClassLoader(ModuleManager addonsManager, YamlConfiguration data, File jarFile, ClassLoader parent)
            throws InvalidModuleInheritException,
            MalformedURLException,
            InvalidDescriptionException,
            InvalidModuleDescriptionException,
            InstantiationException,
            IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        super(new URL[]{jarFile.toURI().toURL()}, parent);

        loader = addonsManager;

        Class<?> javaClass;
        try {
            String mainClass = data.getString("main");
            if (mainClass == null) {
                throw new InvalidModuleFormatException("addon.yml does not define a main class!");
            }
            javaClass = Class.forName(mainClass, true, this);
            if (mainClass.startsWith("xyz.geik.gmanager")) {
                throw new InvalidModuleFormatException("Package declaration cannot start with 'xyz.geik.gmanager'");
            }
        } catch (Exception e) {
            throw new InvalidDescriptionException("Could not load '" + jarFile.getName() + "' in folder '" + jarFile.getParent() + "' - " + e.getMessage());
        }

        Class<? extends Module> addonClass;
        try {
            addonClass = javaClass.asSubclass(Module.class);
        } catch (ClassCastException e) {
            throw new InvalidModuleInheritException("Main class does not extend 'Addon'");
        }

        module = addonClass.getDeclaredConstructor().newInstance();
        module.setDescription(asDescription(data));
    }


    /**
     * Converts the addon.yml to an AddonDescription
     *
     * @param data - yaml config (addon.yml)
     * @return Addon Description
     * @throws InvalidModuleDescriptionException - if there's a bug in the addon.yml
     */
    @NonNull
    public static ModuleDescription asDescription(YamlConfiguration data) throws InvalidModuleDescriptionException {
        // Validate addon.yml
        if (!data.contains("main")) {
            throw new InvalidModuleDescriptionException("Missing 'main' tag. A main class must be listed in addon.yml");
        }
        if (!data.contains("name")) {
            throw new InvalidModuleDescriptionException("Missing 'name' tag. An addon name must be listed in addon.yml");
        }
        if (!data.contains("version")) {
            throw new InvalidModuleDescriptionException("Missing 'version' tag. A version must be listed in addon.yml");
        }
        if (!data.contains("authors")) {
            throw new InvalidModuleDescriptionException("Missing 'authors' tag. At least one author must be listed in addon.yml");
        }

        ModuleDescription.Builder builder = new ModuleDescription.Builder(
                // Mandatory elements
                Objects.requireNonNull(data.getString("main")),
                Objects.requireNonNull(data.getString("name")),
                Objects.requireNonNull(data.getString("version")))
                .authors(Objects.requireNonNull(data.getString("authors")))
                // Optional elements
                .metrics(data.getBoolean("metrics", true))
                .repository(data.getString("repository", ""));

        String depend = data.getString("depend");
        if (depend != null) {
            builder.dependencies(Arrays.asList(depend.split("\\s*,\\s*")));
        }
        String softDepend = data.getString("softdepend");
        if (softDepend != null) {
            builder.softDependencies(Arrays.asList(softDepend.split("\\s*,\\s*")));
        }
        Material icon = Material.getMaterial(data.getString("icon", "PAPER").toUpperCase(Locale.ENGLISH));
        if (icon == null) {
            throw new InvalidModuleDescriptionException("'icon' tag refers to an unknown Material: " + data.getString("icon"));
        }
        builder.icon(Objects.requireNonNull(icon));

        String apiVersion = data.getString("api-version");
        if (apiVersion != null) {
            if (!apiVersion.replace("-SNAPSHOT", "").matches("^(\\d+\\.)?(\\d+\\.)?(\\*|\\d+)$")) {
                throw new InvalidModuleDescriptionException("Provided API version '" + apiVersion + "' is not valid. It must only contain digits and dots and not end with a dot.");
            }
            if (apiVersion.contains("-SNAPSHOT")) {
                GManager.getInstance().logWarning(data.getString("name") + " addon depends on development version of GManager plugin. Some functions may be not implemented.");
            }
            builder.apiVersion(apiVersion);
        }
        // Set permissions
        if (data.isConfigurationSection("permissions")) {
            builder.permissions(Objects.requireNonNull(data.getConfigurationSection("permissions")));
        }

        return builder.build();
    }

    /* (non-Javadoc)
     * @see java.net.URLClassLoader#findClass(java.lang.String)
     */
    @Override
    @Nullable
    protected Class<?> findClass(String name) {
        return findClass(name, true);
    }

    /**
     * This is a custom findClass that enables classes in other addons to be found
     *
     * @param name        - class name
     * @param checkGlobal - check globally or not when searching
     * @return Class - class if found
     */
    public Class<?> findClass(String name, boolean checkGlobal) {
        if (name.startsWith("xyz.geik.gmanager")) {
            return null;
        }
        Class<?> result = classes.get(name);
        if (result == null) {
            if (checkGlobal) {
                result = loader.getClassByName(name);
            }

            if (result == null) {
                try {
                    result = super.findClass(name);
                } catch (ClassNotFoundException | NoClassDefFoundError e) {
                    // Do nothing.
                }
                if (result != null) {
                    loader.setClass(name, result);

                }
            }
            classes.put(name, result);
        }
        return result;
    }

    /**
     * @return class list
     */
    public Set<String> getClasses() {
        return classes.keySet();
    }
}
