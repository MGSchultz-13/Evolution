package tgw.evolution.events;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.block.Blocks;
import net.minecraft.client.GameSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.FirstPersonRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.settings.AttackIndicatorStatus;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.UseAction;
import net.minecraft.potion.EffectUtils;
import net.minecraft.potion.Effects;
import net.minecraft.util.Hand;
import net.minecraft.util.MovementInput;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.*;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.client.ForgeIngameGui;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import tgw.evolution.ClientProxy;
import tgw.evolution.Evolution;
import tgw.evolution.blocks.BlockKnapping;
import tgw.evolution.blocks.BlockMolding;
import tgw.evolution.blocks.tileentities.TEKnapping;
import tgw.evolution.blocks.tileentities.TEMolding;
import tgw.evolution.client.renderer.ambient.LightTextureEv;
import tgw.evolution.client.renderer.ambient.SkyRenderer;
import tgw.evolution.entities.EvolutionAttributes;
import tgw.evolution.init.EvolutionEffects;
import tgw.evolution.init.EvolutionNetwork;
import tgw.evolution.items.IOffhandAttackable;
import tgw.evolution.items.ITwoHanded;
import tgw.evolution.network.PacketCSChangeBlock;
import tgw.evolution.network.PacketCSOpenExtendedInventory;
import tgw.evolution.network.PacketCSPlayerAttack;
import tgw.evolution.network.PacketCSSetProne;
import tgw.evolution.potion.EffectDizziness;
import tgw.evolution.util.EvolutionStyles;
import tgw.evolution.util.MathHelper;
import tgw.evolution.util.PlayerHelper;
import tgw.evolution.world.dimension.DimensionOverworld;

import java.util.UUID;

public class ClientEvents {

    private static final ResourceLocation ICONS = Evolution.location("textures/gui/icons.png");
    private static final String TWO_HANDED = "evolution.actionbar.two_handed";
    private static final ITextComponent COMPONENT_TWO_HANDED = new TranslationTextComponent(TWO_HANDED).setStyle(EvolutionStyles.WHITE);
    private static final BlockPos.MutableBlockPos AUX_POS = new BlockPos.MutableBlockPos();
    private final Minecraft mc;
    private GameRenderer oldGameRenderer;
    private boolean inverted;
    private boolean skyRendererBinded;
    private SkyRenderer skyRenderer;
    //Jump variables
    private boolean jump;
    private boolean isJumpPressed;
    //Prone variables
    private boolean proneToggle;
    private boolean previousPressed;
    //Offhand variables
    private Entity rightPointedEntity;
    private ItemStack offhandStack = ItemStack.EMPTY;
    private int rightTimeSinceLastHit;
    private float rightSwingProgress;
    private float rightPrevSwingProgress;
    private int rightSwingProgressInt;
    private boolean rightIsSwingInProgress;
    private float rightEquipProgress;
    private float rightPrevEquipProgress;
    //Mainhand variables
    private Entity leftPointedEntity;
    private ItemStack mainhandStack = ItemStack.EMPTY;
    private int leftTimeSinceLastHit;
    private float leftSwingProgress;
    private float leftPrevSwingProgress;
    private int leftSwingProgressInt;
    private boolean leftIsSwingInProgress;
    private float leftEquipProgress;
    private float leftPrevEquipProgress;
    private boolean requiresReequiping;

    public ClientEvents(Minecraft mc) {
        this.mc = mc;
    }

