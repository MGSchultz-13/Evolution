package tgw.evolution.client.gui;

import net.minecraft.client.gui.screen.inventory.CreativeScreen;
import net.minecraft.inventory.container.Slot;

public class GuiContainerCreativeHandler extends GuiContainerHandler {

    public GuiContainerCreativeHandler(CreativeScreen guiContainerCreative) {
        super(guiContainerCreative);
    }

    @Override
    public boolean isIgnored(Slot slot) {
        return super.isIgnored(slot) || slot.inventory != this.mc.player.inventory;
    }
}
