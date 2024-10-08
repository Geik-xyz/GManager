package xyz.geik.gmanager.api.modules;

import java.io.File;
import java.io.IOException;

import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.io.Files;
import xyz.geik.gmanager.modules.Module;

/**
 * Provides a shell for addons to become Plugins so that other Plugins
 * can tap into their API more easily. Plugin + addon = Pladdon
 * @author tastybento
 *
 */
public abstract class Pladdon extends JavaPlugin {

    private static final String ADDONS_FOLDER = "GManager" + File.separator + "addons";

    /**
     * This must return a new instance of the addon. It is called when the Pladdon is loaded.
     * @return new instance of the addon
     */
    public abstract Module getAddon();

    @Override
    public void onLoad() {
        String parentFolder = getFile().getParent();
        if (parentFolder == null || !parentFolder.endsWith(ADDONS_FOLDER)) {
            // Jar is in the wrong place. Let's move it
            //moveJar();
        }
    }

    @Override
    public void onDisable() {
        Module module = getAddon();
        if (module != null) {
            module.onDisable();
        }
    }

    protected void moveJar() {
        getLogger().severe(getFile().getName() + " must be in the " + ADDONS_FOLDER + " folder! Trying to move it there...");
        File addons = new File(getFile().getParent(), ADDONS_FOLDER);
        if (addons.exists() || addons.mkdirs()) {
            File to = new File(addons, getFile().getName());
            if (!to.exists()) {
                try {
                    Files.move(getFile(), to);
                    getLogger().severe(getFile().getName() + " moved successfully.");

                } catch (IOException ex) {
                    getLogger().severe("Failed to move it. " + ex.getMessage());
                    getLogger().severe("Move " + getFile().getName() + " manually into the " + ADDONS_FOLDER + " folder. Then restart server.");
                }
            } else {
                getLogger().warning(getFile().getName() + " already is in the addons folder. Delete the one in the plugins folder.");
            }
        } else {
            getLogger().severe("GManager addons folder could not be made! " + addons.getAbsolutePath());
        }

    }


    /**
     * This method enables marks pladdons as enabled.
     * By default, enable status is not set because onEnable and onLoad is not triggered.
     */
    public void setEnabled() {
        this.setEnabled(true);
    }
}