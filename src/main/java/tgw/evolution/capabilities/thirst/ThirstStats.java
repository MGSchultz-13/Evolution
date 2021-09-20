package tgw.evolution.capabilities.thirst;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.EffectInstance;
import net.minecraftforge.fml.network.PacketDistributor;
import tgw.evolution.init.EvolutionEffects;
import tgw.evolution.init.EvolutionNetwork;
import tgw.evolution.network.PacketSCThirstData;
import tgw.evolution.patches.IEffectInstancePatch;
import tgw.evolution.util.MathHelper;
import tgw.evolution.util.Time;

public class ThirstStats implements IThirst {

    public static final ThirstStats CLIENT_INSTANCE = new ThirstStats();
    public static final int THIRST_CAPACITY = 3_000;
    public static final int HYDRATION_CAPACITY = 3_000;
    public static final int INTOXICATION = 1_000;
    public static final int INTOXICATION_II = 2_000;
    public static final float THIRST_SCALE = THIRST_CAPACITY / 10.0f;
    public static final float HYDRATION_SCALE = INTOXICATION / 10.0f;
    public static final float DAILY_CONSUMPTION = 2_500.0f;
    /**
     * Bit 0: intoxicated;<br>
     * Bit 1: veryIntoxicated;<br>
     * Bit 2: extremelyIntoxicated;<br>
     * Bit 3: dehydrated;<br>
     * Bit 4: veryDehydrated;<br>
     * Bit 5: extremelyDehydrated;<br>
     */
    private byte flags;
    private float hydrationExhaustion;
    private int hydrationLevel;
    private boolean needsUpdate;
    private float thirstExhaustion;
    private int thirstLevel = THIRST_CAPACITY;

    @Override
    public void addHydrationExhaustion(float exhaustion) {
        this.hydrationExhaustion += exhaustion;
        if (this.hydrationExhaustion >= 1) {
            this.hydrationExhaustion -= 1;
            this.decreaseHydrationLevel();
        }
    }

    @Override
    public void addThirstExhaustion(float exhaustion) {
        this.thirstExhaustion += exhaustion;
        if (this.thirstExhaustion >= 1) {
            this.thirstExhaustion -= 1;
            this.decreaseThirstLevel();
        }
    }

    @Override
    public void decreaseHydrationLevel() {
        if (this.hydrationLevel > 0) {
            this.hydrationLevel--;
            this.needsUpdate = true;
        }
    }

    @Override
    public void decreaseThirstLevel() {
        if (this.thirstLevel > 0) {
            this.thirstLevel--;
            this.needsUpdate = true;
        }
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt) {
        this.setThirstLevel(nbt.getShort("ThirstLevel"));
        this.setHydrationLevel(nbt.getShort("HydrationLevel"));
        this.setThirstExhaustion(nbt.getFloat("ThirstExhaustion"));
        this.setHydrationExhaustion(nbt.getFloat("HydrationExhaustion"));
        this.flags = nbt.getByte("Flags");
    }

    @Override
    public float getHydrationExhaustion() {
        return this.hydrationExhaustion;
    }

    @Override
    public int getHydrationLevel() {
        return this.hydrationLevel;
    }

    @Override
    public float getThirstExhaustion() {
        return this.thirstExhaustion;
    }

    @Override
    public int getThirstLevel() {
        return this.thirstLevel;
    }

    @Override
    public void increaseHydrationLevel(int amount) {
        if (amount > 0 && this.hydrationLevel < HYDRATION_CAPACITY) {
            this.setHydrationLevel(this.hydrationLevel + amount);
        }
    }

    @Override
    public void increaseThirstLevel(int amount) {
        if (amount > 0 && this.thirstLevel < THIRST_CAPACITY) {
            this.setThirstLevel(this.thirstLevel + amount);
        }
    }

    public boolean isDehydrated() {
        return (this.flags & 8) != 0;
    }

    public boolean isExtremelyDehydrated() {
        return (this.flags & 32) != 0;
    }

    public boolean isExtremelyIntoxicated() {
        return (this.flags & 4) != 0;
    }

    public boolean isIntoxicated() {
        return (this.flags & 1) != 0;
    }

    public boolean isVeryDehydrated() {
        return (this.flags & 16) != 0;
    }

    public boolean isVeryIntoxicated() {
        return (this.flags & 2) != 0;
    }

    @Override
    public CompoundNBT serializeNBT() {
        CompoundNBT nbt = new CompoundNBT();
        nbt.putShort("ThirstLevel", (short) this.thirstLevel);
        nbt.putShort("HydrationLevel", (short) this.hydrationLevel);
        nbt.putFloat("ThirstExhaustion", this.thirstExhaustion);
        nbt.putFloat("HydrationExhaustion", this.hydrationExhaustion);
        nbt.putByte("Flags", this.flags);
        return nbt;
    }

    public void setDehydrated(boolean dehydrated) {
        if (dehydrated) {
            this.flags |= 8;
        }
        else {
            this.flags &= ~8;
        }
    }

    public void setExtremelyDehydrated(boolean extremelyDehydrated) {
        if (extremelyDehydrated) {
            this.flags |= 32;
        }
        else {
            this.flags &= ~32;
        }
    }

    public void setExtremelyIntoxicated(boolean extremelyIntoxicated) {
        if (extremelyIntoxicated) {
            this.flags |= 4;
        }
        else {
            this.flags &= ~4;
        }
    }

