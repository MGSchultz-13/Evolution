package tgw.evolution.capabilities.thirst;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.EffectInstance;
import net.minecraftforge.fml.network.PacketDistributor;
import tgw.evolution.init.EvolutionDamage;
import tgw.evolution.init.EvolutionEffects;
import tgw.evolution.init.EvolutionNetwork;
import tgw.evolution.network.PacketSCThirstData;
import tgw.evolution.potion.InfiniteEffectInstance;
import tgw.evolution.util.MathHelper;

public class ThirstStats implements IThirst {

    public static final ThirstStats CLIENT_INSTANCE = new ThirstStats();
    public static final int THIRST_CAPACITY = 3_000;
    public static final int HYDRATION_CAPACITY = 3_000;
    public static final int INTOXICATION = 1_000;
    public static final int INTOXICATION_II = 2_000;
    public static final float THIRST_SCALE = THIRST_CAPACITY / 10.0f;
    public static final float HYDRATION_SCALE = INTOXICATION / 10.0f;
    private float hydrationExhaustion;
    private int hydrationLevel;
    private byte intoxicated;
    private boolean needsUpdate;
    private float thirstExhaustion;
    private int thirstLevel = THIRST_CAPACITY;
    private int timer;

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
        this.timer = nbt.getByte("Timer");
        this.intoxicated = nbt.getByte("Intoxicated");
    }

    @Override
    public float getHydrationExhaustion() {
        return this.hydrationExhaustion;
    }

    @Override
    public void setHydrationExhaustion(float exhaustion) {
        this.hydrationExhaustion = MathHelper.clampMin(exhaustion, 0);
    }

    @Override
    public int getHydrationLevel() {
        return this.hydrationLevel;
    }

    @Override
    public void setHydrationLevel(int hydration) {
        this.hydrationLevel = MathHelper.clamp(hydration, 0, HYDRATION_CAPACITY);
        this.needsUpdate = true;
    }

    @Override
    public float getThirstExhaustion() {
        return this.thirstExhaustion;
    }

    @Override
    public void setThirstExhaustion(float exhaustion) {
        this.thirstExhaustion = MathHelper.clampMin(exhaustion, 0);
    }

    @Override
    public int getThirstLevel() {
        return this.thirstLevel;
    }

    @Override
    public void setThirstLevel(int thirstLevel) {
        this.thirstLevel = MathHelper.clamp(thirstLevel, 0, THIRST_CAPACITY);
        this.needsUpdate = true;
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

    public boolean isExtremelyIntoxicated() {
        return (this.intoxicated >> 2 & 1) == 1;
    }

    public void setExtremelyIntoxicated(boolean extremelyIntoxicated) {
        if (extremelyIntoxicated != this.isExtremelyIntoxicated()) {
            this.intoxicated ^= 1 << 2;
        }
    }

    public boolean isIntoxicated() {
        return (this.intoxicated & 1) == 1;
    }

    public void setIntoxicated(boolean intoxicated) {
        if (intoxicated != this.isIntoxicated()) {
            this.intoxicated ^= 1;
        }
    }

    public boolean isVeryIntoxicated() {
        return (this.intoxicated >> 1 & 1) == 1;
    }

    public void setVeryIntoxicated(boolean veryIntoxicated) {
        if (veryIntoxicated != this.isVeryIntoxicated()) {
            this.intoxicated ^= 1 << 1;
        }
    }

    @Override
    public CompoundNBT serializeNBT() {
        CompoundNBT nbt = new CompoundNBT();
        nbt.putShort("ThirstLevel", (short) this.thirstLevel);
        nbt.putShort("HydrationLevel", (short) this.hydrationLevel);
        nbt.putFloat("ThirstExhaustion", this.thirstExhaustion);
        nbt.putFloat("HydrationExhaustion", this.hydrationExhaustion);
        nbt.putByte("Timer", (byte) this.timer);
        nbt.putByte("IsIntoxicated", this.intoxicated);
        return nbt;
    }

    @Override
    public void tick(ServerPlayerEntity player) {
        //TODO influence by temperature also
        if (player.isAlive()) {
            float sprintModifier = player.isSprinting() ? 1.15f : 1.0f;
            float thirstEffectModifier = 1.0f;
            if (player.isPotionActive(EvolutionEffects.THIRST.get())) {
                EffectInstance effect = player.getActivePotionEffect(EvolutionEffects.THIRST.get());
                if (effect.getDuration() > 0) {
                    thirstEffectModifier += 0.05f * (effect.getAmplifier() + 1);
                }
            }
            this.addThirstExhaustion(0.104_166_667f * sprintModifier * thirstEffectModifier);
            this.addHydrationExhaustion(0.9f);
            if (this.thirstLevel <= 0) {
                this.timer++;
                if (this.timer >= 80) {
                    player.attackEntityFrom(EvolutionDamage.DEHYDRATION, 1.0F);
                    this.timer = 0;
                }
            }
            else {
                this.timer = 0;
            }
            if (this.hydrationLevel >= HYDRATION_CAPACITY && !this.isExtremelyIntoxicated()) {
                this.setExtremelyIntoxicated(true);
                this.setVeryIntoxicated(true);
                this.setIntoxicated(true);
                player.addPotionEffect(new InfiniteEffectInstance(EvolutionEffects.WATER_INTOXICATION.get(), 2, false, false, true));
            }
            else if (this.hydrationLevel >= INTOXICATION_II && !this.isVeryIntoxicated()) {
                this.setVeryIntoxicated(true);
                this.setIntoxicated(true);
                player.addPotionEffect(new InfiniteEffectInstance(EvolutionEffects.WATER_INTOXICATION.get(), 1, false, false, true));
            }
            else if (this.hydrationLevel >= INTOXICATION && !this.isIntoxicated()) {
                this.setIntoxicated(true);
                player.addPotionEffect(new InfiniteEffectInstance(EvolutionEffects.WATER_INTOXICATION.get(), 0, false, false, true));
            }
            else if (this.isIntoxicated() && this.hydrationLevel <= 0) {
                this.setIntoxicated(false);
                this.setVeryIntoxicated(false);
                this.setExtremelyIntoxicated(false);
                player.removePotionEffect(EvolutionEffects.WATER_INTOXICATION.get());
            }
        }
        else {
            this.setHydrationLevel(0);
            this.setIntoxicated(false);
            this.setVeryIntoxicated(false);
            this.setExtremelyIntoxicated(false);
        }
        if (this.needsUpdate) {
            EvolutionNetwork.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new PacketSCThirstData(this));
            this.needsUpdate = false;
        }
    }
}