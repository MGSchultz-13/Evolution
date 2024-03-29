package tgw.evolution.client.gui.stats;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.achievement.StatsUpdateListener;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.stats.Stats;
import net.minecraft.stats.StatsCounter;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;
import tgw.evolution.Evolution;
import tgw.evolution.client.gui.GUIUtils;
import tgw.evolution.client.renderer.RenderHelper;
import tgw.evolution.init.EvolutionDamage;
import tgw.evolution.init.EvolutionItems;
import tgw.evolution.init.EvolutionStats;
import tgw.evolution.init.EvolutionTexts;
import tgw.evolution.items.ItemBlock;
import tgw.evolution.stats.IEvoStatFormatter;
import tgw.evolution.util.collection.lists.OArrayList;
import tgw.evolution.util.collection.lists.OList;
import tgw.evolution.util.collection.maps.R2OHashMap;
import tgw.evolution.util.collection.maps.R2OMap;
import tgw.evolution.util.collection.sets.RHashSet;
import tgw.evolution.util.collection.sets.RSet;

import java.util.Comparator;
import java.util.Map;

public class ScreenStats extends Screen implements StatsUpdateListener {

    private final R2OMap<Item, ItemStack> cachedModularItems = new R2OHashMap<>();
    private ListDamageStats damageStats;
    private ListDeathStats deathStats;
    private int displayId;
    private @Nullable ObjectSelectionList<?> displaySlot;
    private ListDistanceStats distanceStats;
    private boolean doesGuiPauseGame = true;
    private ListCustomStats generalStats;
    private ListStats itemStats;
    private ListMobStats mobStats;
    private byte refreshCacheCooldown;
    private final ResourceLocation resDamageIcons = Evolution.getResource("textures/gui/damage_icons.png");
    private final ResourceLocation resIcons = Evolution.getResource("textures/gui/stats_icons.png");
    private final StatsCounter stats;
    private final Component textDamageButton = new TranslatableComponent("evolution.gui.stats.damageButton");
    private final Component textDamageDealt = new TranslatableComponent("evolution.gui.stats.damageDealt");
    private final Component textDamageResisted = new TranslatableComponent("evolution.gui.stats.damageResisted");
    private final Component textDamageTaken = new TranslatableComponent("evolution.gui.stats.damageTaken");
    private final Component textDeathButton = new TranslatableComponent("evolution.gui.stats.deathButton");
    private final Component textDistanceButton = new TranslatableComponent("evolution.gui.stats.distanceButton");
    private final Component textGeneralButton = new TranslatableComponent("evolution.gui.stats.generalButton");
    private final Component textItemsButton = new TranslatableComponent("evolution.gui.stats.itemsButton");
    private final Component textMobButton = new TranslatableComponent("evolution.gui.stats.mobsButton");
    private final Component textTimeButton = new TranslatableComponent("evolution.gui.stats.timeButton");
    private ListTimeStats timeStats;

    public ScreenStats(StatsCounter statsCounter) {
        super(new TranslatableComponent("gui.stats"));
        this.stats = statsCounter;
        this.cachedModularItems.put(EvolutionItems.MODULAR_TOOL, EvolutionItems.MODULAR_TOOL.getDefaultInstance());
        this.cachedModularItems.put(EvolutionItems.PART_BLADE, EvolutionItems.PART_BLADE.getDefaultInstance());
        this.cachedModularItems.put(EvolutionItems.PART_GUARD, EvolutionItems.PART_GUARD.getDefaultInstance());
        this.cachedModularItems.put(EvolutionItems.PART_HALFHEAD, EvolutionItems.PART_HALFHEAD.getDefaultInstance());
        this.cachedModularItems.put(EvolutionItems.PART_HANDLE, EvolutionItems.PART_HANDLE.getDefaultInstance());
        this.cachedModularItems.put(EvolutionItems.PART_HEAD, EvolutionItems.PART_HEAD.getDefaultInstance());
        this.cachedModularItems.put(EvolutionItems.PART_GRIP, EvolutionItems.PART_GRIP.getDefaultInstance());
        this.cachedModularItems.put(EvolutionItems.PART_POLE, EvolutionItems.PART_POLE.getDefaultInstance());
        this.cachedModularItems.put(EvolutionItems.PART_POMMEL, EvolutionItems.PART_POMMEL.getDefaultInstance());
        this.cachedModularItems.trim();
        this.refreshCacheCooldown = 30;
    }

    public static String getFormattedName(Stat<ResourceLocation> stat) {
        return I18n.get("stat." + stat.getValue().toString().replace(':', '.'));
    }

    private static int getCategoryOffset(int category) {
        return 115 + 40 * category;
    }

    public ObjectSelectionList<?> byId(int displayId) {
        return switch (displayId) {
            case 0 -> this.generalStats;
            case 1 -> this.itemStats;
            case 2 -> this.mobStats;
            case 3 -> this.distanceStats;
            case 4 -> this.timeStats;
            case 5 -> this.deathStats;
            case 6 -> this.damageStats;
            default -> throw new IllegalStateException("Unknown display id: " + displayId);
        };
    }

    public @Nullable ObjectSelectionList<?> getDisplaySlot() {
        return this.displaySlot;
    }

    public void initButtons() {
        this.addRenderableWidget(new Button(this.width / 2 - 160, this.height - 52, 80, 20, this.textGeneralButton, button -> this.setDisplaySlot(0)));
        Button itemButton = this.addRenderableWidget(new Button(this.width / 2 - 80, this.height - 52, 80, 20, this.textItemsButton, button -> this.setDisplaySlot(1)));
        Button mobButton = this.addRenderableWidget(new Button(this.width / 2, this.height - 52, 80, 20, this.textMobButton, button -> this.setDisplaySlot(2)));
        this.addRenderableWidget(new Button(this.width / 2 + 80, this.height - 52, 80, 20, this.textDistanceButton, button -> this.setDisplaySlot(3)));
        this.addRenderableWidget(new Button(this.width / 2 - 120, this.height - 32, 80, 20, this.textTimeButton, button -> this.setDisplaySlot(4)));
        this.addRenderableWidget(new Button(this.width / 2 - 40, this.height - 32, 80, 20, this.textDeathButton, button -> this.setDisplaySlot(5)));
        this.addRenderableWidget(new Button(this.width / 2 + 40, this.height - 32, 80, 20, this.textDamageButton, button -> this.setDisplaySlot(6)));
        if (this.itemStats.children().isEmpty()) {
            itemButton.active = false;
        }
        if (this.mobStats.children().isEmpty()) {
            mobButton.active = false;
        }
    }

    public void initLists() {
        assert this.minecraft != null;
        this.generalStats = new ListCustomStats(this.minecraft);
        this.itemStats = new ListStats(this.minecraft);
        this.mobStats = new ListMobStats(this.minecraft);
        this.distanceStats = new ListDistanceStats(this.minecraft);
        this.timeStats = new ListTimeStats(this.minecraft);
        this.deathStats = new ListDeathStats(this.minecraft);
        this.damageStats = new ListDamageStats(this.minecraft);
    }

