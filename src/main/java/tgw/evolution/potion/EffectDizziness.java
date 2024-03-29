package tgw.evolution.potion;

import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.player.Player;
import tgw.evolution.util.collection.maps.I2IHashMap;
import tgw.evolution.util.collection.maps.I2IMap;

public class EffectDizziness extends EffectGeneric {

    private static final I2IMap AFFECTED = new I2IHashMap();

    public EffectDizziness() {
        super(MobEffectCategory.HARMFUL, 0x3a_5785);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int lvl) {
        if (entity.level.isClientSide) {
            return;
        }
        if (entity instanceof Player) {
            return;
        }
        int tick = AFFECTED.getOrDefault(entity.getId(), 0);
        entity.zza = Math.signum(Mth.cos(tick * Mth.TWO_PI / (80 >> lvl)));
        entity.setSprinting(false);
        AFFECTED.put(entity.getId(), ++tick);
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributes, int amplifier) {
        AFFECTED.remove(entity.getId());
        super.removeAttributeModifiers(entity, attributes, amplifier);
    }

    @Override
    public int tickInterval(int lvl) {
        return 1;
    }
}
