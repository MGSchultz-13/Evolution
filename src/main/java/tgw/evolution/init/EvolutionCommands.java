package tgw.evolution.init;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import tgw.evolution.commands.*;

public final class EvolutionCommands {

    private EvolutionCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        CommandAtm.register(dispatcher);
        CommandCamera.register(dispatcher);
        CommandDate.register(dispatcher);
        CommandGC.register(dispatcher);
        CommandHeal.register(dispatcher);
        CommandIntegrity.register(dispatcher);
        CommandLoadFactor.register(dispatcher);
        CommandPause.register(dispatcher);
        CommandRegen.register(dispatcher);
        CommandShader.register(dispatcher);
        CommandTemperature.register(dispatcher);
        CommandTickrate.register(dispatcher);
        CommandVariantDebug.register(dispatcher);
    }
}