    @SubscribeEvent
    public void onFogRender(EntityViewRenderEvent.FogDensity event) {
        //Render Blindness fog
        if (this.mc.player != null && this.mc.player.isPotionActive(Effects.BLINDNESS)) {
            float f1 = 5.0F;
            int duration = this.mc.player.getActivePotionEffect(Effects.BLINDNESS).getDuration();
            int amplifier = this.mc.player.getActivePotionEffect(Effects.BLINDNESS).getAmplifier() + 1;
            if (duration < 20) {
                f1 = 5.0F + (this.mc.gameSettings.renderDistanceChunks * 16 - 5.0F) * (1.0F - duration / 20.0F);
            }
            GlStateManager.fogMode(GlStateManager.FogMode.LINEAR);
            float multiplier = 0.25F / amplifier;
            GlStateManager.fogStart(f1 * multiplier);
            GlStateManager.fogEnd(f1 * multiplier * 4.0F);
            if (GL.getCapabilities().GL_NV_fog_distance) {
                GL11.glFogi(34138, 34139);
            }
            event.setDensity(2.0F);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onRenderOutlines(DrawBlockHighlightEvent event) {
        if (event.getTarget().getType() == RayTraceResult.Type.BLOCK) {
            BlockPos hitPos = ((BlockRayTraceResult) event.getTarget()).getPos();
            if (!this.mc.world.getWorldBorder().contains(hitPos)) {
                return;
            }
            if (this.mc.world.getBlockState(hitPos).getBlock() instanceof BlockKnapping) {
                TEKnapping tile = (TEKnapping) this.mc.world.getTileEntity(hitPos);
                this.renderOutlines(tile.type.getShape(), event.getInfo(), hitPos);
                return;
            }
            if (this.mc.world.getBlockState(hitPos).getBlock() instanceof BlockMolding) {
                TEMolding tile = (TEMolding) this.mc.world.getTileEntity(hitPos);
                this.renderOutlines(tile.molding.getShape(), event.getInfo(), hitPos);
            }
        }
    }

    private void renderOutlines(VoxelShape shape, ActiveRenderInfo info, BlockPos pos) {
        GlStateManager.enableBlend();
        GlStateManager.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                                         GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                                         GlStateManager.SourceFactor.ONE,
                                         GlStateManager.DestFactor.ZERO);
        GlStateManager.lineWidth(Math.max(2.5F, this.mc.mainWindow.getFramebufferWidth() / 1920.0F * 2.5F));
        GlStateManager.disableTexture();
        GlStateManager.depthMask(false);
        GlStateManager.matrixMode(5889);
        GlStateManager.pushMatrix();
        GlStateManager.scalef(1.0F, 1.0F, 0.999F);
        double projX = info.getProjectedView().x;
        double projY = info.getProjectedView().y;
        double projZ = info.getProjectedView().z;
        WorldRenderer.drawShape(shape, pos.getX() - projX, pos.getY() - projY, pos.getZ() - projZ, 1.0F, 1.0F, 0.0F, 1.0F);
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(5888);
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture();
        GlStateManager.disableBlend();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        //Turn auto-jump off
        this.mc.gameSettings.autoJump = false;
        if (this.mc.player == null) {
            this.skyRendererBinded = false;
            this.skyRenderer = null;
            return;
        }
        //Bind Sky Renderer
        if (!this.skyRendererBinded) {
            if (this.mc.world.dimension.getType() == DimensionType.OVERWORLD) {
                this.skyRenderer = new SkyRenderer(this.mc.worldRenderer);
                this.mc.world.dimension.setSkyRenderer(this.skyRenderer);
                this.skyRendererBinded = true;
            }
        }
        //Jump calculation
        if (this.jump) {
            PlayerEntity player = this.mc.player;
            Vec3d motion = player.getMotion();
            if (motion.y > 0 && !player.isOnLadder() && !player.abilities.isFlying) {
                player.setMotion(motion.x, motion.y - this.getJumpSlowDown(), motion.z);
            }
            else {
                this.jump = false;
                if (!player.isOnLadder() && !player.abilities.isFlying) {
                    player.setMotion(motion.x, motion.y - 0.055, motion.z);
                }
            }
        }
        //Runs at the start of each tick
        if (event.phase == TickEvent.Phase.START) {
            //RayTrace entities
            if (!this.mc.isGamePaused()) {
                if (this.mc.world.dimension instanceof DimensionOverworld) {
                    this.mc.world.dimension.tick();
                }
                EntityRayTraceResult
                        leftRayTrace =
                        MathHelper.rayTraceEntityFromEyes(this.mc.player, 1f, this.mc.player.getAttribute(PlayerEntity.REACH_DISTANCE).getValue());
                this.leftPointedEntity = leftRayTrace == null ? null : leftRayTrace.getEntity();
                if (this.mc.player.getHeldItemOffhand().getItem() instanceof IOffhandAttackable) {
                    EntityRayTraceResult
                            rightRayTrace =
                            MathHelper.rayTraceEntityFromEyes(this.mc.player,
                                                              1f,
                                                              ((IOffhandAttackable) this.mc.player.getHeldItemOffhand().getItem()).getReach() +
                                                              PlayerHelper.REACH_DISTANCE);
                    this.rightPointedEntity = rightRayTrace == null ? null : rightRayTrace.getEntity();
                }
                else {
                    this.rightPointedEntity = null;
                }
            }
            GameRenderer gameRenderer = this.mc.gameRenderer;
            if (gameRenderer != this.oldGameRenderer) {
                this.oldGameRenderer = gameRenderer;
                ObfuscationReflectionHelper.setPrivateValue(GameRenderer.class,
                                                            this.oldGameRenderer,
                                                            new LightTextureEv(this.oldGameRenderer),
                                                            "field_78513_d");
            }
            //Handle two-handed items
            if (this.mc.player.getHeldItemMainhand().getItem() instanceof ITwoHanded && !this.mc.player.getHeldItemOffhand().isEmpty()) {
                this.leftTimeSinceLastHit = 0;
                this.requiresReequiping = true;
                ObfuscationReflectionHelper.setPrivateValue(Minecraft.class, this.mc, Integer.MAX_VALUE, "field_71429_W");
                this.mc.player.sendStatusMessage(COMPONENT_TWO_HANDED, true);
            }
            //Prevents the player from attacking if on cooldown
            if (this.getLeftCooledAttackStrength(0) != 1 &&
                this.mc.objectMouseOver != null &&
                this.mc.objectMouseOver.getType() != RayTraceResult.Type.BLOCK) {
                ObfuscationReflectionHelper.setPrivateValue(Minecraft.class, this.mc, Integer.MAX_VALUE, "field_71429_W");
            }
            //Handle water physics
            if (this.mc.player.isInWater()) {
                int levelAtPos = this.mc.world.getFluidState(AUX_POS.setPos(this.mc.player)).getLevel();
                Vec3d motion = this.mc.player.getMotion();
                if (!this.mc.player.isSwimming()) {
                    this.mc.player.setMotion(motion.x * (1 + (8 - levelAtPos) * 0.01), motion.y, motion.z * (1 + (8 - levelAtPos) * 0.01));
                }
                //Handle water emerge
                Vec3d motionForEmerge = this.mc.player.getMotion();
                if (this.mc.player.collidedHorizontally && this.mc.player.isOffsetPositionInLiquid(motion.x, motion.y + 0.64, motion.z)) {
                    double waterEmergeMotion = this.getWaterEmergeMotion(levelAtPos);
                    this.mc.player.setMotion(motionForEmerge.x, waterEmergeMotion, motionForEmerge.z);
                }
            }
            //Handle Disoriented Effect
            if (this.mc.player.isPotionActive(EvolutionEffects.DISORIENTED.get())) {
                if (!this.inverted) {
                    this.inverted = true;
                    swapControls(this.mc);
                }
            }
            else {
                if (this.inverted) {
                    this.inverted = false;
                    swapControls(this.mc);
                }
            }
            //Handle Dizziness Effect
            if (!this.mc.player.isPotionActive(EvolutionEffects.DIZZINESS.get())) {
                EffectDizziness.lastMotion = Vec3d.ZERO;
                EffectDizziness.tick = 0;
            }
        }
        //Runs at the end of each tick
        else if (event.phase == TickEvent.Phase.END) {
            //Proning
            boolean pressed = ClientProxy.TOGGLE_PRONE.isKeyDown();
            if (pressed && !this.previousPressed) {
                this.proneToggle = !this.proneToggle;
            }
            this.previousPressed = pressed;
            this.updateClientProneState(this.mc.player);
            //Handle swing
            if (!this.mc.isGamePaused()) {
                if (this.mc.playerController.getIsHittingBlock()) {
                    this.requiresReequiping = false;
                }
                this.rightPrevSwingProgress = this.rightSwingProgress;
                this.rightPrevEquipProgress = this.rightEquipProgress;
                this.leftPrevSwingProgress = this.leftSwingProgress;
                this.leftPrevEquipProgress = this.leftEquipProgress;
                this.updateArmSwingProgress();
                if (this.mc.player.isRowingBoat()) {
                    this.leftEquipProgress = MathHelper.clamp(this.leftEquipProgress - 0.4F, 0.0F, 1.0F);
                    this.rightEquipProgress = MathHelper.clamp(this.rightEquipProgress - 0.4F, 0.0F, 1.0F);
                }
                else {
                    float cooledAttackStrength = this.getRightCooledAttackStrength(this.mc.player.getHeldItemOffhand().getItem(), 1);
                    this.rightEquipProgress +=
                            MathHelper.clamp(cooledAttackStrength * cooledAttackStrength * cooledAttackStrength - this.rightEquipProgress,
                                             -0.4F,
                                             0.4F);
                    cooledAttackStrength = this.getLeftCooledAttackStrength(1);
                    this.leftEquipProgress +=
                            MathHelper.clamp(cooledAttackStrength * cooledAttackStrength * cooledAttackStrength - this.leftEquipProgress,
                                             -0.4F,
                                             0.4F);
                }
                ItemStack stackOffhand = this.mc.player.getHeldItemOffhand();
                if (MathHelper.areItemStacksSufficientlyEqual(stackOffhand, this.offhandStack)) {
                    this.rightTimeSinceLastHit++;
                }
                else {
                    this.rightTimeSinceLastHit = 0;
                    this.offhandStack = stackOffhand;
                }
                ItemStack stackMainhand = this.mc.player.getHeldItemMainhand();
                if (MathHelper.areItemStacksSufficientlyEqual(stackMainhand, this.mainhandStack)) {
                    this.leftTimeSinceLastHit++;
                }
                else {
                    this.leftTimeSinceLastHit = 0;
                    this.mainhandStack = stackMainhand;
                }
            }
        }
    }

//    @SubscribeEvent
//    public void onFogColor(EntityViewRenderEvent.FogColors event) {
//        if (this.skyRenderer == null) {
//            return;
//        }
//        Vec3f colors = EarthHelper.getFogColor(this.skyRenderer.sunElevation);
//        event.setRed(colors.x);
//        event.setGreen(colors.y);
//        event.setBlue(colors.z);
//    }

    public double getJumpSlowDown() {
        IAttributeInstance mass = this.mc.player.getAttribute(EvolutionAttributes.MASS);
        int baseMass = (int) mass.getBaseValue();
        int totalMass = (int) mass.getValue();
        int equipMass = totalMass - baseMass;
        return 0.03 + equipMass * 0.0002;
    }

    private float getLeftCooledAttackStrength(float adjustTicks) {
        return MathHelper.clamp(((float) this.leftTimeSinceLastHit + adjustTicks) / this.mc.player.getCooldownPeriod(), 0.0F, 1.0F);
    }

    private float getWaterEmergeMotion(int levelAtPos) {
        levelAtPos = MathHelper.clamp(levelAtPos, 0, 8);
        switch (levelAtPos) {
            case 0:
                return 0;
            case 1:
                return 0.3f;
            case 2:
                return this.mc.player.getSubmergedHeight() > 0.2 ? 0.28f : 0.18f;
            case 3:
            case 4:
                return 0.15f;
            case 5:
                return 0.135f;
            case 6:
                return 0.12f;
            case 7:
                return 0.1f;
            case 8:
                return 0.08f;
        }
        throw new IllegalStateException("Invalid level " + levelAtPos);
    }

    private static void swapControls(Minecraft mc) {
        swapKeybinds(mc.gameSettings.keyBindJump, mc.gameSettings.keyBindSneak);
        swapKeybinds(mc.gameSettings.keyBindForward, mc.gameSettings.keyBindBack);
        swapKeybinds(mc.gameSettings.keyBindLeft, mc.gameSettings.keyBindRight);
        mc.gameSettings.saveOptions();
        mc.gameSettings.loadOptions();
    }

    private void updateClientProneState(PlayerEntity player) {
        if (player != null) {
            UUID uuid = player.getUniqueID();
            boolean shouldBeProne = ClientProxy.TOGGLE_PRONE.isKeyDown() != this.proneToggle;
            shouldBeProne =
                    shouldBeProne && !player.isInWater() && !player.isInLava() && (!player.isOnLadder() || !this.isJumpPressed && player.onGround);
            shouldBeProne = shouldBeProne && (!player.isOnLadder() || !this.isJumpPressed && player.onGround);
            BlockPos pos = player.getPosition().up(2);
            shouldBeProne =
                    shouldBeProne ||
                    this.proneToggle && player.isOnLadder() && !player.world.getBlockState(pos).getCollisionShape(player.world, pos, null).isEmpty();
            if (shouldBeProne != Evolution.PRONED_PLAYERS.getOrDefault(uuid, false)) {
                EvolutionNetwork.INSTANCE.sendToServer(new PacketCSSetProne(shouldBeProne));
            }
            Evolution.PRONED_PLAYERS.put(uuid, shouldBeProne);
        }
    }

    private void updateArmSwingProgress() {
        int i = this.getArmSwingAnimationEnd();
        if (this.rightIsSwingInProgress) {
            ++this.rightSwingProgressInt;
            if (this.rightSwingProgressInt >= i) {
                this.rightSwingProgressInt = 0;
                this.rightIsSwingInProgress = false;
            }
        }
        else {
            this.rightSwingProgressInt = 0;
        }
        this.rightSwingProgress = (float) this.rightSwingProgressInt / (float) i;
        if (this.leftIsSwingInProgress) {
            ++this.leftSwingProgressInt;
            if (this.leftSwingProgressInt >= i) {
                this.leftSwingProgressInt = 0;
                this.leftIsSwingInProgress = false;
            }
        }
        else {
            this.leftSwingProgressInt = 0;
        }
        this.leftSwingProgress = (float) this.leftSwingProgressInt / (float) i;
    }

    private float getRightCooledAttackStrength(Item item, float adjustTicks) {
        if (!(item instanceof IOffhandAttackable)) {
            return 0;
        }
        return MathHelper.clamp(((float) this.rightTimeSinceLastHit + adjustTicks) / getRightCooldownPeriod((IOffhandAttackable) item), 0.0F, 1.0F);
    }

    private static void swapKeybinds(KeyBinding a, KeyBinding b) {
        InputMappings.Input temp = a.getKey();
        a.bind(b.getKey());
        b.bind(temp);
    }

    private int getArmSwingAnimationEnd() {
        if (EffectUtils.hasMiningSpeedup(this.mc.player)) {
            return 6 - (1 + EffectUtils.getMiningSpeedup(this.mc.player));
        }
        return this.mc.player.isPotionActive(Effects.MINING_FATIGUE) ?
               6 + (1 + this.mc.player.getActivePotionEffect(Effects.MINING_FATIGUE).getAmplifier()) * 2 :
               6;
    }

    private static float getRightCooldownPeriod(IOffhandAttackable item) {
        double attackSpeed = item.getAttackSpeed() + PlayerHelper.ATTACK_SPEED;
        return (float) (1 / attackSpeed * 20);
    }

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Pre event) {
        if (this.mc.player.getRidingEntity() != null) {
            ForgeIngameGui.renderFood = true;
        }
        if (event.getType() == RenderGameOverlayEvent.ElementType.CROSSHAIRS) {
            event.setCanceled(true);
            this.renderAttackIndicator();
        }
    }

