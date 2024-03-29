package tgw.evolution.util.constants;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import tgw.evolution.blocks.tileentities.KnappingRecipe;
import tgw.evolution.capabilities.modular.part.IPart;
import tgw.evolution.capabilities.modular.part.IPartType;
import tgw.evolution.capabilities.modular.part.PartTypes;
import tgw.evolution.init.EvolutionBlocks;
import tgw.evolution.init.EvolutionItems;
import tgw.evolution.init.EvolutionMaterials;
import tgw.evolution.init.IVariant;
import tgw.evolution.items.modular.part.ItemPart;
import tgw.evolution.util.UnregisteredFeatureException;
import tgw.evolution.util.collection.lists.OArrayList;
import tgw.evolution.util.collection.lists.OList;
import tgw.evolution.util.collection.maps.B2OHashMap;
import tgw.evolution.util.collection.maps.B2OMap;

import java.util.Map;

import static tgw.evolution.util.constants.RockType.*;

public enum RockVariant implements IVariant<RockVariant> {
    ANDESITE(0, IGNEOUS_EXTRUSIVE, "andesite"),
    BASALT(1, IGNEOUS_EXTRUSIVE, "basalt"),
    CHERT(2, SEDIMENTARY, "chert"),
    DIORITE(3, IGNEOUS_INTRUSIVE, "diorite"),
    GABBRO(4, IGNEOUS_INTRUSIVE, "gabbro"),
    GNEISS(5, METAMORPHIC, "gneiss"),
    GRANITE(6, IGNEOUS_INTRUSIVE, "granite"),
    LIMESTONE(7, SEDIMENTARY, "limestone"),
    MARBLE(8, METAMORPHIC, "marble"),
    RED_SANDSTONE(9, SEDIMENTARY, "red_sandstone"),
    RHYOLITE(10, IGNEOUS_EXTRUSIVE, "rhyolite"),
    SANDSTONE(11, SEDIMENTARY, "sandstone"),
    SCHIST(12, METAMORPHIC, "schist"),
    SHALE(13, SEDIMENTARY, "shale"),
    SLATE(14, METAMORPHIC, "slate");

    public static final RockVariant[] VALUES = values();
    private static final B2OMap<RockVariant> REGISTRY;
    private static final OList<Map<RockVariant, ? extends Block>> BLOCKS = new OArrayList<>();

    static {
        B2OMap<RockVariant> map = new B2OHashMap<>();
        for (RockVariant variant : VALUES) {
            if (map.put(variant.id, variant) != null) {
                throw new IllegalStateException("RockVariant " + variant + " has duplicate id: " + variant.id);
            }
        }
        map.trim();
        REGISTRY = map.view();
    }

    private final byte id;
    private final String name;
    private final @Nullable RockType rockType;

    RockVariant(int id, @Nullable RockType rockType, String name) {
        this.id = (byte) id;
        this.rockType = rockType;
        this.name = name;
    }

    public static RockVariant fromId(byte id) {
        RockVariant variant = REGISTRY.get(id);
        if (variant == null) {
            throw new UnregisteredFeatureException("Unregistered variant for id: " + id);
        }
        return variant;
    }

    @Contract(pure = true)
    public Block fromEnumVanillaRep(VanillaRockVariant vanilla) {
        return switch (vanilla) {
            case COBBLESTONE -> this.get(EvolutionBlocks.COBBLESTONES);
            case GRAVEL -> this.get(EvolutionBlocks.GRAVELS);
            case SAND -> this.get(EvolutionBlocks.SANDS);
            case STONE -> this.get(EvolutionBlocks.STONES);
            case STONE_BRICKS -> this.get(EvolutionBlocks.STONEBRICKS);
        };
    }

    @Override
    public @UnmodifiableView OList<Map<RockVariant, ? extends Block>> getBlocks() {
        return BLOCKS.view();
    }

    public byte getId() {
        return this.id;
    }

    @Contract(pure = true, value = "_ -> new")
    public ItemStack getKnappedStack(KnappingRecipe knapping) {
        return switch (knapping) {
            case NULL -> new ItemStack(this.get(EvolutionItems.ROCKS));
            case AXE -> this.getPart(PartTypes.Head.AXE);
            case HAMMER -> this.getPart(PartTypes.Head.HAMMER);
            case HOE -> this.getPart(PartTypes.Head.HOE);
            case KNIFE -> this.getPart(PartTypes.Blade.KNIFE);
            case SHOVEL -> this.getPart(PartTypes.Head.SHOVEL);
            case SPEAR -> this.getPart(PartTypes.Head.SPEAR);
            case PRIMITIVE_KNIFE -> new ItemStack(this.get(EvolutionItems.PRIMITIVE_KNIVES));
        };
    }

    @Override
    public String getName() {
        return this.name;
    }

    public RockType getRockType() {
        if (this.rockType == null) {
            throw new IllegalStateException("RockVariant " + this + " does not have a RockType");
        }
        return this.rockType;
    }

    @Override
    public void registerBlocks(Map<RockVariant, ? extends Block> blocks) {
        BLOCKS.add(blocks);
    }

    private EvolutionMaterials getMaterial() {
        return switch (this) {
            case ANDESITE -> EvolutionMaterials.ANDESITE;
            case BASALT -> EvolutionMaterials.BASALT;
            case CHERT -> EvolutionMaterials.CHERT;
            case DIORITE -> EvolutionMaterials.DIORITE;
            case GABBRO -> EvolutionMaterials.GABBRO;
            case GNEISS -> EvolutionMaterials.GNEISS;
            case GRANITE -> EvolutionMaterials.GRANITE;
            case LIMESTONE -> EvolutionMaterials.LIMESTONE;
            case MARBLE -> EvolutionMaterials.MARBLE;
            case RED_SANDSTONE -> EvolutionMaterials.RED_SANDSTONE;
            case RHYOLITE -> EvolutionMaterials.RHYOLITE;
            case SANDSTONE -> EvolutionMaterials.SANDSTONE;
            case SCHIST -> EvolutionMaterials.SCHIST;
            case SHALE -> EvolutionMaterials.SHALE;
            case SLATE -> EvolutionMaterials.SLATE;
        };
    }

    @Contract(pure = true, value = "_ -> new")
    private <T extends IPartType<T, I, P>, I extends ItemPart<T, I, P>, P extends IPart<T, I, P>> ItemStack getPart(T type) {
        return type.partItem().newStack(type, this.getMaterial());
    }
}
