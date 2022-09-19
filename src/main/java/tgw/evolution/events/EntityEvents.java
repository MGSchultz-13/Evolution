package tgw.evolution.events;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
import tgw.evolution.Evolution;
import tgw.evolution.capabilities.SerializableCapabilityProvider;
import tgw.evolution.capabilities.food.CapabilityHunger;
import tgw.evolution.capabilities.food.HungerStats;
import tgw.evolution.capabilities.food.IHunger;
import tgw.evolution.capabilities.health.CapabilityHealth;
import tgw.evolution.capabilities.health.HealthStats;
import tgw.evolution.capabilities.health.IHealth;
import tgw.evolution.capabilities.inventory.CapabilityInventory;
import tgw.evolution.capabilities.inventory.InventoryHandler;
import tgw.evolution.capabilities.stamina.CapabilityStamina;
import tgw.evolution.capabilities.stamina.IStamina;
import tgw.evolution.capabilities.stamina.StaminaStats;
import tgw.evolution.capabilities.temperature.CapabilityTemperature;
import tgw.evolution.capabilities.temperature.ITemperature;
import tgw.evolution.capabilities.temperature.TemperatureStats;
import tgw.evolution.capabilities.thirst.CapabilityThirst;
import tgw.evolution.capabilities.thirst.IThirst;
import tgw.evolution.capabilities.thirst.ThirstStats;
import tgw.evolution.capabilities.toast.CapabilityToast;
import tgw.evolution.capabilities.toast.ToastStats;
import tgw.evolution.entities.EntityGenericCreature;
import tgw.evolution.entities.IAgressive;
import tgw.evolution.entities.misc.EntityPlayerCorpse;
import tgw.evolution.init.*;
import tgw.evolution.network.*;
import tgw.evolution.patches.IBlockPatch;
import tgw.evolution.patches.IEntityPatch;
import tgw.evolution.util.PlayerHelper;
import tgw.evolution.util.Temperature;
import tgw.evolution.util.collection.O2ROpenHashMap;
import tgw.evolution.util.constants.HarvestLevel;
import tgw.evolution.util.constants.SkinType;
import tgw.evolution.util.damage.DamageSourceEntity;
import tgw.evolution.util.damage.DamageSourceEntityIndirect;
import tgw.evolution.util.damage.DamageSourceEv;
import tgw.evolution.util.damage.IHitLocation;
import tgw.evolution.util.earth.ClimateZone;
import tgw.evolution.util.earth.WindVector;
import tgw.evolution.util.math.MathHelper;
import tgw.evolution.util.math.Units;

import java.util.*;
import java.util.random.RandomGenerator;

public class EntityEvents {

    public static final WindVector WIND = new WindVector();
    public static final Map<UUID, SkinType> SKIN_TYPE = new O2ROpenHashMap<>();
    private static final RandomGenerator RANDOM = new Random();
    private final Map<DamageSource, EquipmentSlot> damageMultipliers = new WeakHashMap<>();

    public static void calculateFallDamage(LivingEntity entity, double velocity, double distanceOfSlowDown, boolean isWater) {
        velocity = Units.toSISpeed(velocity);
        double legHeight = PlayerHelper.LEG_HEIGHT;
        double baseMass = PlayerHelper.MASS;
        if (entity instanceof EntityGenericCreature creature) {
            legHeight = creature.getLegHeight();
            baseMass = creature.getBaseMass();
        }
        distanceOfSlowDown += legHeight;
        double totalMass = baseMass;
        if (entity instanceof Player) {
            AttributeInstance massAttribute = entity.getAttribute(EvolutionAttributes.MASS.get());
            assert massAttribute != null;
            baseMass = massAttribute.getBaseValue();
            totalMass = massAttribute.getValue();
        }
        double kineticEnergy = totalMass * velocity * velocity / 2;
        double forceOfImpact = kineticEnergy / distanceOfSlowDown;
        double area = entity.getBbWidth() * entity.getBbWidth();
        double pressureOfFall = forceOfImpact / area;
        double maxSupportedPressure = baseMass / (area * 0.035);
        double deltaPressure = Math.max(pressureOfFall - maxSupportedPressure, 0);
        float amount = (float) Math.pow(deltaPressure, 1.7) / 750_000;
        if (amount >= 1) {
            if (isWater) {
                entity.hurt(EvolutionDamage.WATER_IMPACT, amount);
            }
            else {
                entity.hurt(EvolutionDamage.FALL, amount);
            }
        }
    }

