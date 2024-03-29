package tgw.evolution.init;

import net.minecraft.core.Registry;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import tgw.evolution.Evolution;
import tgw.evolution.util.collection.lists.OArrayList;
import tgw.evolution.util.collection.lists.OList;

public final class EvolutionBlockTags {

    public static final OList<TagKey<Block>> ALL;

    public static final TagKey<Block> BLOCKS_COMBINED_STEP_PARTICLE;
    public static final TagKey<Block> BLOCKS_COMBINED_STEP_SOUND;
    public static final TagKey<Block> COBBLESTONES;
    public static final TagKey<Block> ROCKS;

    static {
        OList<TagKey<Block>> list = new OArrayList<>();
        BLOCKS_COMBINED_STEP_PARTICLE = register(list, "blocks_combined_step_particle");
        BLOCKS_COMBINED_STEP_SOUND = register(list, "blocks_combined_step_sound");
        COBBLESTONES = register(list, "cobblestones");
        ROCKS = register(list, "rocks");
        list.trim();
        ALL = list.view();
    }

    private EvolutionBlockTags() {
    }

    private static TagKey<Block> register(OList<TagKey<Block>> registry, String name) {
        TagKey<Block> tag = TagKey.create(Registry.BLOCK_REGISTRY, Evolution.getResource(name));
        registry.add(tag);
        return tag;
    }
}