    @Override
    public boolean isPauseScreen() {
        return !this.doesGuiPauseGame;
    }

    @Override
    public void onStatsUpdated() {
        if (this.doesGuiPauseGame) {
            this.initLists();
            this.initButtons();
            this.setDisplaySlot(this.displayId);
            this.doesGuiPauseGame = false;
        }
    }

    @Override
    public void render(PoseStack matrices, int mouseX, int mouseY, float partialTicks) {
        if (this.doesGuiPauseGame) {
            this.renderBackground(matrices);
            drawCenteredString(matrices, this.font, I18n.get("multiplayer.downloadingStats"), this.width / 2, this.height / 2, 0xff_ffff);
            drawCenteredString(matrices,
                               this.font,
                               LOADING_SYMBOLS[(int) (Util.getMillis() / 150L % LOADING_SYMBOLS.length)],
                               this.width / 2,
                               this.height / 2 + 9 * 2,
                               0xff_ffff
            );
        }
        else {
            if (this.displaySlot != null) {
                this.displaySlot.render(matrices, mouseX, mouseY, partialTicks);
            }
            drawCenteredString(matrices, this.font, this.title, this.width / 2, 20, 0xff_ffff);
            super.render(matrices, mouseX, mouseY, partialTicks);
        }
    }

    public void setDisplaySlot(int displayId) {
        this.removeWidget(this.generalStats);
        this.removeWidget(this.itemStats);
        this.removeWidget(this.mobStats);
        this.removeWidget(this.distanceStats);
        this.removeWidget(this.timeStats);
        this.removeWidget(this.deathStats);
        this.removeWidget(this.damageStats);
        if (displayId != -1) {
            ObjectSelectionList<?> displaySlot = this.byId(displayId);
            this.addWidget(displaySlot);
            this.displaySlot = displaySlot;
            this.displayId = displayId;
        }
    }

    @Override
    public void tick() {
        if (this.displaySlot == this.itemStats) {
            if (this.refreshCacheCooldown == 0) {
                this.refreshCachedItems();
            }
            else if (this.refreshCacheCooldown > 0) {
                this.refreshCacheCooldown--;
            }
        }
        super.tick();
    }