    public static void calculateWaterFallDamage(LivingEntity entity) {
        BlockPos pos = entity.blockPosition();
        FluidState fluidState = entity.level.getFluidState(pos);
        FluidState fluidStateDown = entity.level.getFluidState(pos.below());
        double distanceOfSlowDown = fluidState.getAmount() * 0.062_5;
        if (!fluidStateDown.isEmpty()) {
            distanceOfSlowDown += 1;
            FluidState fluidStateDown2 = entity.level.getFluidState(pos.below(2));
            if (!fluidStateDown2.isEmpty()) {
                distanceOfSlowDown += 1;
            }
        }
        double velocity = entity.getDeltaMovement().y;
        calculateFallDamage(entity, velocity, distanceOfSlowDown, true);
    }

    private static void spawnDrops(ItemStack stack, Level level, BlockPos pos) {
        double xOffset = level.getRandom().nextFloat() * 0.7F + 0.65F;
        double yOffset = level.getRandom().nextFloat() * 0.7F + 0.65F;
        double zOffset = level.getRandom().nextFloat() * 0.7F + 0.65F;
        ItemEntity entity = new ItemEntity(level, pos.getX() + xOffset, pos.getY() + yOffset, pos.getZ() + zOffset, stack);
        entity.setPickUpDelay(10);
        level.addFreshEntity(entity);
    }

    private static int strangeRespawnFunc(int spawnArea) {
        return spawnArea <= 16 ? spawnArea - 1 : 17;
    }

    @SubscribeEvent
    public void attachCapabilitiesEntities(AttachCapabilitiesEvent<Entity> event) {
        Entity entity = event.getObject();
        if (((IEntityPatch) entity).hasExtendedInventory()) {
            event.addCapability(Evolution.getResource("extended_inventory"),
                                new SerializableCapabilityProvider<>(CapabilityInventory.INSTANCE, new InventoryHandler(entity)));
        }
        if (entity instanceof Player player) {
            if (entity instanceof ServerPlayer) {
                event.addCapability(Evolution.getResource("thirst"),
                                    new SerializableCapabilityProvider<>(CapabilityThirst.INSTANCE, new ThirstStats()));
                event.addCapability(Evolution.getResource("health"),
                                    new SerializableCapabilityProvider<>(CapabilityHealth.INSTANCE, new HealthStats()));
                event.addCapability(Evolution.getResource("toast"), new SerializableCapabilityProvider<>(CapabilityToast.INSTANCE, new ToastStats()));
                event.addCapability(Evolution.getResource("hunger"),
                                    new SerializableCapabilityProvider<>(CapabilityHunger.INSTANCE, new HungerStats()));
                event.addCapability(Evolution.getResource("temperature"),
                                    new SerializableCapabilityProvider<>(CapabilityTemperature.INSTANCE, new TemperatureStats()));
                event.addCapability(Evolution.getResource("stamina"),
                                    new SerializableCapabilityProvider<>(CapabilityStamina.INSTANCE, new StaminaStats()));
            }
        }
    }

    @SubscribeEvent
    public void cloneCapabilitiesEvent(PlayerEvent.Clone event) {
        if (event.getOriginal().level.isClientSide) {
            return;
        }
        Player oldPlayer = event.getOriginal();
        Player newPlayer = event.getPlayer();
        EvolutionCapabilities.revive(oldPlayer);
        EvolutionCapabilities.clonePlayer(oldPlayer, newPlayer, CapabilityInventory.INSTANCE);
        EvolutionCapabilities.clonePlayer(oldPlayer, newPlayer, CapabilityThirst.INSTANCE);
        EvolutionCapabilities.clonePlayer(oldPlayer, newPlayer, CapabilityHealth.INSTANCE);
        EvolutionCapabilities.clonePlayer(oldPlayer, newPlayer, CapabilityToast.INSTANCE);
        EvolutionCapabilities.clonePlayer(oldPlayer, newPlayer, CapabilityHunger.INSTANCE);
        EvolutionCapabilities.clonePlayer(oldPlayer, newPlayer, CapabilityTemperature.INSTANCE);
        EvolutionCapabilities.clonePlayer(oldPlayer, newPlayer, CapabilityStamina.INSTANCE);
        EvolutionCapabilities.invalidate(oldPlayer);
    }

