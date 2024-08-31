package xyz.geik.gmanager.configuration;

import lombok.Getter;
import lombok.Setter;
import xyz.geik.glib.shades.okaeri.configs.OkaeriConfig;
import xyz.geik.glib.shades.okaeri.configs.annotation.Comment;
import xyz.geik.glib.shades.okaeri.configs.annotation.NameModifier;
import xyz.geik.glib.shades.okaeri.configs.annotation.NameStrategy;
import xyz.geik.glib.shades.okaeri.configs.annotation.Names;

/**
 * Main config file
 * @author geik
 * @since 2.0
 */
@Getter
@Setter
@Names(strategy = NameStrategy.HYPHEN_CASE, modifier = NameModifier.TO_LOWER_CASE)
public class ConfigFile extends OkaeriConfig {

    /**
     * Settings menu of config
     */
    @Comment("Main settings")
    private Settings settings = new Settings();

    /**
     * Settings configuration of config
     *
     * @author geik
     * @since 2.0
     */
    @Getter
    @Setter
    public static class Settings extends OkaeriConfig {
        @Comment("Language of plugin")
        private String lang = "tr";

        @Comment("auto or vault, royaleconomy, playerpoints, gringotts, elementalgems")
        private String economy = "auto";
    }

    @Comment({"If you don't know about database settings", "please don't change here. Leave it SQLite"})
    private Database database = new Database();

    /**
     * Database configuration settings
     * @author geik
     * @since 2.0
     */
    @Getter @Setter
    public static class Database extends OkaeriConfig {
        private String databaseType = "SQLite";
        private String host = "localhost";
        private String port = "3306";
        private String tableName = "farmer_db";
        private String userName = "farmer";
        private String password = "supersecretpassword";
    }
}