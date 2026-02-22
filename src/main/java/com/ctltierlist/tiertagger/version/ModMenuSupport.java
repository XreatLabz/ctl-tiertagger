package com.ctltierlist.tiertagger.version;

import com.ctltierlist.tiertagger.CTLTierTagger;
import net.fabricmc.loader.api.FabricLoader;
public final class ModMenuSupport {
    private static final MinecraftVersion MC_1_21_0 = MinecraftVersion.parse("1.21");
    private static final MinecraftVersion MC_1_21_1 = MinecraftVersion.parse("1.21.1");
    private static final MinecraftVersion MC_1_21_3 = MinecraftVersion.parse("1.21.3");
    private static final MinecraftVersion MC_1_21_4 = MinecraftVersion.parse("1.21.4");
    private static final MinecraftVersion MC_1_21_5 = MinecraftVersion.parse("1.21.5");
    private static final MinecraftVersion MC_1_21_8 = MinecraftVersion.parse("1.21.8");
    private static final MinecraftVersion MC_1_21_10 = MinecraftVersion.parse("1.21.10");

    private static final MinecraftVersion MODMENU_11_0_3 = MinecraftVersion.parse("11.0.3");
    private static final MinecraftVersion MODMENU_12_0_0 = MinecraftVersion.parse("12.0.0");
    private static final MinecraftVersion MODMENU_13_0_0 = MinecraftVersion.parse("13.0.0");
    private static final MinecraftVersion MODMENU_14_0_1 = MinecraftVersion.parse("14.0.1");
    private static final MinecraftVersion MODMENU_15_0_1 = MinecraftVersion.parse("15.0.1");
    private static final MinecraftVersion MODMENU_16_0_0 = MinecraftVersion.parse("16.0.0");
    private static final MinecraftVersion MODMENU_17_0_0_BETA_2 = MinecraftVersion.parse("17.0.0-beta.2");

    private ModMenuSupport() {}

    public static void requireCompatibleOrThrow() {
        var container = FabricLoader.getInstance()
            .getModContainer("modmenu")
            .orElseThrow(() -> new IllegalStateException("Mod Menu is required but was not found."));

        MinecraftVersion minecraftVersion = VersionSupport.current();
        MinecraftVersion required = requiredMinimumForMinecraft(minecraftVersion);

        String versionString = container.getMetadata().getVersion().getFriendlyString();
        MinecraftVersion installed;
        try {
            installed = MinecraftVersion.parse(versionString);
        } catch (IllegalArgumentException e) {
            String message = "Unable to parse Mod Menu version '" + versionString + "' for Minecraft "
                + minecraftVersion + ". Required: >=" + required + ".";
            CTLTierTagger.LOGGER.error(message);
            throw new IllegalStateException(message, e);
        }

        if (installed.compareTo(required) < 0) {
            String message = "Incompatible Mod Menu version " + installed + " for Minecraft "
                + minecraftVersion + ". Required: >=" + required + ".";
            CTLTierTagger.LOGGER.error(message);
            throw new IllegalStateException(message);
        }

        CTLTierTagger.LOGGER.info("Detected compatible Mod Menu version {} for Minecraft {} (required >= {})",
            installed, minecraftVersion, required);
    }

    private static MinecraftVersion requiredMinimumForMinecraft(MinecraftVersion minecraftVersion) {
        if (minecraftVersion.isBetweenInclusive(MC_1_21_0, MC_1_21_1)) {
            return MODMENU_11_0_3;
        }
        if (minecraftVersion.isBetweenInclusive(MinecraftVersion.parse("1.21.2"), MC_1_21_3)) {
            return MODMENU_12_0_0;
        }
        if (minecraftVersion.equals(MC_1_21_4)) {
            return MODMENU_13_0_0;
        }
        if (minecraftVersion.equals(MC_1_21_5)) {
            return MODMENU_14_0_1;
        }
        if (minecraftVersion.isBetweenInclusive(MinecraftVersion.parse("1.21.6"), MC_1_21_8)) {
            return MODMENU_15_0_1;
        }
        if (minecraftVersion.isBetweenInclusive(MinecraftVersion.parse("1.21.9"), MC_1_21_10)) {
            return MODMENU_16_0_0;
        }
        return MODMENU_17_0_0_BETA_2;
    }
}
