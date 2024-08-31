package xyz.geik.gmanager.configuration.lang;


import lombok.Getter;
import lombok.Setter;
import xyz.geik.glib.shades.okaeri.configs.annotation.Comment;
import xyz.geik.glib.shades.okaeri.configs.annotation.NameModifier;
import xyz.geik.glib.shades.okaeri.configs.annotation.NameStrategy;
import xyz.geik.glib.shades.okaeri.configs.annotation.Names;
import xyz.geik.gmanager.configuration.LangFile;

/**
 * LangFile of GManager
 * @author geik
 * @since 1.0
 */
@Getter
@Setter
@Names(strategy = NameStrategy.HYPHEN_CASE, modifier = NameModifier.TO_LOWER_CASE)
public class en extends LangFile {

    /**
     * Settings menu of config
     */
    @Comment("Chat messages")
    private Messages messages = new Messages();

    /**
     * Messages of plugin
     *
     * @author geik
     * @since 2.0
     */
    @Getter
    @Setter
    public static class Messages extends LangFile.Messages {

        @Comment("Prefix of messages")
        private String prefix = "&3GManager &8»";

        private String noPerm = "{prefix} &cYou do not have permission to do this action!";
        private String youHaveToBePlayer = "{prefix} &cYou have to be player to execute this command!";
        private String configReloaded = "{prefix} &aConfig reloaded successfully.";
        private String invalidArgument = "{prefix} &cInvalid argument!";
        private String unknownCommand = "{prefix} &cUnknown command!";
        private String notEnoughArguments = "{prefix} &cNot enough arguments!";
        private String tooManyArguments = "{prefix} &cToo many arguments!";
        private String targetPlayerNotAvailable = "{prefix} &cTarget player is not available.";
        private String playerNotOnline = "{prefix} &cTarget player is not online.";
        private String playerNotAvailable = "{prefix} &cPlayer is not available.";
        private String inCooldown = "{prefix} &cYou should wait {time}s for do it again.";
    }
}