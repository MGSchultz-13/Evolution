package tgw.evolution.client.gui.advancements;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import tgw.evolution.client.renderer.RenderHelper;
import tgw.evolution.util.math.MathHelper;

import java.util.LinkedHashMap;
import java.util.Map;

public class GuiAdvancementTab extends GuiComponent {

    protected final Map<Advancement, GuiAdvancementEntry> guis = new LinkedHashMap<>();
    private final Advancement advancement;
    private final DisplayInfo display;
    private final ItemStack icon;
    private final int index;
    private final Minecraft minecraft;
    private final GuiAdvancementEntry root;
    private final ScreenAdvancements screen;
    private final Component title;
    private final AdvancementTabType type;
    protected int scrollX;
    protected int scrollY;
    private boolean centered;
    private float fade;
    private int maxX = Integer.MIN_VALUE;
    private int maxY = Integer.MIN_VALUE;
    private int minX = Integer.MAX_VALUE;
    private int minY = Integer.MAX_VALUE;

    public GuiAdvancementTab(Minecraft mc,
                             ScreenAdvancements screenAdvancements,
                             AdvancementTabType type,
                             int index,
                             Advancement advancement,
                             DisplayInfo displayInfo) {
        this.minecraft = mc;
        this.screen = screenAdvancements;
        this.type = type;
        this.index = index;
        this.advancement = advancement;
        this.display = displayInfo;
        this.icon = displayInfo.getIcon();
        this.title = displayInfo.getTitle();
        this.root = new GuiAdvancementEntry(this, mc, advancement, displayInfo);
        this.addGuiAdvancement(this.root, advancement);
    }

    public static @Nullable GuiAdvancementTab create(Minecraft mc,
                                                     ScreenAdvancements screenAdvancements,
                                                     int index,
                                                     Advancement advancement,
                                                     int width,
                                                     int height) {
        if (advancement.getDisplay() == null) {
            return null;
        }
        AdvancementTabType tabType = AdvancementTabType.getTabType(width, height, index);
        if (tabType == null) {
            return null;
        }
        return new GuiAdvancementTab(mc, screenAdvancements, tabType, index, advancement, advancement.getDisplay());
    }

    public void addAdvancement(Advancement advancement) {
        if (advancement.getDisplay() != null) {
            GuiAdvancementEntry advancementEntry = new GuiAdvancementEntry(this, this.minecraft, advancement, advancement.getDisplay());
            this.addGuiAdvancement(advancementEntry, advancement);
        }
    }

    private void addGuiAdvancement(GuiAdvancementEntry advancementEntry, Advancement advancement) {
        this.guis.put(advancement, advancementEntry);
        int left = advancementEntry.getX();
        int top = advancementEntry.getY();
        this.minX = Math.min(this.minX, left);
        int right = left + 28;
        this.maxX = Math.max(this.maxX, right);
        this.minY = Math.min(this.minY, top);
        int bottom = top + 27;
        this.maxY = Math.max(this.maxY, bottom);
        for (GuiAdvancementEntry gui : this.guis.values()) {
            gui.attachToParent();
        }
    }

    public void drawContents(PoseStack matrices, int width, int height) {
        if (!this.centered) {
            this.scrollX = (width - (this.maxX + this.minX)) / 2;
            this.scrollY = (height - (this.maxY + this.minY)) / 2;
            this.centered = true;
        }
        matrices.pushPose();
        matrices.translate(0, 0, 950);
        RenderSystem.enableDepthTest();
        RenderSystem.colorMask(false, false, false, false);
        fill(matrices, 4_680, 2_260, -4_680, -2_260, 0xff00_0000);
        RenderSystem.colorMask(true, true, true, true);
        matrices.translate(0, 0, -950);
        RenderSystem.depthFunc(GL11.GL_GEQUAL);
        fill(matrices, width, height, 0, 0, 0xff00_0000);
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        ResourceLocation background = this.display.getBackground();
        RenderSystem.setShader(RenderHelper.SHADER_POSITION_TEX);
        if (background != null) {
            RenderSystem.setShaderTexture(0, background);
        }
        else {
            RenderSystem.setShaderTexture(0, TextureManager.INTENTIONAL_MISSING_TEXTURE);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int i = this.scrollX % 16;
        int j = this.scrollY % 16;
        for (int k = -1; k <= 1 + width / 16; k++) {
            int l = -1;
            for (; l <= height / 16; l++) {
                blit(matrices, i + 16 * k, j + 16 * l, 0.0F, 0.0F, 16, 16, 16, 16);
            }
            blit(matrices, i + 16 * k, j + 16 * l, 0.0F, 0.0F, 16, height % 16, 16, 16);
        }
        this.root.drawConnectivity(matrices, this.scrollX, this.scrollY, true);
        this.root.drawConnectivity(matrices, this.scrollX, this.scrollY, false);
        this.root.draw(matrices, this.scrollX, this.scrollY);
        RenderSystem.depthFunc(GL11.GL_GEQUAL);
        matrices.translate(0, 0, -950);
        RenderSystem.colorMask(false, false, false, false);
        fill(matrices, 4_680, 2_260, -4_680, -2_260, 0xff00_0000);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        matrices.popPose();
    }

    public void drawIcon(int left, int top, int width, int height, ItemRenderer renderItem) {
        this.type.drawIcon(left, top, width, height, this.index, renderItem, this.icon);
    }

    public void drawTab(PoseStack matrices, int left, int top, int width, int height, boolean selected) {
        this.type.draw(matrices, this, left, top, width, height, selected, this.index);
    }

    public void drawToolTips(PoseStack matrices, int mouseX, int mouseY, int left, int top, int width, int height) {
        fill(matrices, 0, 0, width, height, Mth.floor(this.fade * 255.0F) << 24);
        boolean flag = false;
        if (mouseX > 0 && mouseX < width && mouseY > 0 && mouseY < height) {
            for (GuiAdvancementEntry betterAdvancementEntryGui : this.guis.values()) {
                if (betterAdvancementEntryGui.isMouseOver(this.scrollX, this.scrollY, mouseX, mouseY)) {
                    flag = true;
                    betterAdvancementEntryGui.drawHover(matrices, this.scrollX, this.scrollY, left, top);
                    break;
                }
            }
        }
        if (flag) {
            this.fade = MathHelper.clamp(this.fade + 0.02F, 0.0F, 0.3F);
        }
        else {
            this.fade = MathHelper.clamp(this.fade - 0.04F, 0.0F, 1.0F);
        }
    }

    public Advancement getAdvancement() {
        return this.advancement;
    }

    public @Nullable GuiAdvancementEntry getAdvancementGui(Advancement advancement) {
        return this.guis.get(advancement);
    }

    public ScreenAdvancements getScreen() {
        return this.screen;
    }

    public Component getTitle() {
        return this.title;
    }

    public boolean isMouseOver(int left, int top, int width, int height, double mouseX, double mouseY) {
        return this.type.isMouseOver(left, top, width, height, this.index, mouseX, mouseY);
    }

    public void scroll(double scrollX, double scrollY, int width, int height) {
        if (this.maxX - this.minX > width) {
            this.scrollX = (int) Math.round(MathHelper.clamp(this.scrollX + scrollX, -(this.maxX - width), -this.minX));
        }
        if (this.maxY - this.minY > height) {
            this.scrollY = (int) Math.round(MathHelper.clamp(this.scrollY + scrollY, -(this.maxY - height), -this.minY));
        }
    }
}
