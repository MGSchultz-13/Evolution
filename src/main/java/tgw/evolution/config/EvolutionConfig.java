package tgw.evolution.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;
import tgw.evolution.util.Time;

public final class EvolutionConfig {

    public static final Common COMMON;
    public static final Client CLIENT;
    private static final ForgeConfigSpec COMMON_SPEC;
    private static final ForgeConfigSpec CLIENT_SPEC;

    static {
        final Pair<Common, ForgeConfigSpec> commonSpecPair = new ForgeConfigSpec.Builder().configure(Common::new);
        COMMON_SPEC = commonSpecPair.getRight();
        COMMON = commonSpecPair.getLeft();
        final Pair<Client, ForgeConfigSpec> clientSpecPair = new ForgeConfigSpec.Builder().configure(Client::new);
        CLIENT_SPEC = clientSpecPair.getRight();
        CLIENT = clientSpecPair.getLeft();
    }

    private EvolutionConfig() {
    }

    public static void register(final ModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, COMMON_SPEC);
        context.registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC);
    }

    public static class Common {
        public final ForgeConfigSpec.IntValue torchTime;

        Common(final ForgeConfigSpec.Builder builder) {
            builder.push("Common");
            this.torchTime = builder.translation("evolution.config.torchTime")
                                    .defineInRange("torchTime", 10, 0, Time.YEAR_IN_TICKS / Time.HOUR_IN_TICKS);
            builder.pop();
        }
    }

    public static class Client {

        public final ForgeConfigSpec.BooleanValue celestialEquator;
        public final ForgeConfigSpec.BooleanValue celestialForceAll;
        public final ForgeConfigSpec.BooleanValue celestialPoles;
        public final ForgeConfigSpec.BooleanValue crazyMode;
        public final ForgeConfigSpec.BooleanValue ecliptic;
        public final ForgeConfigSpec.BooleanValue firstPersonRenderer;
        public final ForgeConfigSpec.BooleanValue hitmarkers;
        public final ForgeConfigSpec.BooleanValue limitTimeUnitsToHour;
        public final ForgeConfigSpec.BooleanValue planets;
        public final ForgeConfigSpec.BooleanValue showPlanets;

        Client(final ForgeConfigSpec.Builder builder) {
            builder.push("Client");
            this.crazyMode = builder.translation("evolution.config.crazyMode").define("crazyMode", false);
            this.limitTimeUnitsToHour = builder.translation("evolution.config.limitTimeUnitsToHour").define("limitTimeUnitsToHour", false);
            this.hitmarkers = builder.translation("evolution.config.hitmarkers").define("hitmarkers", true);
            this.firstPersonRenderer = builder.translation("evolution.config.firstPersonRenderer").define("firstPersonRenderer", true);
            this.showPlanets = builder.translation("evolution.config.showPlanets").define("showPlanets", true);
            builder.push("debug");
            builder.push("sky");
            this.celestialForceAll = builder.translation("evolution.config.celestialForceAll").define("celestialForceAll", false);
            this.celestialEquator = builder.translation("evolution.config.celestialEquator").define("celestialEquator", false);
            this.celestialPoles = builder.translation("evolution.config.celestialPoles").define("celestialPoles", false);
            this.ecliptic = builder.translation("evolution.config.ecliptic").define("ecliptic", false);
            this.planets = builder.translation("evolution.config.planets").define("planets", false);
            builder.pop();
            builder.pop();
            builder.pop();
        }
    }
}
