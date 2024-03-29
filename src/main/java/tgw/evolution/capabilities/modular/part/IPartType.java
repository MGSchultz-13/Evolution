package tgw.evolution.capabilities.modular.part;

import net.minecraft.network.chat.Component;
import tgw.evolution.init.EvolutionMaterials;
import tgw.evolution.items.modular.part.ItemPart;

public interface IPartType<T extends IPartType<T, I, P>, I extends ItemPart<T, I, P>, P extends IPart<T, I, P>> {

    boolean canBeSharpened();

    Component getComponent();

    byte getId();

    String getName();

    double getVolume(EvolutionMaterials material);

    boolean hasVariantIn(EvolutionMaterials material);

    default String modelSuffix(EvolutionMaterials material) {
        return "";
    }

    I partItem();
}