    @SubscribeEvent
    public void onEntityCreated(EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof LivingEntity living) {
            if (living instanceof Player player) {
                if (player.level.isClientSide) {
                    if (player.equals(Evolution.PROXY.getClientPlayer())) {
                        EvolutionNetwork.INSTANCE.sendToServer(new PacketCSSkinType());
                    }
                }
                else if (player instanceof ServerPlayer serverPlayer) {
                    Collection<MobEffectInstance> effects = player.getActiveEffects();
                    if (!effects.isEmpty()) {
                        for (MobEffectInstance instance : effects) {
                            //noinspection ObjectAllocationInLoop
                            EvolutionNetwork.send(serverPlayer, new PacketSCAddEffect(instance, PacketSCAddEffect.Logic.ADD));
                        }
                    }
                }
            }
            //TODO
//        else {
//            //Makes the Living Entities able to step up one block, instead of jumping (it looks better)
//            entity.stepHeight = 1.0625F;
//        }
        }
    }

    @SubscribeEvent
    public void onEntityKnockedBack(LivingKnockBackEvent event) {
        event.setCanceled(true);
        LivingEntity entity = event.getEntityLiving();
        double knockbackResistance = entity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
        if (knockbackResistance == 1) {
            return;
        }
        float strength = event.getStrength();
        double xRatio = event.getRatioX();
        double zRatio = event.getRatioZ();
        entity.hasImpulse = true;
        Vec3 motion = entity.getDeltaMovement();
        Vec3 knockbackVec = new Vec3(xRatio, 0, zRatio).normalize().scale(strength);
        if (knockbackResistance > 0) {
            knockbackVec = knockbackVec.scale(1 - knockbackResistance);
        }
        entity.setDeltaMovement(motion.x - knockbackVec.x, 0, motion.z - knockbackVec.z);
        entity.hurtMarked = true;
        if (entity instanceof ServerPlayer player) {
            EvolutionNetwork.send(player, new PacketSCUpdateCameraTilt(player));
        }
    }

    @SubscribeEvent
    public void onLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getWorld().isClientSide) {
            return;
        }
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        //Makes players able to hit through non-collidable blocks
        BlockState state = event.getWorld().getBlockState(event.getPos());
        if (!state.getCollisionShape(event.getWorld(), event.getPos()).isEmpty()) {
            return;
        }
        EntityHitResult rayTraceResult = MathHelper.rayTraceEntityFromEyes(player, 1, player.getAttributeValue(ForgeMod.REACH_DISTANCE.get()));
        if (rayTraceResult != null) {
            player.attack(rayTraceResult.getEntity());
            player.resetAttackStrengthTicker();
        }
    }

    @SubscribeEvent
    public void onLivingAttack(LivingAttackEvent event) {
        if (event.getEntityLiving().level.isClientSide) {
            return;
        }
        LivingEntity hitEntity = event.getEntityLiving();
        DamageSource source = event.getSource();
        if (!hitEntity.isAlive()) {
            return;
        }
        float damage = event.getAmount();
        if (hitEntity instanceof ServerPlayer player) {
            if (source instanceof DamageSourceEv sourceEv) {
                EvolutionDamage.Type damageType = sourceEv.getType();
                ResourceLocation resLoc = EvolutionStats.DAMAGE_TAKEN_RAW.get(damageType);
                if (resLoc != null) {
                    PlayerHelper.addStat(player, resLoc, damage);
                }
                if (source instanceof DamageSourceEntity) {
                    if (source instanceof DamageSourceEntityIndirect) {
                        PlayerHelper.addStat(player, EvolutionStats.DAMAGE_TAKEN_RAW.get(EvolutionDamage.Type.RANGED), damage);
                    }
                    else {
                        PlayerHelper.addStat(player, EvolutionStats.DAMAGE_TAKEN_RAW.get(EvolutionDamage.Type.MELEE), damage);
                    }
                }
                PlayerHelper.addStat(player, EvolutionStats.DAMAGE_TAKEN_RAW.get(EvolutionDamage.Type.TOTAL), damage);
            }
            else {
                Evolution.warn("Bad damage source: " + source);
            }
        }
        Evolution.debug("amount = " + damage);
        //Raytracing projectile damage
        if (source instanceof DamageSourceEntityIndirect && source.isProjectile()) {
            if (hitEntity instanceof Player hitPlayer) {
                EquipmentSlot hitLocation = PlayerHelper.getPartByPosition(source.getDirectEntity().getBoundingBox().minY, hitPlayer);
                ((DamageSourceEntityIndirect) source).setHitLocation(hitLocation);
                Evolution.debug("hitLocation ranged = {}", hitLocation);
            }
            return;
        }
        //Raytracing melee damage
        if (source instanceof DamageSourceEntity) {
            Entity trueSource = source.getEntity();
            if (trueSource instanceof Player) {
                return;
            }
            double range = 3;
            if (trueSource instanceof IAgressive agressive) {
                range = agressive.getReach();
            }
            EntityHitResult rayTrace = MathHelper.rayTraceEntityFromEyes(trueSource, 1.0F, range);
            if (rayTrace == null) {
                event.setCanceled(true);
                return;
            }
            Entity rayTracedEntity = rayTrace.getEntity();
            if (hitEntity.equals(rayTracedEntity)) {
                if (hitEntity instanceof Player player) {
                    EquipmentSlot type = PlayerHelper.getPartByPosition(rayTrace.getLocation().y, player);
                    Evolution.debug("type = {}", type);
                    this.damageMultipliers.put(source, type);
                }
            }
            else {
                rayTracedEntity.hurt(source, damage);
                event.setCanceled(true);
            }
            return;
        }
        Evolution.debug("Damage Source not calculated: " + source.msgId);
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level.isClientSide) {
            return;
        }
        LivingEntity killed = event.getEntityLiving();
        LivingEntity killer = killed.getKillCredit();
        if (killed instanceof Player player) {
            EntityPlayerCorpse corpse = new EntityPlayerCorpse(player);
            if (!player.level.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).get()) {
                corpse.setInventory(player);
            }
            player.level.addFreshEntity(corpse);
            if (killer != null) {
                killer.killed((ServerLevel) killer.level, player);
            }
            PlayerHelper.takeStat(player, Stats.CUSTOM.get(EvolutionStats.TIME_SINCE_LAST_DEATH));
            PlayerHelper.takeStat(player, Stats.CUSTOM.get(EvolutionStats.TIME_SINCE_LAST_REST));
            player.awardStat(EvolutionStats.DEATHS);
            DamageSource src = event.getSource();
            if (src instanceof DamageSourceEv) {
                ResourceLocation stat = EvolutionStats.DEATH_SOURCE.get(src.msgId);
                if (stat == null) {
                    if (!"fall_damage".equals(src.msgId)) {
                        Evolution.warn("Unknown stat for {}", src);
                    }
                }
                else {
                    player.awardStat(stat);
                }
            }
        }
        if (killer instanceof ServerPlayer killerPlayer) {
            EvolutionNetwork.send(killerPlayer, new PacketSCHitmarker(true));
        }
    }

    @SubscribeEvent
    public void onLivingFall(LivingFallEvent event) {
        LivingEntity entity = event.getEntityLiving();
        double velocity = entity.getDeltaMovement().y;
        double fallDistanceSlowDown = 1 - event.getDamageMultiplier();
        if (entity instanceof Player) {
            if (entity.level.isClientSide) {
                EvolutionNetwork.INSTANCE.sendToServer(new PacketCSPlayerFall(velocity, fallDistanceSlowDown));
            }
            return;
        }
        calculateFallDamage(entity, velocity, fallDistanceSlowDown, false);
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntityLiving().level.isClientSide) {
            return;
        }
        DamageSource source = event.getSource();
        if (event.getEntityLiving() instanceof Player player) {
            EquipmentSlot hitPart = this.damageMultipliers.remove(source);
            if (source instanceof IHitLocation hitLocation) {
                hitPart = hitLocation.getHitLocation();
            }
            EvolutionDamage.Type type = EvolutionDamage.Type.GENERIC;
            if (source instanceof DamageSourceEv sourceEv) {
                type = sourceEv.getType();
            }
            float computedDamage = PlayerHelper.getDamage(hitPart, player, event.getAmount(), type);
            if (computedDamage > 0) {
                float statDamage = computedDamage;
                if (statDamage > player.getHealth() + player.getAbsorptionAmount()) {
                    statDamage = player.getHealth() + player.getAbsorptionAmount();
                }
                ResourceLocation resLoc = EvolutionStats.DAMAGE_TAKEN_ACTUAL.get(type);
                if (resLoc != null) {
                    PlayerHelper.addStat(player, resLoc, statDamage);
                }
                if (source instanceof DamageSourceEntity) {
                    if (source instanceof DamageSourceEntityIndirect) {
                        PlayerHelper.addStat(player, EvolutionStats.DAMAGE_TAKEN_ACTUAL.get(EvolutionDamage.Type.RANGED), statDamage);
                    }
                    else {
                        PlayerHelper.addStat(player, EvolutionStats.DAMAGE_TAKEN_ACTUAL.get(EvolutionDamage.Type.MELEE), statDamage);
                    }
                    PlayerHelper.addStat(player, EvolutionStats.DAMAGE_TAKEN.get(), source.getEntity().getType(), statDamage);
                }
                PlayerHelper.addStat(player, EvolutionStats.DAMAGE_TAKEN_ACTUAL.get(EvolutionDamage.Type.TOTAL), statDamage);
            }
            event.setAmount(computedDamage);
        }
        Entity trueSource = source.getEntity();
        if (trueSource instanceof ServerPlayer sourcePlayer) {
            EvolutionNetwork.send(sourcePlayer, new PacketSCHitmarker(false));
        }
    }

    @SubscribeEvent
    public void onLivingJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntityLiving() instanceof ServerPlayer player) {
            player.awardStat(EvolutionStats.JUMPS);
        }
    }

    @SubscribeEvent
    public void onLivingTick(LivingUpdateEvent event) {
        LivingEntity entity = event.getEntityLiving();
        if (!(entity instanceof Player) && !(entity instanceof EntityGenericCreature)) {
            return;
        }
        //Removes damage immunity
        entity.invulnerableTime = 0;
        //Deals damage inside blocks
        if (entity.isInWall()) {
            if (entity.tickCount % 10 == 0) {
                entity.hurt(EvolutionDamage.IN_WALL, 5.0F);
            }
        }
    }

    /**
     * Cancels the default player attack. Attack is calculated in {@link PlayerHelper#performAttack(PlayerEntity, Entity, Hand, double)}
     */
    @SubscribeEvent
    public void onPlayerAttack(AttackEntityEvent event) {
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onPlayerBreakBlock(PlayerEvent.BreakSpeed event) {
        BlockState state = event.getState();
        //If the block is breakable by hand, do nothing
        if (((IBlockPatch) state.getBlock()).getHarvestLevel(state) <= HarvestLevel.HAND) {
            return;
        }
        //Prevents players from breaking blocks if their tool cannot harvest the block
        if (!event.getPlayer().getMainHandItem().isCorrectToolForDrops(event.getState())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onPlayerLogIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getPlayer();
        Level level = player.level;
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            for (Player otherPlayer : level.players()) {
                if (!otherPlayer.equals(player)) {
                    //noinspection ObjectAllocationInLoop
                    EvolutionNetwork.send(serverPlayer, new PacketSCFixRotation(otherPlayer));
                }
            }
            EvolutionNetwork.INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> player), new PacketSCFixRotation(player));
        }
    }

    @SubscribeEvent
    public void onPlayerLogOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getPlayer();
        player.awardStat(EvolutionStats.LEAVE_GAME);
        EvolutionNetwork.playerLogOut(player);
    }

    @SubscribeEvent
    public void onPlayerRespawns(PlayerEvent.PlayerRespawnEvent event) {
        ServerPlayer player = (ServerPlayer) event.getPlayer();
        player.getStats().markAllDirty();
        player.clearFire();
        ServerLevel world = (ServerLevel) player.level;
        Optional<BlockPos> bedPos = player.getSleepingPos();
        if (bedPos.isPresent()) {
            return;
        }
        BlockPos worldSpawnPos = world.getSharedSpawnPos();
        if (world.dimensionType().hasSkyLight() && world.getServer().getWorldData().getGameType() != GameType.ADVENTURE) {
            int spawnRadius = Math.max(0, player.server.getSpawnRadius(world));
            int worldBorder = Mth.floor(world.getWorldBorder().getDistanceToBorder(worldSpawnPos.getX(), worldSpawnPos.getZ()));
            if (worldBorder < spawnRadius) {
                spawnRadius = worldBorder;
            }
            if (worldBorder <= 1) {
                spawnRadius = 1;
            }
            long spawnDiameter = spawnRadius * 2L + 1;
            long spawnArea = spawnDiameter * spawnDiameter;
            int spawnAreaCasted = spawnArea > 2_147_483_647L ? Integer.MAX_VALUE : (int) spawnArea;
            int spawnAreaClamped = strangeRespawnFunc(spawnAreaCasted);
            int randomInt = RANDOM.nextInt(spawnAreaCasted);
            for (int l1 = 0; l1 < spawnAreaCasted; ++l1) {
                int i2 = (randomInt + spawnAreaClamped * l1) % spawnAreaCasted;
                int j2 = i2 % (spawnRadius * 2 + 1);
                int k2 = i2 / (spawnRadius * 2 + 1);
                BlockPos spawnPos = WorldEvents.findSpawn(world, worldSpawnPos.getX() + j2 - spawnRadius, worldSpawnPos.getZ() + k2 - spawnRadius);
                if (spawnPos != null) {
                    player.moveTo(spawnPos, 0.0F, 0.0F);
                    if (world.isUnobstructed(player)) {
                        player.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                        break;
                    }
                }
            }
        }
        for (Player otherPlayer : world.players()) {
            if (!otherPlayer.equals(player)) {
                //noinspection ObjectAllocationInLoop
                EvolutionNetwork.send(player, new PacketSCFixRotation(otherPlayer));
            }
        }
        EvolutionNetwork.INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> player), new PacketSCFixRotation(player));
        EvolutionCapabilities.revive(player);
        ITemperature temperature = EvolutionCapabilities.getCapabilityOrThrow(player, CapabilityTemperature.INSTANCE);
        ClimateZone.Region region = temperature.getRegion();
        temperature.setCurrentTemperature(Temperature.getBaseTemperatureForRegion(region));
        temperature.setCurrentMinComfort(Temperature.getMinComfortForRegion(region));
        temperature.setCurrentMaxComfort(Temperature.getMaxComfortForRegion(region));
        IStamina stamina = EvolutionCapabilities.getCapabilityOrThrow(player, CapabilityStamina.INSTANCE);
        stamina.setStamina(StaminaStats.MAX_STAMINA);
        EvolutionCapabilities.invalidate(player);
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Player player = event.player;
        ProfilerFiller profiler = player.level.getProfiler();
        if (event.phase == TickEvent.Phase.START) {
            profiler.push("preTick");
            profiler.push("reach");
            AttributeInstance reachDist = player.getAttribute(ForgeMod.REACH_DISTANCE.get());
            assert reachDist != null;
            if (player.isCreative()) {
                reachDist.setBaseValue(8);
            }
            else {
                reachDist.setBaseValue(PlayerHelper.REACH_DISTANCE);
            }
            profiler.pop();
            profiler.pop();
        }
        else if (event.phase == TickEvent.Phase.END) {
            profiler.push("postTick");
            profiler.push("stats");
            player.awardStat(EvolutionStats.TIME_PLAYED);
            if (player.getPose() == Pose.CROUCHING) {
                player.setSprinting(false);
                player.awardStat(EvolutionStats.TIME_SNEAKING);
            }
            if (!player.isSleeping()) {
                if (player.isAlive()) {
                    player.awardStat(EvolutionStats.TIME_SINCE_LAST_REST);
                }
            }
            else {
                PlayerHelper.takeStat(player, Stats.CUSTOM.get(EvolutionStats.TIME_SINCE_LAST_REST));
            }
            if (player.isAlive()) {
                player.awardStat(EvolutionStats.TIME_SINCE_LAST_DEATH);
            }
            profiler.popPush("status");
            //Handles Status Updates
            if (!player.level.isClientSide) {
                //Ticks Player systems
                if (!player.isCreative() && !player.isSpectator()) {
                    ServerPlayer sPlayer = (ServerPlayer) player;
                    EvolutionCapabilities.revive(player);
                    profiler.push("thirst");
                    IThirst thirst = EvolutionCapabilities.getCapabilityOrThrow(player, CapabilityThirst.INSTANCE);
                    thirst.tick(sPlayer);
                    profiler.popPush("health");
                    IHealth health = EvolutionCapabilities.getCapabilityOrThrow(player, CapabilityHealth.INSTANCE);
                    health.tick(sPlayer);
                    profiler.popPush("hunger");
                    IHunger hunger = EvolutionCapabilities.getCapabilityOrThrow(player, CapabilityHunger.INSTANCE);
                    hunger.tick(sPlayer);
                    profiler.popPush("temperature");
                    ITemperature temperature = EvolutionCapabilities.getCapabilityOrThrow(player, CapabilityTemperature.INSTANCE);
                    temperature.tick(sPlayer);
                    profiler.pop();
                    EvolutionCapabilities.invalidate(player);
                }
            }
            profiler.popPush("water");
            if (!player.level.isClientSide) {
                //Put off torches in Water
                if (player.isEyeInFluid(FluidTags.WATER)) {
                    ItemStack mainHand = player.getMainHandItem();
                    ItemStack offHand = player.getOffhandItem();
                    boolean torch = false;
                    if (mainHand.getItem() == EvolutionItems.torch.get()) {
                        int count = mainHand.getCount();
                        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(EvolutionItems.torch_unlit.get(), count));
                        torch = true;
                    }
                    if (offHand.getItem() == EvolutionItems.torch.get()) {
                        int count = offHand.getCount();
                        player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(EvolutionItems.torch_unlit.get(), count));
                        torch = true;
                    }
                    if (torch) {
                        player.level.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F,
                                               2.6F + (player.level.random.nextFloat() - player.level.random.nextFloat()) * 0.8F);
                    }
                }
            }
            profiler.pop();
            profiler.pop();
        }
    }

    @SubscribeEvent
    public void onPotionAdded(PotionEvent.PotionAddedEvent event) {
        LivingEntity entity = event.getEntityLiving();
        MobEffectInstance oldInstance = event.getOldPotionEffect();
//        if (oldInstance != null) {
//            oldInstance.getEffect().removeAttributeModifiers(entity, entity.getAttributes(), oldInstance.getAmplifier());
//        }
        MobEffectInstance newInstance = event.getPotionEffect();
//        newInstance.getEffect().addAttributeModifiers(entity, entity.getAttributes(), newInstance.getAmplifier());
        if (entity instanceof ServerPlayer player) {
            if (oldInstance == null) {
                EvolutionNetwork.send(player, new PacketSCAddEffect(newInstance, PacketSCAddEffect.Logic.ADD));
            }
            else {
                MobEffectInstance newEffect = new MobEffectInstance(oldInstance);
                newEffect.update(newInstance);
                boolean isSame = oldInstance.getAmplifier() == newEffect.getAmplifier();
                if (isSame) {
                    EvolutionNetwork.send(player, new PacketSCAddEffect(newEffect, PacketSCAddEffect.Logic.UPDATE));
                }
                else {
                    EvolutionNetwork.send(player, new PacketSCAddEffect(newEffect, PacketSCAddEffect.Logic.REPLACE));
                }
            }
        }
    }

    @SubscribeEvent
    public void onPotionExpired(PotionEvent.PotionExpiryEvent event) {
        if (!(event.getEntityLiving() instanceof ServerPlayer player)) {
            return;
        }
        MobEffectInstance effect = event.getPotionEffect();
        if (effect != null) {
            EvolutionNetwork.send(player, new PacketSCRemoveEffect(effect.getEffect()));
        }
    }

    @SubscribeEvent
    public void onPotionRemoved(PotionEvent.PotionRemoveEvent event) {
        if (!(event.getEntityLiving() instanceof ServerPlayer player)) {
            return;
        }
        EvolutionNetwork.send(player, new PacketSCRemoveEffect(event.getPotion()));
    }

    @SubscribeEvent
    public void onServerStart(ServerStartingEvent event) {
        EvolutionCommands.register(event.getServer().getCommands().getDispatcher());
    }

    @SubscribeEvent
    public void onServerStop(ServerStoppedEvent event) {
        EvolutionNetwork.resetCache();
    }

    @SubscribeEvent
    public void onSetAttackTarget(LivingSetAttackTargetEvent event) {
        if (event.getEntity() instanceof Mob mob) {
            if (mob.getTarget() != null && event.getTarget() != null) {
                MobEffectInstance effect = mob.getEffect(MobEffects.BLINDNESS);
                if (effect != null) {
                    int effectLevel = 3 - effect.getAmplifier();
                    if (effectLevel < 0) {
                        effectLevel = 0;
                    }
                    if (mob.distanceTo(event.getTarget()) > effectLevel) {
                        mob.setTarget(null);
                    }
                }
            }
        }
    }
}
