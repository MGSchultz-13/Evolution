package tgw.evolution.capabilities.toast;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import tgw.evolution.util.InjectionUtil;

import javax.annotation.Nullable;

public final class CapabilityToast {

    @CapabilityInject(IToastData.class)
    public static final Capability<IToastData> INSTANCE = InjectionUtil.Null();

    private CapabilityToast() {
    }

    public static void register() {
        CapabilityManager.INSTANCE.register(IToastData.class, new Capability.IStorage<IToastData>() {

            @Override
            public void readNBT(Capability<IToastData> capability, IToastData handler, Direction side, INBT nbt) {
                handler.deserializeNBT((CompoundNBT) nbt);
            }

            @Nullable
            @Override
            public INBT writeNBT(Capability<IToastData> capability, IToastData handler, Direction side) {
                return handler.serializeNBT();
            }

        }, () -> {
            throw new IllegalStateException("Could not register CapabilityToast");
        });
    }
}