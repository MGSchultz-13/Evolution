package tgw.evolution.init;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import tgw.evolution.Evolution;

public enum EvolutionArmorMaterials implements ArmorMaterial {
    generic("placeholder", 400, new int[]{8, 10, 9, 7}, 25, EvolutionItems.debug_item.get(), "entity.ender_dragon.growl", 0.0f, 0.0f);

    private static final int[] MAX_DAMAGE_ARRAY = {13, 15, 16, 11};
    private final int[] damageReduction;
    private final int durability;
    private final int enchantability;
    private final String equipSound;
    private final float knockbackResistance;
    private final String name;
    private final Item repairItem;
    private final float toughness;

    EvolutionArmorMaterials(String name,
                            int durability,
                            int[] damageReductionAmount,
                            int enchantability,
                            Item repairItem,
                            String equipSound,
                            float toughness,
                            float knockbackResistance) {
        this.name = name;
        this.equipSound = equipSound;
        this.durability = durability;
        this.enchantability = enchantability;
        this.damageReduction = damageReductionAmount;
        this.repairItem = repairItem;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
    }

    @Override
    public int getDefenseForSlot(EquipmentSlot slot) {
        return this.damageReduction[slot.getIndex()];
    }

    @Override
    public int getDurabilityForSlot(EquipmentSlot slot) {
        return MAX_DAMAGE_ARRAY[slot.getIndex()] * this.durability;
    }

    @Override
    public int getEnchantmentValue() {
        return this.enchantability;
    }

    @Override
    public SoundEvent getEquipSound() {
        return new SoundEvent(new ResourceLocation(this.equipSound));
    }

    @Override
    public float getKnockbackResistance() {
        return this.knockbackResistance;
    }

    @Override
    public String getName() {
        return Evolution.MODID + ":" + this.name;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(this.repairItem);
    }

    @Override
    public float getToughness() {
        return this.toughness;
    }
}