    @Override
    protected void init() {
        this.doesGuiPauseGame = true;
        assert this.minecraft != null;
        ClientPacketListener connection = this.minecraft.getConnection();
        assert connection != null;
        connection.send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.REQUEST_STATS));
    }

    private void blitSlot(PoseStack matrices, int x, int y, Item item) {
        this.blitSlotIcon(matrices, x + 1, y + 1, 0, 0);
        ItemStack stack = this.cachedModularItems.get(item);
        if (stack == null) {
            stack = item.getDefaultInstance();
        }
        this.itemRenderer.renderGuiItem(stack, x + 2, y + 2);
    }

    private void blitSlotIcon(PoseStack matrices, int x, int y, int u, int v) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShader(RenderHelper.SHADER_POSITION_TEX);
        RenderSystem.setShaderTexture(0, this.resIcons);
        blit(matrices, x, y, this.getBlitOffset(), u, v, 18, 18, 256, 128);
    }

    private void drawDamageSprite(PoseStack matrices, int x, int y, EvolutionDamage.Type type) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, this.resIcons);
        blit(matrices, x, y, this.getBlitOffset(), 0, 0, 18, 18, 256, 128);
        RenderSystem.enableBlend();
        RenderSystem.setShaderTexture(0, this.resDamageIcons);
        blit(matrices, x + 1, y + 1, this.getBlitOffset(), type.getTexX() * 16, type.getTexY() * 16, 16, 16, 128, 128);
        RenderSystem.disableBlend();
    }

    private void refreshCachedItems() {
        for (Item item : this.cachedModularItems.keySet()) {
            this.cachedModularItems.put(item, item.getDefaultInstance());
        }
        this.refreshCacheCooldown = 30;
    }

    class ListCustomStats extends ObjectSelectionList<ListCustomStats.Entry> {

        public ListCustomStats(Minecraft mc) {
            super(mc, ScreenStats.this.width, ScreenStats.this.height, 32, ScreenStats.this.height - 64, 12);
            OList<Stat<ResourceLocation>> list = new OArrayList<>();
            for (Stat<ResourceLocation> stat : Stats.CUSTOM) {
                if (stat.getValue().getNamespace().equals(Evolution.MODID)) {
                    if (!stat.getValue().getPath().startsWith("distance_") &&
                        !stat.getValue().getPath().startsWith("time_") &&
                        !stat.getValue().getPath().startsWith("death_") &&
                        !stat.getValue().getPath().startsWith("damage_")) {
                        list.add(stat);
                    }
                }
            }
            list.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(getFormattedName(a), getFormattedName(b)));
            for (int i = 0, l = list.size(); i < l; i++) {
                //noinspection ObjectAllocationInLoop
                this.addEntry(new Entry(list.get(i)));
            }
        }

        @Override
        protected int getScrollbarPosition() {
            return this.width - 6;
        }

        @Override
        protected void renderBackground(PoseStack matrices) {
            ScreenStats.this.renderBackground(matrices);
        }

        final class Entry extends ObjectSelectionList.Entry<Entry> {
            private final String title;
            private final String value;

            private Entry(Stat<ResourceLocation> stat) {
                this.title = getFormattedName(stat);
                this.value = ((IEvoStatFormatter) stat.formatter).format(ScreenStats.this.stats.getValue_(stat));
            }

            @Override
            public Component getNarration() {
                return EvolutionTexts.EMPTY;
            }

            @Override
            public void render(PoseStack matrices,
                               int index,
                               int y,
                               int x,
                               int width,
                               int height,
                               int mouseX,
                               int mouseY,
                               boolean hovered,
                               float partialTicks) {
                int color = index % 2 == 0 ? 0xff_ffff : 0x75_7575;
                drawString(matrices, ScreenStats.this.font, this.title, x + 2, y + 1, color);
                drawString(matrices, ScreenStats.this.font, this.value, x + 2 + 213 - ScreenStats.this.font.width(this.value), y + 1, color);
            }
        }
    }

    class ListDamageStats extends ObjectSelectionList<ScreenStats.ListDamageStats.Entry> {
        protected final Comparator<EvolutionDamage.Type> comparator = new ListComparator();
        protected int currentHeader = -1;
        protected final OList<EvolutionDamage.Type> damageList;
        protected final OList<Map<EvolutionDamage.Type, ResourceLocation>> damageStatList;
        protected int sortOrder;
        protected @Nullable Map<EvolutionDamage.Type, ResourceLocation> sorting;
        private final int[] headerTexture = {1, 2, 3};

        public ListDamageStats(Minecraft mc) {
            super(mc, ScreenStats.this.width, ScreenStats.this.height, 32, ScreenStats.this.height - 64, 20);
            this.damageStatList = new OArrayList<>();
            this.damageStatList.add(EvolutionStats.DAMAGE_DEALT_BY_TYPE);
            this.damageStatList.add(EvolutionStats.DAMAGE_RESISTED_BY_TYPE);
            this.damageStatList.add(EvolutionStats.DAMAGE_TAKEN_BY_TYPE);
            this.setRenderHeader(true, 20);
            this.damageList = new OArrayList<>(EvolutionDamage.ALL);
            for (int i = 0; i < this.damageList.size(); i++) {
                //noinspection ObjectAllocationInLoop
                this.addEntry(new ScreenStats.ListDamageStats.Entry());
            }
            this.damageList.sort(this.comparator);
        }

        @Override
        public int getRowWidth() {
            return super.getRowWidth() + 4 * 18;
        }

        @Override
        protected void clickedHeader(int mouseX, int mouseY) {
            this.currentHeader = -1;
            if (mouseY < this.headerHeight + this.y0 + 3) {
                for (int i = 0; i < this.headerTexture.length; i++) {
                    int j = mouseX - getCategoryOffset(i);
                    if (j >= -36 && j <= 0) {
                        this.currentHeader = i;
                        break;
                    }
                }
            }
            if (this.currentHeader >= 0) {
                this.sortBy(this.damageStatList.get(this.currentHeader));
                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }
        }

        @Override
        protected int getScrollbarPosition() {
            return this.width - 6;
        }

        @Override
        protected void renderBackground(PoseStack matrices) {
            ScreenStats.this.renderBackground(matrices);
        }

        @Override
        protected void renderDecorations(PoseStack matrices, int mouseX, int mouseY) {
            if (mouseY >= this.y0 && mouseY <= this.y1) {
                ScreenStats.ListDamageStats.Entry entryAtPos = this.getEntryAtPosition(mouseX, mouseY);
                int i = (this.width - this.getRowWidth()) / 2;
                if (entryAtPos != null) {
                    if (mouseX < i + 40 || mouseX > i + 40 + 20) {
                        return;
                    }
                    EvolutionDamage.Type type = this.damageList.get(this.children().indexOf(entryAtPos));
                    ScreenStats.this.renderTooltip(matrices, type.getTextComponent(), mouseX, mouseY);
                }
                else {
                    Component tooltip = null;
                    if (mouseY < this.headerHeight + this.y0 + 3) {
                        int j = mouseX - i;
                        for (int k = 0; k < this.headerTexture.length; ++k) {
                            int l = getCategoryOffset(k);
                            if (j >= l - 18 && j <= l) {
                                switch (k) {
                                    case 0 -> tooltip = ScreenStats.this.textDamageDealt;
                                    case 1 -> tooltip = ScreenStats.this.textDamageResisted;
                                    case 2 -> tooltip = ScreenStats.this.textDamageTaken;
                                }
                                break;
                            }
                        }
                    }
                    if (tooltip != null) {
                        ScreenStats.this.renderTooltip(matrices, tooltip, mouseX, mouseY);
                    }
                }
            }
        }

        @Override
        protected void renderHeader(PoseStack matrices, int mouseX, int mouseY, Tesselator tesselator) {
            if (!this.minecraft.mouseHandler.isLeftPressed()) {
                this.currentHeader = -1;
            }
            for (int i = 0; i < this.headerTexture.length; ++i) {
                ScreenStats.this.blitSlotIcon(matrices, mouseX + getCategoryOffset(i) - 18, mouseY + 1, 0, this.currentHeader == i ? 0 : 90);
            }
            if (this.sorting != null) {
                int k = getCategoryOffset(this.damageStatList.indexOf(this.sorting)) - 36;
                int j = this.sortOrder == 1 ? 2 : 1;
                ScreenStats.this.blitSlotIcon(matrices, mouseX + k, mouseY + 1, 18 * j, 0);
            }
            for (int l = 0; l < this.headerTexture.length; ++l) {
                int i1 = this.currentHeader == l ? 1 : 0;
                ScreenStats.this.blitSlotIcon(matrices, mouseX + getCategoryOffset(l) - 18 + i1, mouseY + 1 + i1, 18 * this.headerTexture[l], 90);
            }
        }

        protected void sortBy(Map<EvolutionDamage.Type, ResourceLocation> statType) {
            if (statType != this.sorting) {
                this.sorting = statType;
                this.sortOrder = -1;
            }
            else if (this.sortOrder == -1) {
                this.sortOrder = 1;
            }
            else {
                this.sorting = null;
                this.sortOrder = 0;
            }
            this.damageList.sort(this.comparator);
        }

        final class Entry extends ObjectSelectionList.Entry<ScreenStats.ListDamageStats.Entry> {

            private Entry() {
            }

            @Override
            public Component getNarration() {
                return EvolutionTexts.EMPTY;
            }

            @Override
            public void render(PoseStack matrices,
                               int index,
                               int y,
                               int x,
                               int width,
                               int height,
                               int mouseX,
                               int mouseY,
                               boolean hovered,
                               float partialTicks) {
                EvolutionDamage.Type type = ListDamageStats.this.damageList.get(index);
                ScreenStats.this.drawDamageSprite(matrices, x + 40, y, type);
                for (int j = 0; j < ListDamageStats.this.damageStatList.size(); j++) {
                    Stat<?> stat = null;
                    ResourceLocation resLoc = ListDamageStats.this.damageStatList.get(j).get(type);
                    if (resLoc != null) {
                        stat = Stats.CUSTOM.get(resLoc);
                    }
                    this.drawStatCount(matrices, stat, x + getCategoryOffset(j), y, index % 2 == 0);
                }
            }

            private void drawStatCount(PoseStack matrices, @Nullable Stat<?> stat, int x, int y, boolean highlight) {
                String s = stat == null ? "-" : EvolutionStats.METRIC.format(ScreenStats.this.stats.getValue_(stat));
                drawString(matrices, ScreenStats.this.font, s, x - ScreenStats.this.font.width(s), y + 5, highlight ? 0xff_ffff : 0x75_7575);
            }
        }

        final class ListComparator implements Comparator<EvolutionDamage.Type> {

            private ListComparator() {
            }

            @Override
            public int compare(EvolutionDamage.Type a, EvolutionDamage.Type b) {
                long i;
                long j;
                if (ScreenStats.ListDamageStats.this.sorting == null) {
                    i = 0;
                    j = 0;
                }
                else {
                    ResourceLocation aRes = ListDamageStats.this.sorting.get(a);
                    if (aRes != null) {
                        Stat<ResourceLocation> aStat = Stats.CUSTOM.get(aRes);
                        i = ScreenStats.this.stats.getValue_(aStat);
                    }
                    else {
                        i = -1;
                    }
                    ResourceLocation bRes = ListDamageStats.this.sorting.get(b);
                    if (bRes != null) {
                        Stat<ResourceLocation> bStat = Stats.CUSTOM.get(bRes);
                        j = ScreenStats.this.stats.getValue_(bStat);
                    }
                    else {
                        j = -1;
                    }
                }
                return i == j ?
                       String.CASE_INSENSITIVE_ORDER.compare(a.getTextComponent().getString(), b.getTextComponent().getString()) :
                       ListDamageStats.this.sortOrder * Long.compare(i, j);
            }
        }
    }

    class ListDeathStats extends ObjectSelectionList<ListDeathStats.Entry> {
        protected int currentHeader = -1;
        protected int sortOrder;
        private final Comparator<Stat<ResourceLocation>> comparator = new ListDeathStats.ListComparator();
        private final OList<Stat<ResourceLocation>> deathList;
        private final int[] headerTexture = {1};

        public ListDeathStats(Minecraft mc) {
            super(mc, ScreenStats.this.width, ScreenStats.this.height, 32, ScreenStats.this.height - 64, 12);
            this.setRenderHeader(true, 20);
            this.deathList = new OArrayList<>();
            for (Stat<ResourceLocation> stat : Stats.CUSTOM) {
                if (stat.getValue().getNamespace().equals(Evolution.MODID)) {
                    if (stat.getValue().getPath().startsWith("death_")) {
                        this.deathList.add(stat);
                        //noinspection ObjectAllocationInLoop
                        this.addEntry(new Entry());
                    }
                }
            }
            this.deathList.sort(this.comparator);
        }

        @Override
        public int getRowWidth() {
            return 260;
        }

        @Override
        protected void clickedHeader(int mouseX, int mouseY) {
            this.currentHeader = -1;
            if (mouseY < this.headerHeight + this.y0 + 3) {
                for (int i = 0; i < this.headerTexture.length; i++) {
                    int j = mouseX - getCategoryOffset(i) - 18;
                    if (j >= -36 && j <= 0) {
                        this.currentHeader = i;
                        break;
                    }
                }
            }
            if (this.currentHeader >= 0) {
                this.sort();
                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }
        }

        @Override
        protected int getScrollbarPosition() {
            return this.width - 6;
        }

        @Override
        protected void renderHeader(PoseStack matrices, int mouseX, int mouseY, Tesselator tesselator) {
            if (!this.minecraft.mouseHandler.isLeftPressed()) {
                this.currentHeader = -1;
            }
            for (int i = 0; i < this.headerTexture.length; ++i) {
                ScreenStats.this.blitSlotIcon(matrices, mouseX + getCategoryOffset(i), mouseY + 1, 0, this.currentHeader == i ? 0 : 6 * 18);
            }
            if (this.sortOrder != 0) {
                int k = getCategoryOffset(0) - 18;
                int j = this.sortOrder == 1 ? 2 : 1;
                ScreenStats.this.blitSlotIcon(matrices, mouseX + k, mouseY + 1, 18 * j, 0);
            }
            for (int l = 0; l < this.headerTexture.length; ++l) {
                int i1 = this.currentHeader == l ? 1 : 0;
                ScreenStats.this.blitSlotIcon(matrices, mouseX + getCategoryOffset(l) + i1, mouseY + 1 + i1, 18 * this.headerTexture[l], 6 * 18);
            }
        }

        protected void sort() {
            switch (this.sortOrder) {
                case -1 -> this.sortOrder = 1;
                case 0 -> this.sortOrder = -1;
                case 1 -> this.sortOrder = 0;
            }
            this.deathList.sort(this.comparator);
        }

        class Entry extends ObjectSelectionList.Entry<ListDeathStats.Entry> {

            @Override
            public Component getNarration() {
                return EvolutionTexts.EMPTY;
            }

            @Override
            public void render(PoseStack matrices,
                               int index,
                               int y,
                               int x,
                               int width,
                               int height,
                               int mouseX,
                               int mouseY,
                               boolean hovered,
                               float partialTicks) {
                String name = getFormattedName(ListDeathStats.this.deathList.get(index));
                int color = index % 2 == 0 ? 0xff_ffff : 0x75_7575;
                drawString(matrices, ScreenStats.this.font, name, x + 2, y + 1, color);
                String value = EvolutionStats.DEFAULT.format(ScreenStats.this.stats.getValue_(ListDeathStats.this.deathList.get(index)));
                drawString(matrices,
                           ScreenStats.this.font,
                           value,
                           x + 2 + ListDeathStats.this.getRowWidth() - 7 - ScreenStats.this.font.width(value),
                           y + 1,
                           color
                );
            }
        }

        final class ListComparator implements Comparator<Stat<ResourceLocation>> {

            @Override
            public int compare(Stat<ResourceLocation> a, Stat<ResourceLocation> b) {
                long i;
                long j;
                if (ListDeathStats.this.sortOrder == 0) {
                    i = 0;
                    j = 0;
                }
                else {
                    i = ScreenStats.this.stats.getValue_(a);
                    j = ScreenStats.this.stats.getValue_(b);
                }
                return i == j ? String.CASE_INSENSITIVE_ORDER.compare(getFormattedName(a), getFormattedName(b)) : ListDeathStats.this.sortOrder * Long.compare(i, j);
            }
        }
    }

    class ListDistanceStats extends ObjectSelectionList<ListDistanceStats.Entry> {

        protected int currentHeader = -1;
        protected int sortOrder;
        private final Comparator<Stat<ResourceLocation>> comparator = new ListComparator();
        private final OList<Stat<ResourceLocation>> distanceList;
        private final int[] headerTexture = {1};

        public ListDistanceStats(Minecraft mc) {
            super(mc, ScreenStats.this.width, ScreenStats.this.height, 32, ScreenStats.this.height - 64, 12);
            this.setRenderHeader(true, 20);
            this.distanceList = new OArrayList<>();
            for (Stat<ResourceLocation> stat : Stats.CUSTOM) {
                if (stat.getValue().getNamespace().equals(Evolution.MODID)) {
                    if (stat.getValue().getPath().startsWith("distance")) {
                        this.distanceList.add(stat);
                        //noinspection ObjectAllocationInLoop
                        this.addEntry(new Entry());
                    }
                }
            }
            this.distanceList.sort(this.comparator);
        }

        @Override
        protected void clickedHeader(int mouseX, int mouseY) {
            this.currentHeader = -1;
            if (mouseY < this.headerHeight + this.y0 + 3) {
                for (int i = 0; i < this.headerTexture.length; i++) {
                    int j = mouseX - getCategoryOffset(i);
                    if (j >= -36 && j <= 0) {
                        this.currentHeader = i;
                        break;
                    }
                }
            }
            if (this.currentHeader >= 0) {
                this.sort();
                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }
        }

        protected String getFormattedName(Stat<ResourceLocation> stat) {
            return I18n.get("stat." + stat.getValue().toString().replace(':', '.'));
        }

        @Override
        protected int getScrollbarPosition() {
            return this.width - 6;
        }

        @Override
        protected void renderHeader(PoseStack matrices, int mouseX, int mouseY, Tesselator tesselator) {
            if (!this.minecraft.mouseHandler.isLeftPressed()) {
                this.currentHeader = -1;
            }
            for (int i = 0; i < this.headerTexture.length; ++i) {
                ScreenStats.this.blitSlotIcon(matrices, mouseX + getCategoryOffset(i) - 18, mouseY + 1, 0, this.currentHeader == i ? 0 : 54);
            }
            if (this.sortOrder != 0) {
                int k = getCategoryOffset(0) - 18 * 2;
                int j = this.sortOrder == 1 ? 2 : 1;
                ScreenStats.this.blitSlotIcon(matrices, mouseX + k, mouseY + 1, 18 * j, 0);
            }
            for (int l = 0; l < this.headerTexture.length; ++l) {
                int i1 = this.currentHeader == l ? 1 : 0;
                ScreenStats.this.blitSlotIcon(matrices, mouseX + getCategoryOffset(l) - 18 + i1, mouseY + 1 + i1, 18 * this.headerTexture[l], 54);
            }
        }

        protected void sort() {
            switch (this.sortOrder) {
                case -1 -> this.sortOrder = 1;
                case 0 -> this.sortOrder = -1;
                case 1 -> this.sortOrder = 0;
            }
            this.distanceList.sort(this.comparator);
        }

        class Entry extends ObjectSelectionList.Entry<ScreenStats.ListDistanceStats.Entry> {

            @Override
            public Component getNarration() {
                return EvolutionTexts.EMPTY;
            }

            @Override
            public void render(PoseStack matrices, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTicks) {
                String name = ListDistanceStats.this.getFormattedName(ListDistanceStats.this.distanceList.get(index));
                int color = index % 2 == 0 ? 0xff_ffff : 0x75_7575;
                drawString(matrices, ScreenStats.this.font, name, x + 2, y + 1, color);
                String value = EvolutionStats.DISTANCE.format(ScreenStats.this.stats.getValue_(ListDistanceStats.this.distanceList.get(index)));
                drawString(matrices, ScreenStats.this.font, value, x + 2 + 213 - ScreenStats.this.font.width(value), y + 1, color);
            }
        }

        final class ListComparator implements Comparator<Stat<ResourceLocation>> {

            @Override
            public int compare(Stat<ResourceLocation> a, Stat<ResourceLocation> b) {
                long i;
                long j;
                if (ListDistanceStats.this.sortOrder == 0) {
                    i = 0;
                    j = 0;
                }
                else {
                    i = ScreenStats.this.stats.getValue_(a);
                    j = ScreenStats.this.stats.getValue_(b);
                }
                return i == j ? String.CASE_INSENSITIVE_ORDER.compare(ListDistanceStats.this.getFormattedName(a), ListDistanceStats.this.getFormattedName(b)) : ListDistanceStats.this.sortOrder * Long.compare(i, j);
            }
        }
    }

    class ListMobStats extends ObjectSelectionList<ScreenStats.ListMobStats.Entry> {

        protected int currentHeader = -1;
        protected final OList<StatType<EntityType<?>>> statTypes;
        private final Comparator<EntityType<?>> comparator = new ListMobStats.ListComparator();
        private final R2OMap<EntityType<?>, LivingEntity> entities = new R2OHashMap<>();
        private final OList<EntityType<?>> entityList;
        private final int[] headerTexture = {1, 2, 3, 4};
        private int sortOrder;
        private @Nullable StatType<?> sorting;

        public ListMobStats(Minecraft mc) {
            super(mc, ScreenStats.this.width, ScreenStats.this.height, 32, ScreenStats.this.height - 64, 9 * 6);
            this.statTypes = new OArrayList<>();
            this.statTypes.add(Stats.ENTITY_KILLED);
            this.statTypes.add(Stats.ENTITY_KILLED_BY);
            this.statTypes.add(EvolutionStats.DAMAGE_DEALT);
            this.statTypes.add(EvolutionStats.DAMAGE_TAKEN);
            this.setRenderHeader(true, 20);
            this.entityList = new OArrayList<>();
            for (EntityType<?> entityType : Registry.ENTITY_TYPE) {
                if (this.shouldAddEntry(entityType)) {
                    this.entityList.add(entityType);
                    assert ScreenStats.this.minecraft != null;
                    assert ScreenStats.this.minecraft.level != null;
                    this.entities.put(entityType, GUIUtils.getEntity(ScreenStats.this.minecraft.level, entityType));
                    //noinspection ObjectAllocationInLoop
                    this.addEntry(new ScreenStats.ListMobStats.Entry());
                }
            }
            this.entityList.sort(this.comparator);
        }

        @Override
        protected void clickedHeader(int mouseX, int mouseY) {
            this.currentHeader = -1;
            if (mouseY < this.headerHeight + this.y0 + 3) {
                for (int i = 0; i < this.headerTexture.length; i++) {
                    int j = mouseX - getCategoryOffset(i) + 18 * 3;
                    if (j >= -36 && j <= 0) {
                        this.currentHeader = i;
                        break;
                    }
                }
            }
            if (this.currentHeader >= 0) {
                this.sortBy(this.statTypes.get(this.currentHeader));
                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }
        }

        @Override
        protected int getScrollbarPosition() {
            return this.width - 6;
        }

        @Override
        protected void renderBackground(PoseStack matrices) {
            ScreenStats.this.renderBackground(matrices);
        }

        @Override
        protected void renderDecorations(PoseStack matrices, int mouseX, int mouseY) {
            if (mouseY >= this.y0 && mouseY <= this.y1) {
                ScreenStats.ListMobStats.Entry entryAtPos = this.getEntryAtPosition(mouseX, mouseY);
                if (entryAtPos == null) {
                    int i = (this.width - this.getRowWidth()) / 2;
                    Component text = null;
                    if (mouseY < this.headerHeight + this.y0 + 3) {
                        int j = mouseX - i;
                        for (int k = 0; k < this.headerTexture.length; ++k) {
                            int l = getCategoryOffset(k) - 18 * 3;
                            if (j >= l - 18 && j <= l) {
                                text = new TranslatableComponent(this.statTypes.get(k).getTranslationKey() + ".name");
                                break;
                            }
                        }
                    }
                    if (text != null) {
                        ScreenStats.this.renderTooltip(matrices, text, mouseX, mouseY);
                    }
                }
            }
        }

        @Override
        protected void renderHeader(PoseStack matrices, int mouseX, int mouseY, Tesselator tessellator) {
            if (!this.minecraft.mouseHandler.isLeftPressed()) {
                this.currentHeader = -1;
            }
            for (int i = 0; i < this.headerTexture.length; ++i) {
                ScreenStats.this.blitSlotIcon(matrices, mouseX + getCategoryOffset(i) - 18 * 4, mouseY + 1, 0, this.currentHeader == i ? 0 : 36);
            }
            if (this.sorting != null) {
                int k = getCategoryOffset(this.statTypes.indexOf(this.sorting)) - 18 * 5;
                int j = this.sortOrder == 1 ? 2 : 1;
                ScreenStats.this.blitSlotIcon(matrices, mouseX + k, mouseY + 1, 18 * j, 0);
            }
            for (int l = 0; l < this.headerTexture.length; ++l) {
                int i1 = this.currentHeader == l ? 1 : 0;
                ScreenStats.this.blitSlotIcon(matrices, mouseX + getCategoryOffset(l) - 18 * 4 + i1, mouseY + 1 + i1, 18 * this.headerTexture[l], 36);
            }
        }

        protected void sortBy(StatType<?> statType) {
            if (statType != this.sorting) {
                this.sorting = statType;
                this.sortOrder = -1;
            }
            else if (this.sortOrder == -1) {
                this.sortOrder = 1;
            }
            else {
                this.sorting = null;
                this.sortOrder = 0;
            }
            this.entityList.sort(this.comparator);
        }

        private boolean shouldAddEntry(EntityType<?> type) {
            if (ScreenStats.this.stats.getValue_(Stats.ENTITY_KILLED.get(type)) > 0) {
                return true;
            }
            if (ScreenStats.this.stats.getValue_(Stats.ENTITY_KILLED_BY.get(type)) > 0) {
                return true;
            }
            if (ScreenStats.this.stats.getValue_(EvolutionStats.DAMAGE_DEALT.get(type)) > 0) {
                return true;
            }
            return ScreenStats.this.stats.getValue_(EvolutionStats.DAMAGE_TAKEN.get(type)) > 0;
        }

        class Entry extends ObjectSelectionList.Entry<ScreenStats.ListMobStats.Entry> {

            public Entry() {
            }

            private static String getDmgDealtValue(String plural, long dmgDealt) {
                String s = EvolutionStats.DAMAGE_DEALT.getTranslationKey();
                return dmgDealt == 0 ? I18n.get(s + ".none", plural) : I18n.get(s, EvolutionStats.DAMAGE.format(dmgDealt), plural);
            }

            private static String getDmgTakenValue(String plural, long dmgTaken) {
                String s = EvolutionStats.DAMAGE_TAKEN.getTranslationKey();
                return dmgTaken == 0 ? I18n.get(s + ".none", plural) : I18n.get(s, EvolutionStats.DAMAGE.format(dmgTaken), plural);
            }

            private static String getKilledByValue(String plural, long killedBy) {
                String s = Stats.ENTITY_KILLED_BY.getTranslationKey();
                if (killedBy == 0) {
                    return I18n.get(s + ".none", plural);
                }
                return killedBy > 1 ? I18n.get(s, plural, killedBy) : I18n.get(s + ".once", plural);
            }

            private static String getKilledValue(String singular, String plural, long killed) {
                String s = Stats.ENTITY_KILLED.getTranslationKey();
                if (killed == 0) {
                    return I18n.get(s + ".none", plural);
                }
                return killed > 1 ? I18n.get(s, killed, plural) : I18n.get(s + ".once", singular);
            }

            @Override
            public Component getNarration() {
                return new TranslatableComponent("narrator.select");
            }

            @Override
            public void render(PoseStack matrices, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTicks) {
                EntityType<?> type = ListMobStats.this.entityList.get(index);
                LivingEntity entity = ListMobStats.this.entities.get(type);
                if (type == EntityType.PLAYER) {
                    entity = ListMobStats.this.minecraft.player;
                }
                if (entity != null) {
                    GUIUtils.drawEntityOnScreen(x - 30, y + 45, GUIUtils.getEntityScale(entity, 1.0f, 40, 35), mouseX, mouseY, entity);
                }
                long dmgDealt = ScreenStats.this.stats.getValue_(EvolutionStats.DAMAGE_DEALT, type);
                long dmgTaken = ScreenStats.this.stats.getValue_(EvolutionStats.DAMAGE_TAKEN, type);
                long killed = ScreenStats.this.stats.getValue_(Stats.ENTITY_KILLED, type);
                long killedBy = ScreenStats.this.stats.getValue_(Stats.ENTITY_KILLED_BY, type);
                String entityName = I18n.get(Util.makeDescriptionId("entity", EntityType.getKey(type)));
                String entityNamePlural = I18n.get(Util.makeDescriptionId("entity", EntityType.getKey(type)) + ".plural");
                drawString(matrices, ScreenStats.this.font, entityName, x + 2, y + 1, 0xff_ffff);
                int dmgColor = dmgDealt == dmgTaken ? dmgDealt == 0 ? 0x75_7575 : 0xc4_ad00 : dmgDealt > dmgTaken ? 0x33_b500 : 0xff_3030;
                drawString(matrices, ScreenStats.this.font, getDmgDealtValue(entityNamePlural, dmgDealt), x + 2 + 10, y + 1 + 9, dmgColor);
                drawString(matrices, ScreenStats.this.font, getDmgTakenValue(entityNamePlural, dmgTaken), x + 2 + 10, y + 1 + 9 * 2, dmgColor);
                int killColor = killed == killedBy ? killed == 0 ? 0x75_7575 : 0xc4_ad00 : killed > killedBy ? 0x33_b500 : 0xff_3030;
                drawString(matrices,
                           ScreenStats.this.font,
                           getKilledValue(entityName, entityNamePlural, killed),
                           x + 2 + 10,
                           y + 1 + 9 * 3,
                           killColor);
                drawString(matrices, ScreenStats.this.font, getKilledByValue(entityNamePlural, killedBy), x + 2 + 10, y + 1 + 9 * 4, killColor);
            }
        }

        final class ListComparator implements Comparator<EntityType<?>> {

            private ListComparator() {
            }

            @Override
            public int compare(EntityType<?> a, EntityType<?> b) {
                long i;
                long j;
                if (ScreenStats.ListMobStats.this.sorting == null) {
                    i = 0;
                    j = 0;
                }
                else {
                    StatType<EntityType<?>> entitySorting = (StatType<EntityType<?>>) ScreenStats.ListMobStats.this.sorting;
                    i = ScreenStats.this.stats.getValue_(entitySorting, a);
                    j = ScreenStats.this.stats.getValue_(entitySorting, b);
                }
                return i == j ? String.CASE_INSENSITIVE_ORDER.compare(a.getDescription().getString(), b.getDescription().getString()) : ListMobStats.this.sortOrder * Long.compare(i, j);
            }
        }
    }

    class ListStats extends ObjectSelectionList<ScreenStats.ListStats.Entry> {
        protected final OList<StatType<Block>> blockStatList;
        protected final Comparator<Item> comparator = new ListComparator();
        protected int currentHeader = -1;
        protected final OList<Item> itemList;
        protected final OList<StatType<Item>> itemStatList;
        protected int sortOrder;
        protected @Nullable StatType<?> sorting;
        private final int[] headerTexture = {3, 7, 4, 1, 2, 5, 6};

        public ListStats(Minecraft mc) {
            super(mc, ScreenStats.this.width, ScreenStats.this.height, 32, ScreenStats.this.height - 64, 20);
            this.blockStatList = new OArrayList<>();
            this.blockStatList.add(Stats.BLOCK_MINED);
            this.blockStatList.add(EvolutionStats.BLOCK_PLACED);
            this.itemStatList = new OArrayList<>();
            this.itemStatList.add(Stats.ITEM_BROKEN);
            this.itemStatList.add(Stats.ITEM_CRAFTED);
            this.itemStatList.add(Stats.ITEM_USED);
            this.itemStatList.add(Stats.ITEM_PICKED_UP);
            this.itemStatList.add(Stats.ITEM_DROPPED);
            this.setRenderHeader(true, 20);
            RSet<Item> set = new RHashSet<>();
            allItemInRegistry:
            for (Item item : Registry.ITEM) {
                for (int i = 0, l = this.itemStatList.size(); i < l; i++) {
                    StatType<Item> statType = this.itemStatList.get(i);
                    if (statType.contains(item) && ScreenStats.this.stats.getValue_(statType.get(item)) > 0) {
                        set.add(item);
                        continue allItemInRegistry;
                    }
                }
            }
            allBlockInRegistry:
            for (Block block : Registry.BLOCK) {
                for (int i = 0, l = this.blockStatList.size(); i < l; i++) {
                    StatType<Block> statType = this.blockStatList.get(i);
                    if (statType.contains(block) && ScreenStats.this.stats.getValue_(statType.get(block)) > 0) {
                        set.add(block.asItem());
                        continue allBlockInRegistry;
                    }
                }
            }
            set.remove(Items.AIR);
            this.itemList = new OArrayList<>(set);
            for (int i = 0; i < this.itemList.size(); i++) {
                //noinspection ObjectAllocationInLoop
                this.addEntry(new ScreenStats.ListStats.Entry());
            }
            this.itemList.sort(this.comparator);
        }

        @Override
        public int getRowWidth() {
            return 425;
        }

        @Override
        protected void clickedHeader(int mouseX, int mouseY) {
            this.currentHeader = -1;
            if (mouseY < this.headerHeight + this.y0 + 3) {
                for (int i = 0; i < this.headerTexture.length; i++) {
                    int j = mouseX - getCategoryOffset(i);
                    if (j >= -36 && j <= 0) {
                        this.currentHeader = i;
                        break;
                    }
                }
            }
            if (this.currentHeader >= 0) {
                this.sortBy(this.byIndex(this.currentHeader));
                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }
        }

        @Override
        protected int getScrollbarPosition() {
            return this.width - 6;
        }

        @Override
        protected void renderBackground(PoseStack matrices) {
            ScreenStats.this.renderBackground(matrices);
        }

        @Override
        protected void renderDecorations(PoseStack matrices, int mouseX, int mouseY) {
            if (mouseY >= this.y0 && mouseY <= this.y1) {
                ScreenStats.ListStats.Entry entryAtPos = this.getEntryAtPosition(mouseX, mouseY);
                int i = (this.width - this.getRowWidth()) / 2;
                if (entryAtPos != null) {
                    if (mouseX < i + 40 || mouseX > i + 40 + 20) {
                        return;
                    }
                    Item item = this.itemList.get(this.children().indexOf(entryAtPos));
                    ItemStack stack = ScreenStats.this.cachedModularItems.get(item);
                    String descId = stack == null ? item.getDescriptionId() : item.getDescriptionId(stack);
                    ScreenStats.this.renderTooltip(matrices, new TranslatableComponent(descId), mouseX, mouseY);
                }
                else {
                    Component name = null;
                    if (mouseY < this.headerHeight + this.y0 + 3) {
                        int j = mouseX - i;
                        for (int k = 0; k < this.headerTexture.length; ++k) {
                            int l = getCategoryOffset(k);
                            if (j >= l - 18 && j <= l) {
                                name = new TranslatableComponent(this.byIndex(k).getTranslationKey());
                                break;
                            }
                        }
                    }
                    if (name != null) {
                        ScreenStats.this.renderTooltip(matrices, name, mouseX, mouseY);
                    }
                }
            }
        }

        @Override
        protected void renderHeader(PoseStack matrices, int x, int y, Tesselator tesselator) {
            if (!this.minecraft.mouseHandler.isLeftPressed()) {
                this.currentHeader = -1;
            }
            for (int i = 0; i < this.headerTexture.length; ++i) {
                ScreenStats.this.blitSlotIcon(matrices, x + getCategoryOffset(i) - 18, y + 1, 0, this.currentHeader == i ? 0 : 18);
            }
            if (this.sorting != null) {
                int k = getCategoryOffset(this.indexOf(this.sorting)) - 36;
                int j = this.sortOrder == 1 ? 2 : 1;
                ScreenStats.this.blitSlotIcon(matrices, x + k, y + 1, 18 * j, 0);
            }
            for (int l = 0; l < this.headerTexture.length; ++l) {
                int i1 = this.currentHeader == l ? 1 : 0;
                ScreenStats.this.blitSlotIcon(matrices, x + getCategoryOffset(l) - 18 + i1, y + 1 + i1, 18 * this.headerTexture[l], 18);
            }
        }

        protected void sortBy(StatType<?> statType) {
            if (statType != this.sorting) {
                this.sorting = statType;
                this.sortOrder = -1;
            }
            else if (this.sortOrder == -1) {
                this.sortOrder = 1;
            }
            else {
                this.sorting = null;
                this.sortOrder = 0;
            }
            this.itemList.sort(this.comparator);
        }

        private StatType<?> byIndex(int index) {
            return index < this.blockStatList.size() ? this.blockStatList.get(index) : this.itemStatList.get(index - this.blockStatList.size());
        }

        private int indexOf(StatType<?> statType) {
            int blockIndex = this.blockStatList.indexOf(statType);
            if (blockIndex >= 0) {
                return blockIndex;
            }
            int itemIndex = this.itemStatList.indexOf(statType);
            return itemIndex >= 0 ? itemIndex + this.blockStatList.size() : -1;
        }

        final class Entry extends ObjectSelectionList.Entry<ScreenStats.ListStats.Entry> {

            private Entry() {
            }

            @Override
            public Component getNarration() {
                return EvolutionTexts.EMPTY;
            }

            @Override
            public void render(PoseStack matrices, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTicks) {
                Item item = ScreenStats.this.itemStats.itemList.get(index);
                ScreenStats.this.blitSlot(matrices, x + 40, y, item);
                for (int i = 0; i < ScreenStats.this.itemStats.blockStatList.size(); ++i) {
                    Stat<Block> stat;
                    if (item instanceof ItemBlock b) {
                        stat = ScreenStats.this.itemStats.blockStatList.get(i).get(b.getBlock());
                    }
                    else if (item instanceof BlockItem b) {
                        stat = ScreenStats.this.itemStats.blockStatList.get(i).get(b.getBlock());
                    }
                    else {
                        stat = null;
                    }
                    this.drawStatCount(matrices, stat, x + getCategoryOffset(i), y, index % 2 == 0);
                }
                for (int j = 0; j < ScreenStats.this.itemStats.itemStatList.size(); ++j) {
                    this.drawStatCount(matrices, ScreenStats.this.itemStats.itemStatList.get(j).get(item), x + getCategoryOffset(j + ScreenStats.this.itemStats.blockStatList.size()), y, index % 2 == 0);
                }
            }

            private void drawStatCount(PoseStack matrices, @Nullable Stat<?> stat, int x, int y, boolean highlight) {
                String s = stat == null ? "-" : EvolutionStats.METRIC.format(ScreenStats.this.stats.getValue_(stat));
                drawString(matrices, ScreenStats.this.font, s, x - ScreenStats.this.font.width(s), y + 5, highlight ? 0xff_ffff : 0x75_7575);
            }
        }

        final class ListComparator implements Comparator<Item> {

            private ListComparator() {
            }

            @Override
            public int compare(Item a, Item b) {
                long i;
                long j;
                if (ScreenStats.ListStats.this.sorting == null) {
                    i = 0;
                    j = 0;
                }
                else if (ScreenStats.ListStats.this.blockStatList.contains(ScreenStats.ListStats.this.sorting)) {
                    StatType<Block> blockSorting = (StatType<Block>) ScreenStats.ListStats.this.sorting;
                    i = this.getBlockStats(blockSorting, a);
                    j = this.getBlockStats(blockSorting, b);
                }
                else {
                    StatType<Item> itemSorting = (StatType<Item>) ScreenStats.ListStats.this.sorting;
                    i = ScreenStats.this.stats.getValue_(itemSorting, a);
                    j = ScreenStats.this.stats.getValue_(itemSorting, b);
                }
                return i == j ? String.CASE_INSENSITIVE_ORDER.compare(a.getDescription().getString(), b.getDescription().getString()) : ListStats.this.sortOrder * Long.compare(i, j);
            }

            private long getBlockStats(StatType<Block> blockSorting, Item item) {
                if (item instanceof ItemBlock b) {
                    return ScreenStats.this.stats.getValue_(blockSorting, b.getBlock());
                }
                if (item instanceof BlockItem b) {
                    return ScreenStats.this.stats.getValue_(blockSorting, b.getBlock());
                }
                return -1;
            }
        }
    }

    class ListTimeStats extends ObjectSelectionList<ListTimeStats.Entry> {
        protected int currentHeader = -1;
        protected int sortOrder;
        private final Comparator<Stat<ResourceLocation>> comparator = new ListTimeStats.ListComparator();
        private final int[] headerTexture = {1};
        private final OList<Stat<ResourceLocation>> timeList;

        public ListTimeStats(Minecraft mc) {
            super(mc, ScreenStats.this.width, ScreenStats.this.height, 32, ScreenStats.this.height - 64, 12);
            this.setRenderHeader(true, 20);
            this.timeList = new OArrayList<>();
            for (Stat<ResourceLocation> stat : Stats.CUSTOM) {
                if (stat.getValue().getNamespace().equals(Evolution.MODID)) {
                    if (stat.getValue().getPath().startsWith("time_")) {
                        this.timeList.add(stat);
                        //noinspection ObjectAllocationInLoop
                        this.addEntry(new Entry());
                    }
                }
            }
            this.timeList.sort(this.comparator);
        }

        @Override
        protected void clickedHeader(int mouseX, int mouseY) {
            this.currentHeader = -1;
            if (mouseY < this.headerHeight + this.y0 + 3) {
                for (int i = 0; i < this.headerTexture.length; i++) {
                    int j = mouseX - getCategoryOffset(i);
                    if (j >= -36 && j <= 0) {
                        this.currentHeader = i;
                        break;
                    }
                }
            }
            if (this.currentHeader >= 0) {
                this.sort();
                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }
        }

        protected String getFormattedName(Stat<ResourceLocation> stat) {
            return I18n.get("stat." + stat.getValue().toString().replace(':', '.'));
        }

        @Override
        protected int getScrollbarPosition() {
            return this.width - 6;
        }

        @Override
        protected void renderHeader(PoseStack matrices, int x, int y, Tesselator tesselator) {
            if (!this.minecraft.mouseHandler.isLeftPressed()) {
                this.currentHeader = -1;
            }
            for (int i = 0; i < this.headerTexture.length; ++i) {
                ScreenStats.this.blitSlotIcon(matrices, x + getCategoryOffset(i) - 18, y + 1, 0, this.currentHeader == i ? 0 : 72);
            }
            if (this.sortOrder != 0) {
                int k = getCategoryOffset(0) - 18 * 2;
                int j = this.sortOrder == 1 ? 2 : 1;
                ScreenStats.this.blitSlotIcon(matrices, x + k, y + 1, 18 * j, 0);
            }
            for (int l = 0; l < this.headerTexture.length; ++l) {
                int i1 = this.currentHeader == l ? 1 : 0;
                ScreenStats.this.blitSlotIcon(matrices, x + getCategoryOffset(l) - 18 + i1, y + 1 + i1, 18 * this.headerTexture[l], 72);
            }
        }

        protected void sort() {
            switch (this.sortOrder) {
                case -1 -> this.sortOrder = 1;
                case 0 -> this.sortOrder = -1;
                case 1 -> this.sortOrder = 0;
            }
            this.timeList.sort(this.comparator);
        }

        class Entry extends ObjectSelectionList.Entry<ListTimeStats.Entry> {

            @Override
            public Component getNarration() {
                return EvolutionTexts.EMPTY;
            }

            @Override
            public void render(PoseStack matrices, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTicks) {
                String name = ListTimeStats.this.getFormattedName(ListTimeStats.this.timeList.get(index));
                int color = index % 2 == 0 ? 0xff_ffff : 0x75_7575;
                drawString(matrices, ScreenStats.this.font, name, x + 2, y + 1, color);
                String value = EvolutionStats.TIME.format(ScreenStats.this.stats.getValue_(ListTimeStats.this.timeList.get(index)));
                drawString(matrices, ScreenStats.this.font, value, x + 2 + 213 - ScreenStats.this.font.width(value), y + 1, color);
            }
        }

        final class ListComparator implements Comparator<Stat<ResourceLocation>> {

            @Override
            public int compare(Stat<ResourceLocation> a, Stat<ResourceLocation> b) {
                long i;
                long j;
                if (ListTimeStats.this.sortOrder == 0) {
                    i = 0;
                    j = 0;
                }
                else {
                    i = ScreenStats.this.stats.getValue_(a);
                    j = ScreenStats.this.stats.getValue_(b);
                }
                return i == j ? String.CASE_INSENSITIVE_ORDER.compare(ListTimeStats.this.getFormattedName(a), ListTimeStats.this.getFormattedName(b)) : ListTimeStats.this.sortOrder * Long.compare(i, j);
            }
        }
    }
}
