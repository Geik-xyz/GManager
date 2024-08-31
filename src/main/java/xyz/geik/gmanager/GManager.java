package xyz.geik.gmanager;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.geik.glib.GLib;
import xyz.geik.glib.chat.ChatUtils;
import xyz.geik.glib.shades.okaeri.configs.ConfigManager;
import xyz.geik.glib.shades.okaeri.configs.yaml.bukkit.YamlBukkitConfigurer;
import xyz.geik.glib.shades.triumphteam.cmd.bukkit.BukkitCommandManager;
import xyz.geik.glib.shades.triumphteam.cmd.bukkit.message.BukkitMessageKey;
import xyz.geik.glib.shades.triumphteam.cmd.core.message.MessageKey;
import xyz.geik.glib.simplixstorage.SimplixStorageAPI;
import xyz.geik.gmanager.api.managers.CommandManager;
import xyz.geik.gmanager.api.managers.ModuleManager;
import xyz.geik.gmanager.configuration.ConfigFile;
import xyz.geik.gmanager.configuration.LangFile;

import java.io.File;

/**
 * Main class of Gmanager
 * @author poyraz.inan
 * @since release
 */
@Setter
public class GManager extends JavaPlugin {

    @Getter
    private static GManager instance;

    @Getter
    private SimplixStorageAPI simplixStorageAPI;

    @Getter
    private static LangFile langFile;

    @Getter
    private static ConfigFile configFile;

    @Getter
    private static ModuleManager moduleManager;

    /**
     * CommandManager
     */
    @Getter
    private static BukkitCommandManager<CommandSender> commandManager;

    public void onLoad() {
        instance = this;
        simplixStorageAPI = new SimplixStorageAPI(this);
        setupFiles();
    }

    public void onEnable() {
        new GLib(this, getLangFile().getMessages().getPrefix());
        setupCommands();
        moduleManager = new ModuleManager(this);
        moduleManager.loadAddons();
        moduleManager.enableAddons();
    }

    public void onDisable() {
        CommandManager.unregisterCommands();
        moduleManager.disableAddons();
    }


    private void setupCommands() {
        commandManager = BukkitCommandManager.create(this);
        commandManager.registerMessage(MessageKey.INVALID_ARGUMENT, (sender, invalidArgumentContext) ->
                ChatUtils.sendMessage(sender, getLangFile().getMessages().getInvalidArgument()));
        commandManager.registerMessage(MessageKey.UNKNOWN_COMMAND, (sender, invalidArgumentContext) ->
                ChatUtils.sendMessage(sender, getLangFile().getMessages().getUnknownCommand()));
        commandManager.registerMessage(MessageKey.NOT_ENOUGH_ARGUMENTS, (sender, invalidArgumentContext) ->
                ChatUtils.sendMessage(sender, getLangFile().getMessages().getNotEnoughArguments()));
        commandManager.registerMessage(MessageKey.TOO_MANY_ARGUMENTS, (sender, invalidArgumentContext) ->
                ChatUtils.sendMessage(sender, getLangFile().getMessages().getTooManyArguments()));
        commandManager.registerMessage(BukkitMessageKey.NO_PERMISSION, (sender, invalidArgumentContext) ->
                ChatUtils.sendMessage(sender, getLangFile().getMessages().getNoPerm()));
    }

    /**
     * Setups config, lang and modules file file
     */
    public void setupFiles() {
        try {
            configFile = ConfigManager.create(ConfigFile.class, (it) -> {
                it.withConfigurer(new YamlBukkitConfigurer());
                it.withBindFile(new File(getDataFolder(), "config.yml"));
                it.saveDefaults();
                it.load(true);
            });
            String langName = configFile.getSettings().getLang();
            Class langClass = Class.forName("xyz.geik.gmanager.configuration.lang." + langName);
            Class<LangFile> languageClass = langClass;
            langFile = ConfigManager.create(languageClass, (it) -> {
                it.withConfigurer(new YamlBukkitConfigurer());
                it.withBindFile(new File(getDataFolder() + "/lang", langName + ".yml"));
                it.saveDefaults();
                it.load(true);
            });
        } catch (Exception exception) {
            getPluginLoader().disablePlugin(this);
            throw new RuntimeException("Error loading configuration file");
        }
    }

    public void log(String string) {
        getLogger().info(() -> string);
    }

    public void logDebug(Object object) {
        getLogger().info(() -> "DEBUG: " + object);
    }

    public void logError(String error) {
        getLogger().severe(() -> error);
    }

    public void logWarning(String warning) {
        getLogger().warning(() -> warning);
    }

    /**
     * Logs the stacktrace of a Throwable that was thrown by an error.
     * It should be used preferably instead of {@link Throwable#printStackTrace()} as it does not risk exposing sensitive information.
     * @param throwable the Throwable that was thrown by an error.
     * @since 1.3.0
     */
    public void logStacktrace(@NonNull Throwable throwable) {
        logError(ExceptionUtils.getStackTrace(throwable));
    }
}