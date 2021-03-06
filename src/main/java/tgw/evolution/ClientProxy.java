package tgw.evolution;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.renderer.model.ModelBakery;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.glfw.GLFW;
import tgw.evolution.client.gui.ScreenCorpse;
import tgw.evolution.client.gui.ScreenInventoryExtended;
import tgw.evolution.events.ClientEvents;
import tgw.evolution.events.ItemEvents;
import tgw.evolution.init.EvolutionContainers;
import tgw.evolution.init.EvolutionParticles;
import tgw.evolution.init.EvolutionRenderer;
import tgw.evolution.init.EvolutionResources;
import tgw.evolution.util.reflection.StaticFieldHandler;

import java.util.Set;

public class ClientProxy implements IProxy {

    public static final KeyBinding TOGGLE_PRONE = new KeyBinding("key.prone.toggle",
                                                                 KeyConflictContext.IN_GAME,
                                                                 InputMappings.Type.KEYSYM,
                                                                 GLFW.GLFW_KEY_X,
                                                                 "key.categories.movement");
    public static final KeyBinding BUILDING_ASSIST = new KeyBinding("key.build_assist",
                                                                    KeyConflictContext.IN_GAME,
                                                                    InputMappings.Type.KEYSYM,
                                                                    GLFW.GLFW_KEY_BACKSLASH,
                                                                    "key.categories.creative");
    public static final StaticFieldHandler<ModelBakery, Set<ResourceLocation>> BUILTIN_TEXTURES = new StaticFieldHandler<>(ModelBakery.class,
                                                                                                                           "field_177602_b");

    private static void addTextures() {
        Set<ResourceLocation> builtin = BUILTIN_TEXTURES.get();
        for (String str : EvolutionResources.SLOT_EXTENDED) {
            //noinspection ObjectAllocationInLoop
            builtin.add(new ResourceLocation(str));
        }
    }

    public static void changeWorldOrders() {
        int evId = 0;
        for (WorldType worldType : WorldType.WORLD_TYPES) {
            if (worldType != null && "ev_default".equals(worldType.getName())) {
                evId = worldType.getId();
                break;
            }
        }
        WorldType evWorld = WorldType.WORLD_TYPES[evId];
        System.arraycopy(WorldType.WORLD_TYPES, 0, WorldType.WORLD_TYPES, 1, evId);
        WorldType.WORLD_TYPES[0] = evWorld;
    }

    @Override
    public PlayerEntity getClientPlayer() {
        return Minecraft.getInstance().player;
    }

    @Override
    public World getClientWorld() {
        return Minecraft.getInstance().world;
    }

    @Override
    public void init() {
        EvolutionRenderer.registryEntityRenders();
        addTextures();
        ScreenManager.registerFactory(EvolutionContainers.EXTENDED_INVENTORY.get(), ScreenInventoryExtended::new);
        ScreenManager.registerFactory(EvolutionContainers.CORPSE.get(), ScreenCorpse::new);
        ColorManager.registerBlockColorHandlers(Minecraft.getInstance().getBlockColors());
        ColorManager.registerItemColorHandlers(Minecraft.getInstance().getItemColors());
        MinecraftForge.EVENT_BUS.register(new ClientEvents(Minecraft.getInstance()));
        MinecraftForge.EVENT_BUS.register(new ItemEvents());
        ClientRegistry.registerKeyBinding(TOGGLE_PRONE);
        ClientRegistry.registerKeyBinding(BUILDING_ASSIST);
        changeWorldOrders();
        EvolutionParticles.register();
    }
}
