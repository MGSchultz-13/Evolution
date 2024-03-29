package tgw.evolution.items.modular.part;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Contract;
import tgw.evolution.capabilities.modular.part.IPart;
import tgw.evolution.capabilities.modular.part.IPartType;
import tgw.evolution.init.EvolutionMaterials;
import tgw.evolution.inventory.SlotType;
import tgw.evolution.items.IDurability;
import tgw.evolution.items.IMass;
import tgw.evolution.items.ItemGeneric;
import tgw.evolution.util.collection.lists.custom.EitherList;

import java.util.Random;

public abstract class ItemPart<T extends IPartType<T, I, P>, I extends ItemPart<T, I, P>, P extends IPart<T, I, P>> extends ItemGeneric implements IDurability, IMass {

    protected static final Random RANDOM = new Random();

    public ItemPart(Properties properties) {
        super(properties);
    }

    protected static boolean verifyStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return stack.hasTag();
    }

    @Override
    public final boolean canBeDepleted() {
        return true;
    }

    public abstract P createNew();

    @Override
    public final void fillItemCategory(CreativeModeTab tab, NonNullList<ItemStack> items) {
        if (this.allowdedIn(tab)) {
            for (T t : this.iterable()) {
                for (EvolutionMaterials material : EvolutionMaterials.VALUES) {
                    if (t.hasVariantIn(material)) {
                        //noinspection ObjectAllocationInLoop
                        items.add(this.newStack(t, material));
                    }
                }
            }
        }
    }

    @Override
    public final Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        this.putMassAttributes(builder, stack, SlotType.byEquipment(slot));
        return builder.build();
    }

    @Override
    public final int getBarColor(ItemStack stack) {
        int maxDamage = stack.getMaxDamage();
        float f = Math.max(0.0F, (maxDamage - stack.getDamageValue()) / (float) maxDamage);
        return Mth.hsvToRgb(f / 3.0F, 1.0F, 1.0F);
    }

    @Override
    public final int getBarWidth(ItemStack stack) {
        return Math.round(13.0F - stack.getDamageValue() * 13.0F / stack.getMaxDamage());
    }

    protected abstract String getCapName();

    @Override
    public final int getDamage(ItemStack stack) {
        if (!this.canBeDepleted()) {
            return 0;
        }
        if (!verifyStack(stack)) {
            return 0;
        }
        //noinspection ConstantConditions
        return this.getPartCap().getDurabilityDmg(stack.getTag());
    }

    @Override
    public final String getDescriptionId(ItemStack stack) {
        if (!verifyStack(stack)) {
            return "null";
        }
        //noinspection ConstantConditions
        return this.getPartCap().getDescriptionId(stack.getTag());
    }

    @Override
    public final double getMass(ItemStack stack) {
        if (!verifyStack(stack)) {
            return 0;
        }
        //noinspection ConstantConditions
        return this.getPartCap().getMass(stack.getTag());
    }

    @Override
    public final int getMaxDamage(ItemStack stack) {
        if (!this.canBeDepleted()) {
            return 0;
        }
        if (!verifyStack(stack)) {
            return 0;
        }
        //noinspection ConstantConditions
        return this.getPartCap().getMaxDamage(stack.getTag());
    }

    protected abstract P getPartCap();

    protected abstract T[] iterable();

    public final void makeTooltip(EitherList<FormattedText, TooltipComponent> tooltip, ItemStack stack, int num) {
        P partCap = this.getPartCap();
        partCap.appendText(tooltip, num);
    }

    @Contract(pure = true, value = "_, _ -> new")
    public final ItemStack newStack(T type, EvolutionMaterials material) {
        ItemStack stack = new ItemStack(this);
        this.getPartCap().set(stack, type, material);
        return stack;
    }
}
