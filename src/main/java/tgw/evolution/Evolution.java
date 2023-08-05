package tgw.evolution;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import net.minecraft.commands.synchronization.ArgumentTypes;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Contract;
import org.slf4j.Logger;
import tgw.evolution.blocks.BlockFire;
import tgw.evolution.blocks.tileentities.MoldingPatterns;
import tgw.evolution.commands.argument.EnumArgument;
import tgw.evolution.init.EvolutionEntities;
import tgw.evolution.init.EvolutionNetwork;
import tgw.evolution.util.EvolutionDataSerializers;
import tgw.evolution.util.hitbox.HitboxRegistry;
import tgw.evolution.util.toast.Toasts;

public final class Evolution implements ModInitializer {

    public static final String MODID = "evolution";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void debug(String message, Object... objects) {
        String clazz = Thread.currentThread().getStackTrace()[2].getClassName();
        LOGGER.debug("[" + clazz + "]: " + message, objects);
    }

    public static void debug(String message) {
        String clazz = Thread.currentThread().getStackTrace()[2].getClassName();
        LOGGER.debug("[{}]: {}", clazz, message);
    }

    public static void deprecatedConstructor() {
        warn("Calling deprecated constructor!");
    }

    public static void deprecatedMethod() {
        warn("Calling deprecated method!");
    }

    public static void error(String message, Object... objects) {
        String clazz = Thread.currentThread().getStackTrace()[2].getClassName();
        LOGGER.error("[" + clazz + "]: " + message, objects);
    }

    public static void error(String message) {
        String clazz = Thread.currentThread().getStackTrace()[2].getClassName();
        LOGGER.error("[{}]: {}", clazz, message);
    }

    @Contract("_ -> new")
    public static ResourceLocation getResource(String name) {
        return new ResourceLocation(MODID, name);
    }

    public static void info(String message, Object... objects) {
        String clazz = Thread.currentThread().getStackTrace()[2].getClassName();
        LOGGER.info("[" + clazz + "]: " + message, objects);
    }

    public static void info(String message) {
        String clazz = Thread.currentThread().getStackTrace()[2].getClassName();
        LOGGER.info("[{}]: {}", clazz, message);
    }

    public static void usingPlaceholder(Player player, String obj) {
        player.displayClientMessage(new TextComponent("[DEBUG] Using placeholder " + obj + "!"), false);
    }

    public static void warn(String message) {
        String clazz = Thread.currentThread().getStackTrace()[2].getClassName();
        LOGGER.warn("[{}]: {}", clazz, message);
    }

    public static void warn(String message, Throwable t) {
        warn(message);
        LOGGER.warn("Exception: ", t);
    }

    public static void warn(String message, Object... objects) {
        String clazz = Thread.currentThread().getStackTrace()[2].getClassName();
        LOGGER.warn("[" + clazz + "]: " + message, objects);
    }

    @Override
    public void onInitialize() {
        ArgumentTypes.register("evolution:enum", EnumArgument.class, new EnumArgument.Serializer());
        BlockFire.init();
        EvolutionDataSerializers.register();
        MoldingPatterns.load();
        Toasts.register();
        HitboxRegistry.register();
        EvolutionEntities.registerEntityWorldSpawns();
        EvolutionNetwork.registerEntitySpawnData();
        info("Evolution Mod initialized");
    }
}
