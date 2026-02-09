package com.ctltierlist.tiertagger.version;

import com.ctltierlist.tiertagger.CTLTierTagger;
import net.fabricmc.loader.api.FabricLoader;
public final class ModMenuSupport {
    public static final MinecraftVersion MIN_SUPPORTED = MinecraftVersion.parse("13.0.0");

    private ModMenuSupport() {}

    public static void requireCompatibleOrThrow() {
        var container = FabricLoader.getInstance()
            .getModContainer("modmenu")
            .orElseThrow(() -> new IllegalStateException("Mod Menu is required but was not found."));

        String versionString = container.getMetadata().getVersion().getFriendlyString();
        MinecraftVersion version;
        try {
            version = MinecraftVersion.parse(versionString);
        } catch (IllegalArgumentException e) {
            String message = "Unable to parse Mod Menu version '" + versionString + "'. "
                + "Expected >=13.0.0.";
            CTLTierTagger.LOGGER.error(message);
            throw new IllegalStateException(message, e);
        }

        if (version.compareTo(MIN_SUPPORTED) < 0) {
            String message = "Incompatible Mod Menu version " + version
                + ". Required: >=13.0.0.";
            CTLTierTagger.LOGGER.error(message);
            throw new IllegalStateException(message);
        }

        CTLTierTagger.LOGGER.info("Detected compatible Mod Menu version {}", version);
    }
}
