package com.bundleintooltip.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;

public final class BundleInTooltipConfig {
    public static final Client CLIENT;
    public static final ForgeConfigSpec CLIENT_SPEC;
    private static ModConfig clientConfig;
    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        CLIENT = new Client(builder);
        CLIENT_SPEC = builder.build();
    }

    private BundleInTooltipConfig() {
    }

    public static boolean showCapacityBar() {
        return CLIENT.showCapacityBar.get();
    }

    public static CapacityLabelMode capacityLabelMode() {
        return CLIENT.capacityLabelMode.get();
    }

    public static SlotTextureMode slotTextureMode() {
        return CLIENT.slotTextureMode.get();
    }

    public static boolean showSelectionBar() {
        return CLIENT.showSelectionBar.get();
    }

    public static void setClientConfig(ModConfig config) {
        if (config.getSpec() == CLIENT_SPEC) {
            clientConfig = config;
        }
    }

    public static void save() {
        if (clientConfig != null) {
            clientConfig.save();
        }
    }

    public static final class Client {
        public final ForgeConfigSpec.BooleanValue showCapacityBar;
        public final ForgeConfigSpec.EnumValue<CapacityLabelMode> capacityLabelMode;
        public final ForgeConfigSpec.EnumValue<SlotTextureMode> slotTextureMode;
        public final ForgeConfigSpec.BooleanValue showSelectionBar;

        private Client(ForgeConfigSpec.Builder builder) {
            builder.push("tooltip");
            showCapacityBar = builder
                .comment("Choose which capacity display to use (Vanilla = bar, Classic = line).")
                .define("showCapacityBar", true);
            capacityLabelMode = builder
                .comment("What text, if any, should appear inside the capacity bar.",
                    "NONE - match 1.21 preview and leave the bar blank.",
                    "VANILLA - show Empty / Full like modern snapshots.",
                    "CLASSIC - show the classic 24/64 fullness number.",
                    "HYBRID - Empty/Full labels at the extremes, numeric value otherwise.",
                    "VANILLA_REVISED - Empty/Filling/Full labels similar to the 1.21 preview.")
                .defineEnum("capacityLabelMode", CapacityLabelMode.VANILLA);
            slotTextureMode = builder
                .comment("Choose which bundle slot texture style to use.",
                    "VANILLA - match the latest 1.21 UI with gray slot backgrounds (default).",
                    "EXPERIMENT - floating icons inspired by the early snapshot preview.",
                    "CLASSIC - use the older grid from 1.20.1.")
                .defineEnum("slotTextureMode", SlotTextureMode.VANILLA);
            showSelectionBar = builder
                .comment("Show the 1.21-style preview bar for the currently selected item.")
                .define("showSelectionBar", true);
            builder.pop();
        }
    }

    public enum CapacityLabelMode {
        NONE,
        VANILLA,
        CLASSIC,
        HYBRID,
        VANILLA_REVISED
    }

    public enum SlotTextureMode {
        VANILLA,
        EXPERIMENT,
        CLASSIC;

        public boolean isClassic() {
            return this == CLASSIC;
        }

        public boolean isModern() {
            return this != CLASSIC;
        }
    }
}
