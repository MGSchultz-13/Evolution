package tgw.evolution.client.models.item.part;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelDataMap;
import tgw.evolution.capabilities.modular.part.HiltPart;
import tgw.evolution.capabilities.modular.part.PartTypes;
import tgw.evolution.init.EvolutionResources;
import tgw.evolution.init.ItemMaterial;

public class BakedModelPartHilt extends BakedModelPart<PartTypes.Hilt, HiltPart, BakedModelPartHilt.BakedModelFinalPartHilt> {

    public BakedModelPartHilt(BakedModel baseModel) {
        super(baseModel, new ItemOverridesPartHilt(new BakedModelFinalPartHilt(baseModel)));
    }

    public static class ItemOverridesPartHilt extends ItemOverridesPart<PartTypes.Hilt, HiltPart, BakedModelFinalPartHilt> {

        public ItemOverridesPartHilt(BakedModelFinalPartHilt finalModel) {
            super(finalModel, HiltPart.DUMMY);
        }

        @Override
        protected void setModelData(HiltPart part) {
            this.finalModel.setData(part.getType(), part.getMaterial().getMaterial());
        }
    }

    public static class BakedModelFinalPartHilt extends BakedModelFinalPart<PartTypes.Hilt> {
        public BakedModelFinalPartHilt(BakedModel baseModel) {
            super(baseModel, PartTypes.Hilt.NULL);
        }

        @Override
        protected void appendToEmptyModelData(ModelDataMap.Builder builder) {
        }

        @Override
        protected ModelResourceLocation getModel(IModelData extraData) {
            return EvolutionResources.MODULAR_HILTS.get(extraData.getData(this.type), extraData.getData(this.material));
        }

        protected void setData(PartTypes.Hilt type, ItemMaterial material) {
            this.modelData.setData(this.type, type);
            this.modelData.setData(this.material, material);
        }
    }
}