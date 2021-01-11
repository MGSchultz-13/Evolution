package tgw.evolution.items;

import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import tgw.evolution.blocks.fluids.FluidGeneric;
import tgw.evolution.init.EvolutionItems;

import java.util.function.Supplier;

public class ItemBucketCreative extends ItemGenericBucket {

    public ItemBucketCreative(Supplier<? extends Fluid> fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    public ItemStack emptyBucket() {
        return new ItemStack(EvolutionItems.bucket_creative_empty.get());
    }

    @Override
    public int getAmount(ItemStack stack) {
        if (!stack.hasTag()) {
            return 0;
        }
        return stack.getTag().getInt("Amount");
    }

    @Override
    public Item getFullBucket(Fluid fluid) {
        if (fluid instanceof FluidGeneric) {
            switch (((FluidGeneric) fluid).getId()) {
                case FluidGeneric.FRESH_WATER:
                    return EvolutionItems.bucket_creative_fresh_water.get();
                case FluidGeneric.SALT_WATER:
                    return EvolutionItems.bucket_creative_salt_water.get();
            }
        }
        return EvolutionItems.bucket_creative_empty.get();
    }

    @Override
    public int getMaxAmount() {
        return 100_000;
    }
}