    @Override
    public void setHydrationExhaustion(float exhaustion) {
        this.hydrationExhaustion = MathHelper.clampMin(exhaustion, 0);
    }

    @Override
    public void setHydrationLevel(int hydration) {
        int old = this.hydrationLevel;
        this.hydrationLevel = MathHelper.clamp(hydration, 0, HYDRATION_CAPACITY);
        if (old != this.hydrationLevel) {
            this.needsUpdate = true;
        }
    }

    public void setIntoxicated(boolean intoxicated) {
        if (intoxicated) {
            this.flags |= 1;
        }
        else {
            this.flags &= ~1;
        }
    }

    @Override
    public void setThirstExhaustion(float exhaustion) {
        this.thirstExhaustion = MathHelper.clampMin(exhaustion, 0);
    }

    @Override
    public void setThirstLevel(int thirstLevel) {
        int old = this.thirstLevel;
        this.thirstLevel = MathHelper.clamp(thirstLevel, 0, THIRST_CAPACITY);
        if (old != this.thirstLevel) {
            this.needsUpdate = true;
        }
    }

    public void setVeryDehydrated(boolean veryDehydrated) {
        if (veryDehydrated) {
            this.flags |= 16;
        }
        else {
            this.flags &= ~16;
        }
    }

    public void setVeryIntoxicated(boolean veryIntoxicated) {
        if (veryIntoxicated) {
            this.flags |= 2;
        }
        else {
            this.flags &= ~2;
        }
    }

    @Override
    public void tick(ServerPlayerEntity player) {
        //TODO influence by temperature also
        if (player.isAlive()) {
            float sprintModifier = player.isSprinting() ? 0.15f : 0.0f;
            float thirstEffectModifier = 0.0f;
            float hydrationModifier = this.hydrationLevel > 0 ? -0.15f : 0.0f;
            if (player.hasEffect(EvolutionEffects.THIRST.get())) {
                EffectInstance effect = player.getEffect(EvolutionEffects.THIRST.get());
                if (effect.getDuration() > 0) {
                    thirstEffectModifier = 0.1f * (effect.getAmplifier() + 1);
                }
            }
            this.addThirstExhaustion(DAILY_CONSUMPTION / Time.DAY_IN_TICKS * (1.0f + sprintModifier + thirstEffectModifier + hydrationModifier));
            this.addHydrationExhaustion(0.9f);
            if (this.thirstLevel <= 0 && !this.isExtremelyDehydrated()) {
                this.setDehydrated(true);
                this.setVeryDehydrated(true);
                this.setExtremelyDehydrated(true);
                player.addEffect(IEffectInstancePatch.newInfinite(EvolutionEffects.DEHYDRATION.get(), 2, false, false, true));
            }
            else if (this.thirstLevel <= 0.1 * THIRST_CAPACITY && !this.isVeryDehydrated()) {
                this.setDehydrated(true);
                this.setVeryDehydrated(true);
                player.addEffect(IEffectInstancePatch.newInfinite(EvolutionEffects.DEHYDRATION.get(), 1, false, false, true));
            }
            else if (this.thirstLevel <= 0.25 * THIRST_CAPACITY && !this.isDehydrated()) {
                this.setDehydrated(true);
                player.addEffect(IEffectInstancePatch.newInfinite(EvolutionEffects.DEHYDRATION.get(), 0, false, false, true));
            }
            else if (this.thirstLevel > 0.25 * THIRST_CAPACITY && this.isDehydrated()) {
                this.setDehydrated(false);
                this.setVeryDehydrated(false);
                this.setExtremelyDehydrated(false);
                player.removeEffect(EvolutionEffects.DEHYDRATION.get());
            }
            if (this.hydrationLevel >= HYDRATION_CAPACITY && !this.isExtremelyIntoxicated()) {
                this.setExtremelyIntoxicated(true);
                this.setVeryIntoxicated(true);
                this.setIntoxicated(true);
                player.addEffect(IEffectInstancePatch.newInfinite(EvolutionEffects.WATER_INTOXICATION.get(), 2, false, false, true));
            }
            else if (this.hydrationLevel >= INTOXICATION_II && !this.isVeryIntoxicated()) {
                this.setVeryIntoxicated(true);
                this.setIntoxicated(true);
                player.addEffect(IEffectInstancePatch.newInfinite(EvolutionEffects.WATER_INTOXICATION.get(), 1, false, false, true));
            }
            else if (this.hydrationLevel >= INTOXICATION && !this.isIntoxicated()) {
                this.setIntoxicated(true);
                player.addEffect(IEffectInstancePatch.newInfinite(EvolutionEffects.WATER_INTOXICATION.get(), 0, false, false, true));
            }
            else if (this.hydrationLevel <= 0 && this.isIntoxicated()) {
                this.setIntoxicated(false);
                this.setVeryIntoxicated(false);
                this.setExtremelyIntoxicated(false);
                player.removeEffect(EvolutionEffects.WATER_INTOXICATION.get());
            }
        }
        else {
            this.setHydrationLevel(0);
            this.setIntoxicated(false);
            this.setVeryIntoxicated(false);
            this.setExtremelyIntoxicated(false);
            this.setDehydrated(false);
            this.setVeryDehydrated(false);
            this.setExtremelyDehydrated(false);
        }
        if (this.needsUpdate) {
            EvolutionNetwork.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new PacketSCThirstData(this));
            this.needsUpdate = false;
        }
    }
}