    private void renderAttackIndicator() {
        GameSettings gamesettings = this.mc.gameSettings;
        boolean offhandValid = this.mc.player.getHeldItemOffhand().getItem() instanceof IOffhandAttackable;
        this.mc.getTextureManager().bindTexture(ICONS);
        int scaledWidth = this.mc.mainWindow.getScaledWidth();
        int scaledHeight = this.mc.mainWindow.getScaledHeight();
        if (gamesettings.thirdPersonView == 0) {
            if (this.mc.playerController.getCurrentGameType() != GameType.SPECTATOR || this.rayTraceMouse(this.mc.objectMouseOver)) {
                if (gamesettings.showDebugInfo && !gamesettings.hideGUI && !this.mc.player.hasReducedDebug() && !gamesettings.reducedDebugInfo) {
                    GlStateManager.pushMatrix();
                    int blitOffset = 0;
                    GlStateManager.translatef((float) (scaledWidth / 2), (float) (scaledHeight / 2), (float) blitOffset);
                    ActiveRenderInfo activerenderinfo = this.mc.gameRenderer.getActiveRenderInfo();
                    GlStateManager.rotatef(activerenderinfo.getPitch(), -1.0F, 0.0F, 0.0F);
                    GlStateManager.rotatef(activerenderinfo.getYaw(), 0.0F, 1.0F, 0.0F);
                    GlStateManager.scalef(-1.0F, -1.0F, -1.0F);
                    GLX.renderCrosshair(10);
                    GlStateManager.popMatrix();
                }
                else {
                    GlStateManager.enableBlend();
                    GlStateManager.enableAlphaTest();
                    GlStateManager.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                                                     GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                                                     GlStateManager.SourceFactor.ONE,
                                                     GlStateManager.DestFactor.ZERO);
                    GlStateManager.blendFuncSeparate(GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR,
                                                     GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR,
                                                     GlStateManager.SourceFactor.ONE,
                                                     GlStateManager.DestFactor.ZERO);
                    this.blit((scaledWidth - 15) / 2, (scaledHeight - 15) / 2, 0, 0, 15, 15);
                    if (this.mc.gameSettings.attackIndicator == AttackIndicatorStatus.CROSSHAIR) {
                        float leftCooledAttackStrength = this.getLeftCooledAttackStrength(0);
                        boolean shouldShowLeftAttackIndicator = false;
                        if (this.leftPointedEntity instanceof LivingEntity && leftCooledAttackStrength >= 1) {
                            shouldShowLeftAttackIndicator = this.mc.player.getCooldownPeriod() > 5;
                            shouldShowLeftAttackIndicator &= this.leftPointedEntity.isAlive();
                        }
                        int x = scaledWidth / 2 - 8;
                        x = offhandValid ? x + 10 : x;
                        int y = scaledHeight / 2 - 7 + 16;
                        if (shouldShowLeftAttackIndicator) {
                            this.blit(x, y, 68, 94, 16, 16);
                        }
                        else if (leftCooledAttackStrength < 1.0F) {
                            int l = (int) (leftCooledAttackStrength * 17.0F);
                            this.blit(x, y, 36, 94, 16, 4);
                            this.blit(x, y, 52, 94, l, 4);
                        }
                        if (offhandValid) {
                            boolean shouldShowRightAttackIndicator = false;
                            float rightCooledAttackStrength = this.getRightCooledAttackStrength(this.mc.player.getHeldItemOffhand().getItem(), 0);
                            if (this.rightPointedEntity instanceof LivingEntity && rightCooledAttackStrength >= 1) {
                                shouldShowRightAttackIndicator = this.rightPointedEntity.isAlive();
                            }
                            x -= 20;
                            if (shouldShowRightAttackIndicator) {
                                this.blit(x, y, 68, 110, 16, 16);
                            }
                            else if (rightCooledAttackStrength < 1.0F) {
                                int l = (int) (rightCooledAttackStrength * 17.0F);
                                this.blit(x, y, 36, 110, 16, 4);
                                this.blit(x, y, 52, 110, l, 4);
                            }
                        }
                        GlStateManager.disableAlphaTest();
                    }
                }
            }
        }
    }

    private boolean rayTraceMouse(RayTraceResult rayTraceResult) {
        if (rayTraceResult == null) {
            return false;
        }
        if (rayTraceResult.getType() == RayTraceResult.Type.ENTITY) {
            return ((EntityRayTraceResult) rayTraceResult).getEntity() instanceof INamedContainerProvider;
        }
        if (rayTraceResult.getType() == RayTraceResult.Type.BLOCK) {
            BlockPos blockpos = ((BlockRayTraceResult) rayTraceResult).getPos();
            World world = this.mc.world;
            return world.getBlockState(blockpos).getContainer(world, blockpos) != null;
        }
        return false;
    }

    private void blit(int x, int y, int textureX, int textureY, int sizeX, int sizeY) {
        AbstractGui.blit(x, y, 20, (float) textureX, (float) textureY, sizeX, sizeY, 256, 256);
    }

    public void swingArm(Hand hand) {
        ItemStack stack = this.mc.player.getHeldItem(hand);
        if (!stack.isEmpty() && stack.onEntitySwing(this.mc.player)) {
            return;
        }
        if (hand == Hand.OFF_HAND) {
            if (!this.rightIsSwingInProgress || this.rightSwingProgressInt >= this.getArmSwingAnimationEnd() / 2 || this.rightSwingProgressInt < 0) {
                this.rightSwingProgressInt = -1;
                this.rightIsSwingInProgress = true;
            }
        }
        else {
            if (!this.leftIsSwingInProgress || this.leftSwingProgressInt >= this.getArmSwingAnimationEnd() / 2 || this.leftSwingProgressInt < 0) {
                this.leftSwingProgressInt = -1;
                this.leftIsSwingInProgress = true;
            }
        }
    }

    @SubscribeEvent
    public void onRenderHand(RenderSpecificHandEvent event) {
        if (event.getHand() == Hand.OFF_HAND && this.mc.player.getHeldItemOffhand().getItem() instanceof IOffhandAttackable) {
            event.setCanceled(true);
            float partialTicks = event.getPartialTicks();
            float pitch = event.getInterpolatedPitch();
            float rightSwingProgress = this.getRightSwingProgress(partialTicks);
            FirstPersonRenderer renderer = this.mc.getFirstPersonRenderer();
            float rightEquipProgress = 1.0F - MathHelper.lerp(partialTicks, this.rightPrevEquipProgress, this.rightEquipProgress);
            renderer.renderItemInFirstPerson(this.mc.player,
                                             partialTicks,
                                             pitch,
                                             Hand.OFF_HAND,
                                             rightSwingProgress,
                                             this.mc.player.getHeldItemOffhand(),
                                             rightEquipProgress);
            return;
        }
        if (event.getHand() == Hand.MAIN_HAND && this.requiresReequiping) {
            event.setCanceled(true);
            float partialTicks = event.getPartialTicks();
            float pitch = event.getInterpolatedPitch();
            float leftSwingProgress = this.getLeftSwingProgress(partialTicks);
            FirstPersonRenderer renderer = this.mc.getFirstPersonRenderer();
            float leftEquipProgress = 1.0F - MathHelper.lerp(partialTicks, this.leftPrevEquipProgress, this.leftEquipProgress);
            if (this.leftEquipProgress == 1 && this.leftPrevEquipProgress == 1 && this.leftTimeSinceLastHit != 0) {
                this.requiresReequiping = false;
            }
            renderer.renderItemInFirstPerson(this.mc.player,
                                             partialTicks,
                                             pitch,
                                             Hand.MAIN_HAND,
                                             leftSwingProgress,
                                             this.mc.player.getHeldItemMainhand(),
                                             leftEquipProgress);
        }
    }

    private float getRightSwingProgress(float partialTickTime) {
        float f = this.rightSwingProgress - this.rightPrevSwingProgress;
        if (f < 0.0F) {
            ++f;
        }
        return this.rightPrevSwingProgress + f * partialTickTime;
    }

    private float getLeftSwingProgress(float partialTickTime) {
        float f = this.leftSwingProgress - this.leftPrevSwingProgress;
        if (f < 0.0F) {
            ++f;
        }
        return this.leftPrevSwingProgress + f * partialTickTime;
    }

    @SubscribeEvent
    public void onPlayerInput(InputUpdateEvent event) {
        MovementInput movementInput = event.getMovementInput();
        this.isJumpPressed = movementInput.jump;
        if (!this.jump && this.mc.player.onGround && this.isJumpPressed && !this.proneToggle) {
            this.jump = true;
            return;
        }
        if (this.proneToggle && !this.mc.player.isOnLadder()) {
            movementInput.jump = false;
        }
    }

    @SubscribeEvent
    public void shutDownInternalServer(FMLServerStoppedEvent event) {
        if (this.inverted) {
            this.inverted = false;
            swapControls(this.mc);
        }
    }

    @SubscribeEvent
    public void onGUIOpen(GuiOpenEvent event) {
        if (event.getGui() instanceof InventoryScreen) {
            event.setCanceled(true);
            EvolutionNetwork.INSTANCE.sendToServer(new PacketCSOpenExtendedInventory());
        }
    }

    @SubscribeEvent
    public void onTextureStitch(TextureStitchEvent.Pre event) {
        //        if (!event.getMap().getBasePath().equals("textures")) {
        //            return;
        //        }
        //        event.addSprite(new ResourceLocation(Evolution.MODID, "block/clay"));
    }

    @SubscribeEvent
    public void onModelBake(ModelBakeEvent event) {
        //        event.getModelRegistry().put(new ModelResourceLocation(EvolutionBlocks.FANCYBLOCK.get().getRegistryName(), ""), new
        //        FancyBakedModel(DefaultVertexFormats.BLOCK));
        //        event.getModelRegistry().put(new ModelResourceLocation(EvolutionBlocks.MOLDING.get().getRegistryName(), "layers=1"), new
        //        ModelTEMolding(DefaultVertexFormats.BLOCK));
        //        event.getModelRegistry().put(new ModelResourceLocation(EvolutionBlocks.MOLDING.get().getRegistryName(), "layers=2"), new
        //        ModelTEMolding(DefaultVertexFormats.BLOCK));
        //        event.getModelRegistry().put(new ModelResourceLocation(EvolutionBlocks.MOLDING.get().getRegistryName(), "layers=3"), new
        //        ModelTEMolding(DefaultVertexFormats.BLOCK));
        //        event.getModelRegistry().put(new ModelResourceLocation(EvolutionBlocks.MOLDING.get().getRegistryName(), "layers=4"), new
        //        ModelTEMolding(DefaultVertexFormats.BLOCK));
        //        event.getModelRegistry().put(new ModelResourceLocation(EvolutionBlocks.MOLDING.get().getRegistryName(), "layers=5"), new
        //        ModelTEMolding(DefaultVertexFormats.BLOCK));
    }

    @SubscribeEvent
    public void onKeyboardEvent(InputEvent.KeyInputEvent event) {
        if (this.mc.currentScreen != null || this.mc.player == null) {
            return;
        }
        KeyBinding attack = this.mc.gameSettings.keyBindAttack;
        KeyBinding use = this.mc.gameSettings.keyBindUseItem;
        if (attack.getKey().getType() == InputMappings.Type.KEYSYM && attack.getKey().getKeyCode() == event.getKey() && event.getAction() == 1) {
            this.onLeftMouseClick();
        }
        if (use.getKey().getType() == InputMappings.Type.KEYSYM && use.getKey().getKeyCode() == event.getKey() && event.getAction() == 1) {
            this.onRightMouseClick();
        }
    }

    @SubscribeEvent
    public void onMouseEvent(InputEvent.MouseInputEvent event) {
        if (this.mc.currentScreen != null || this.mc.player == null) {
            return;
        }
        KeyBinding attack = this.mc.gameSettings.keyBindAttack;
        KeyBinding use = this.mc.gameSettings.keyBindUseItem;
        if (attack.getKey().getType() == InputMappings.Type.MOUSE && attack.getKey().getKeyCode() == event.getButton() && event.getAction() == 1) {
            this.onLeftMouseClick();
        }
        if (use.getKey().getType() == InputMappings.Type.MOUSE && use.getKey().getKeyCode() == event.getButton() && event.getAction() == 1) {
            this.onRightMouseClick();
        }
    }

    //Handle mainhand attack
    private void onLeftMouseClick() {
        float cooldown = this.mc.player.getCooldownPeriod();
        if (this.leftTimeSinceLastHit >= cooldown && this.mc.objectMouseOver.getType() != RayTraceResult.Type.BLOCK) {
            this.requiresReequiping = true;
            this.leftTimeSinceLastHit = 0;
            EvolutionNetwork.INSTANCE.sendToServer(new PacketCSPlayerAttack(this.leftPointedEntity, Hand.MAIN_HAND));
            this.swingArm(Hand.MAIN_HAND);
        }
    }

    //Handle offhand attack
    private void onRightMouseClick() {
        if (this.mc.player.isCreative() && this.mc.player.isSneaking() && this.mc.player.getHeldItemMainhand().getItem() instanceof BlockItem) {
            if (this.mc.objectMouseOver instanceof BlockRayTraceResult) {
                BlockPos pos = ((BlockRayTraceResult) this.mc.objectMouseOver).getPos();
                if (this.mc.world.getBlockState(pos).getBlock() != Blocks.AIR) {
                    EvolutionNetwork.INSTANCE.sendToServer(new PacketCSChangeBlock((BlockRayTraceResult) this.mc.objectMouseOver));
                    ObfuscationReflectionHelper.setPrivateValue(Minecraft.class, this.mc, 10, "field_71467_ac");
                }
                return;
            }
        }
        Item offhandItem = this.mc.player.getHeldItemOffhand().getItem();
        if (!(offhandItem instanceof IOffhandAttackable)) {
            return;
        }
        ItemStack mainHandStack = this.mc.player.getHeldItemMainhand();
        float cooldown = getRightCooldownPeriod((IOffhandAttackable) offhandItem);
        if (offhandItem instanceof IOffhandAttackable &&
            this.rightTimeSinceLastHit >= cooldown &&
            mainHandStack.getUseAction() == UseAction.NONE &&
            this.mc.objectMouseOver.getType() != RayTraceResult.Type.BLOCK) {
            this.rightTimeSinceLastHit = 0;
            EvolutionNetwork.INSTANCE.sendToServer(new PacketCSPlayerAttack(this.rightPointedEntity, Hand.OFF_HAND));
            this.swingArm(Hand.OFF_HAND);
        }
    }
}
