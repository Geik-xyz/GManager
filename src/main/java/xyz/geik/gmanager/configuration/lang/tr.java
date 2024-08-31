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
public class tr extends LangFile {

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

        private String noPerm = "{prefix} &cBunu yapmak için yetkin yok!";
        private String youHaveToBePlayer = "{prefix} &cBunu yapmak için oyuncu olman gerek!";
        private String configReloaded = "{prefix} &aConfig ve addonlar başarıyla yenilendi.";
        private String invalidArgument = "{prefix} &cGeçersiz Argüman!!";
        private String unknownCommand = "{prefix} &cBilinmeyen Komut!";
        private String notEnoughArguments = "{prefix} &cYetersiz argüman kullanımı!";
        private String tooManyArguments = "{prefix} &cÇok fazla argüman kullanımı!";
        private String targetPlayerNotAvailable = "{prefix} &cHedef oyuncu geçerli değil.";
        private String playerNotOnline = "{prefix} &cHedef oyuncu çevrim içi değil.";
        private String playerNotAvailable = "{prefix} &cOyuncu erişilemez.";
        private String inCooldown = "{prefix} &cBunu tekrar yapmak için {time} saniye beklemen gerek.";
    }
}