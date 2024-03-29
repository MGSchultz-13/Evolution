package tgw.evolution.hooks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import tgw.evolution.Evolution;
import tgw.evolution.util.collection.maps.O2OHashMap;
import tgw.evolution.util.collection.maps.O2OMap;

import java.util.function.Supplier;

public final class ClientHooks {

    private static final O2OMap<ModelLayerLocation, Supplier<LayerDefinition>> LAYER_DEFINITIONS = new O2OHashMap<>();

    private ClientHooks() {
    }

    public static void loadLayerDefinitions(O2OMap<ModelLayerLocation, LayerDefinition> map) {
        for (long it = LAYER_DEFINITIONS.beginIteration(); LAYER_DEFINITIONS.hasNextIteration(it); it = LAYER_DEFINITIONS.nextEntry(it)) {
            map.put(LAYER_DEFINITIONS.getIterationKey(it), LAYER_DEFINITIONS.getIterationValue(it).get());
        }
    }

    public static void onPickBlock(HitResult target, Player player, BlockGetter level) {
        Minecraft mc = Minecraft.getInstance();
        assert mc.gameMode != null;
        ItemStack result = ItemStack.EMPTY;
        boolean isCreative = player.getAbilities().instabuild;
        BlockEntity te = null;
        if (target.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHitResult = (BlockHitResult) target;
            int x = blockHitResult.posX();
            int y = blockHitResult.posY();
            int z = blockHitResult.posZ();
            BlockState state = level.getBlockState_(x, y, z);
            if (state.isAir()) {
                return;
            }
            if (isCreative && Screen.hasControlDown() && state.hasBlockEntity()) {
                te = level.getBlockEntity_(x, y, z);
            }
            result = state.getBlock().getCloneItemStack(state, target, level, x, y, z, player);
        }
        else if (target.getType() == HitResult.Type.ENTITY) {
            Entity entity = ((EntityHitResult) target).getEntity();
            result = entity.getPickResult();
            if (result == null) {
                Evolution.warn("Picking on: [{}] {} gave null item", target.getType(), entity.getType());
                return;
            }
        }
        if (result.isEmpty()) {
            return;
        }
        if (te != null) {
            mc.addCustomNbtData(result, te);
        }
        if (isCreative) {
            player.getInventory().setPickedItem(result);
            mc.gameMode.handleCreativeModeItemAdd(player.getItemInHand(InteractionHand.MAIN_HAND),
                                                  36 + player.getInventory().selected);
            return;
        }
        int slot = player.getInventory().findSlotMatchingItem(result);
        if (slot != -1) {
            if (Inventory.isHotbarSlot(slot)) {
                player.getInventory().selected = slot;
            }
            else {
                mc.gameMode.handlePickItem(slot);
            }
        }
    }

    public static void registerLayerDefinition(ModelLayerLocation layerLocation, Supplier<LayerDefinition> supplier) {
        LAYER_DEFINITIONS.put(layerLocation, supplier);
    }
}
