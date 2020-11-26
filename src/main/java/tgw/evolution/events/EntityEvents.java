package tgw.evolution.events;

import net.minecraft.block.BlockState;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.merchant.villager.WanderingTraderEntity;
import net.minecraft.entity.monster.PhantomEntity;
import net.minecraft.entity.passive.horse.TraderLlamaEntity;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.fluid.IFluidState;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.Effects;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import tgw.evolution.Evolution;
import tgw.evolution.capabilities.inventory.PlayerInventoryCapability;
import tgw.evolution.capabilities.inventory.PlayerInventoryCapabilityProvider;
import tgw.evolution.entities.EntityGenericCreature;
import tgw.evolution.entities.EvolutionAttributes;
import tgw.evolution.entities.IAgressive;
import tgw.evolution.init.EvolutionDamage;
import tgw.evolution.init.EvolutionItems;
import tgw.evolution.init.EvolutionNetwork;
import tgw.evolution.inventory.ContainerExtendedHandler;
import tgw.evolution.network.PacketCSPlayerFall;
import tgw.evolution.util.Gravity;
import tgw.evolution.util.HarvestLevel;
import tgw.evolution.util.MathHelper;
import tgw.evolution.util.PlayerHelper;
import tgw.evolution.util.damage.EvDamageSource;
import tgw.evolution.util.damage.EvEntityDamageSource;
import tgw.evolution.util.damage.EvIndirectEntityDamageSource;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class EntityEvents {

    private static final Method SET_POSE_METHOD = ObfuscationReflectionHelper.findMethod(Entity.class, "func_213301_b", Pose.class);
    private static final Random RANDOM = new Random();
    private static final Set<DamageSource> IGNORED_DAMAGE_SOURCES = new HashSet<>();
    private static final Map<UUID, Double> PLAYER_MASS_MAP = new HashMap<>();

    static {
        IGNORED_DAMAGE_SOURCES.add(EvolutionDamage.FALL);
        IGNORED_DAMAGE_SOURCES.add(EvolutionDamage.FALLING_ROCK);
        IGNORED_DAMAGE_SOURCES.add(EvolutionDamage.IN_FIRE);
        IGNORED_DAMAGE_SOURCES.add(EvolutionDamage.ON_FIRE);
    }

    private final Map<DamageSource, EquipmentSlotType> damageMultipliers = new WeakHashMap<>();
    private final Map<UUID, Integer> playerTimeSinceLastHit = new HashMap<>();

    @SubscribeEvent
    public void onLivingHeal(LivingHealEvent event) {
        if (event.getEntityLiving() instanceof PlayerEntity) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onSetAttackTarget(LivingSetAttackTargetEvent event) {
        if (event.getEntity() instanceof LivingEntity) {
            MobEntity entity = (MobEntity) event.getEntity();
            if (entity.getAttackTarget() != null && event.getTarget() != null) {
                if (entity.isPotionActive(Effects.BLINDNESS)) {
                    int effectLevel = 3 - entity.getActivePotionEffect(Effects.BLINDNESS).getAmplifier();
                    if (effectLevel < 0) {
                        effectLevel = 0;
                    }
                    if (entity.getDistance(event.getTarget()) > effectLevel) {
                        entity.setAttackTarget(null);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerRespawns(PlayerEvent.PlayerRespawnEvent event) {
        ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
        ServerWorld world = (ServerWorld) player.world;
        BlockPos bedPos = player.getBedLocation(player.dimension);
        if (bedPos != null) {
            Optional<Vec3d> optional = PlayerEntity.func_213822_a(world, bedPos, false);
            if (optional.isPresent()) {
                return;
            }
        }
        BlockPos worldSpawnPos = world.getSpawnPoint();
        if (world.dimension.hasSkyLight() && world.getWorldInfo().getGameType() != GameType.ADVENTURE) {
            int spawnRadius = Math.max(0, player.server.func_184108_a(world));
            int worldBorder = MathHelper.floor(world.getWorldBorder().getClosestDistance(worldSpawnPos.getX(), worldSpawnPos.getZ()));
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
                    player.moveToBlockPosAndAngles(spawnPos, 0.0F, 0.0F);
                    if (world.areCollisionShapesEmpty(player)) {
                        player.setPositionAndUpdate(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                        break;
                    }
                }
            }
        }
    }

    private static int strangeRespawnFunc(int spawnArea) {
        return spawnArea <= 16 ? spawnArea - 1 : 17;
    }

    @SubscribeEvent
    public void onLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getWorld().isRemote) {
            return;
        }
        PlayerEntity player = event.getEntityPlayer();
        if (player == null) {
            return;
        }
        //Makes players able to hit through non-collidable blocks
        BlockState state = event.getWorld().getBlockState(event.getPos());
        if (!state.getCollisionShape(event.getWorld(), event.getPos()).isEmpty()) {
            return;
        }
        EntityRayTraceResult rayTraceResult = MathHelper.rayTraceEntityFromEyes(player,
                                                                                1,
                                                                                player.getAttribute(PlayerEntity.REACH_DISTANCE).getValue());
        if (rayTraceResult != null) {
            player.attackTargetEntityWithCurrentItem(rayTraceResult.getEntity());
            player.resetCooldown();
        }
    }

    @SubscribeEvent
    public void onEntityCreated(EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();
        //Prevents Phantoms, Trader Llamas and Wandering Traders from spawning
        if (entity instanceof PhantomEntity || entity instanceof TraderLlamaEntity || entity instanceof WanderingTraderEntity) {
            event.setCanceled(true);
            return;
        }
        if (!(entity instanceof PlayerEntity) && !(entity instanceof EntityGenericCreature)) {
            return;
        }
        //Sets the gravity of the Living Entities and the Player to be that of the dimension they're in
        ((LivingEntity) entity).getAttribute(LivingEntity.ENTITY_GRAVITY).setBaseValue(Gravity.gravity(entity.world.getDimension()));
        if (entity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) entity;
            player.getAttribute(SharedMonsterAttributes.ATTACK_SPEED).setBaseValue(PlayerHelper.ATTACK_SPEED);
            player.getAttribute(PlayerEntity.REACH_DISTANCE).setBaseValue(PlayerHelper.REACH_DISTANCE);
            if (player.world.isRemote) {
                player.abilities.setWalkSpeed((float) PlayerHelper.WALK_SPEED);
            }
            else {
                ObfuscationReflectionHelper.setPrivateValue(PlayerAbilities.class,
                                                            player.abilities,
                                                            (float) PlayerHelper.WALK_SPEED,
                                                            "field_149495_f");
            }
            player.getAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(PlayerHelper.WALK_SPEED);
        }
//        else {
//            //Makes the Living Entities able to step up one block, instead of jumping (it looks better)
//            entity.stepHeight = 1.0625F;
//        }
    }

    @SubscribeEvent
    public void onLivingFall(LivingFallEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof PlayerEntity) && !(entity instanceof EntityGenericCreature)) {
            return;
        }
        double velocity = entity.getMotion().y;
        double fallDistanceSlowDown = 1 - event.getDamageMultiplier();
        if (entity instanceof PlayerEntity) {
            if (entity.world.isRemote) {
                EvolutionNetwork.INSTANCE.sendToServer(new PacketCSPlayerFall(velocity, fallDistanceSlowDown));
            }
            return;
        }
        calculateFallDamage(entity, velocity, fallDistanceSlowDown);
    }

    public static void calculateFallDamage(Entity entity, double velocity, double distanceOfSlowDown) {
        //Convert from m/t to m/s
        velocity *= 20;
        double legHeight = PlayerHelper.LEG_HEIGHT;
        double baseMass = PlayerHelper.MASS;
        if (entity instanceof EntityGenericCreature) {
            EntityGenericCreature creature = (EntityGenericCreature) entity;
            legHeight = creature.getLegHeight();
            baseMass = creature.getMass();
        }
        distanceOfSlowDown += legHeight;
        double totalMass = baseMass;
        if (entity instanceof PlayerEntity) {
            IAttributeInstance massAttribute = ((PlayerEntity) entity).getAttribute(EvolutionAttributes.MASS);
            baseMass = massAttribute.getBaseValue();
            totalMass = massAttribute.getValue();
        }
        double kinecticEnergy = totalMass * velocity * velocity / 2;
        double forceOfImpact = kinecticEnergy / distanceOfSlowDown;
        double area = entity.getWidth() * entity.getWidth();
        double pressureOfFall = forceOfImpact / area;
        double maxSupportedPressure = baseMass / (area * 0.035);
        double deltaPressure = MathHelper.clampMin(pressureOfFall - maxSupportedPressure, 0);
        float amount = (float) Math.pow(deltaPressure, 1.7) / 750_000;
        if (amount >= 1) {
            entity.attackEntityFrom(EvolutionDamage.FALL, amount);
        }
    }

    @SubscribeEvent
    public void onLivingTick(LivingUpdateEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof PlayerEntity) && !(entity instanceof EntityGenericCreature)) {
            return;
        }
        //Deals damage inside blocks
        if (entity.isEntityInsideOpaqueBlock()) {
            entity.attackEntityFrom(EvolutionDamage.IN_WALL, 5.0F);
        }
        //Deals with water
        if (!entity.onGround && entity.isInWater()) {
            calculateWaterFallDamage(entity);
        }
        //Handles Player health regeneration
        if (!entity.world.isRemote && entity instanceof PlayerEntity && entity.world.getGameRules().getBoolean(GameRules.NATURAL_REGENERATION)) {
            PlayerEntity player = (PlayerEntity) entity;
            UUID uuid = player.getUniqueID();
            if (player.hurtTime > 0) {
                this.playerTimeSinceLastHit.put(uuid, 0);
            }
            else {
                this.playerTimeSinceLastHit.put(uuid, this.playerTimeSinceLastHit.getOrDefault(uuid, 0) + 1);
            }
            if (this.playerTimeSinceLastHit.get(uuid) == 80) {
                this.playerTimeSinceLastHit.put(uuid, 0);
                if (player.shouldHeal()) {
                    float currentHealth = MathHelper.clampMin(player.getHealth(), 1);
                    float healAmount = currentHealth / 100;
                    player.setHealth(currentHealth + healAmount);
                }
            }
        }
    }

    private static void calculateWaterFallDamage(Entity entity) {
        if (entity instanceof ServerPlayerEntity) {
            return;
        }
        BlockPos pos = entity.getPosition();
        IFluidState fluidState = entity.world.getFluidState(pos);
        double distanceOfSlowDown = fluidState.getLevel() * 0.062_5;
        double velocity = entity.getMotion().y;
        if (entity instanceof ClientPlayerEntity) {
            EvolutionNetwork.INSTANCE.sendToServer(new PacketCSPlayerFall(velocity, distanceOfSlowDown));
            return;
        }
        calculateFallDamage(entity, velocity, distanceOfSlowDown);
    }

    @SubscribeEvent
    public void onPlayerAttack(AttackEntityEvent event) {
        //Cancels the default player attack. Attack is calculated in PlayerHelper.performAttack(PlayerEntity, Entity, Hand)
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onEntityKnockedBack(LivingKnockBackEvent event) {
        event.setCanceled(true);
        LivingEntity entity = event.getEntityLiving();
        double knockbackResistance = entity.getAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).getValue();
        if (knockbackResistance == 1) {
            return;
        }
        float strength = event.getStrength();
        double xRatio = event.getRatioX();
        double zRatio = event.getRatioZ();
        entity.isAirBorne = true;
        Vec3d motion = entity.getMotion();
        Vec3d knockbackVec = new Vec3d(xRatio, 0, zRatio).normalize().scale(strength);
        if (knockbackResistance > 0) {
            knockbackVec = knockbackVec.scale(1 - knockbackResistance);
        }
        entity.setMotion(motion.x - knockbackVec.x, 0, motion.z - knockbackVec.z);
        entity.velocityChanged = true;
    }

    @SubscribeEvent
    public void onPlayerBreakBlock(PlayerEvent.BreakSpeed event) {
        //If the block is breakable by hand, do nothing
        if (event.getState().getHarvestLevel() <= HarvestLevel.HAND) {
            return;
        }
        //Prevents players from breaking blocks if their tool cannot harvest the block
        if (!event.getPlayer().getHeldItemMainhand().canHarvestBlock(event.getState())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof PlayerEntity) {
            event.addCapability(Evolution.location("extended_inventory"), new PlayerInventoryCapabilityProvider());
        }
    }

    @SubscribeEvent
    public void cloneCapabilitiesEvent(PlayerEvent.Clone event) {
        try {
            ContainerExtendedHandler handler = (ContainerExtendedHandler) event.getOriginal()
                                                                               .getCapability(PlayerInventoryCapability.CAPABILITY_EXTENDED_INVENTORY)
                                                                               .orElseThrow(IllegalStateException::new);
            CompoundNBT nbt = handler.serializeNBT();
            ContainerExtendedHandler handlerClone = (ContainerExtendedHandler) event.getPlayer()
                                                                                    .getCapability(PlayerInventoryCapability.CAPABILITY_EXTENDED_INVENTORY)
                                                                                    .orElseThrow(IllegalStateException::new);
            handlerClone.deserializeNBT(nbt);
        }
        catch (Exception e) {
            Evolution.LOGGER.error("Could not clone player [" + event.getOriginal().getName() + "] extended inventory when changing dimensions");
        }
    }

    @SubscribeEvent
    public void onEntityDeath(LivingDeathEvent event) {
        if (event.getEntity().world.isRemote) {
            return;
        }
        if (event.getEntity() instanceof PlayerEntity) {
            //Drop contents of extended inventory if keepInventory is off
            PlayerEntity player = (PlayerEntity) event.getEntity();
            if (!player.world.getGameRules().get(GameRules.KEEP_INVENTORY).get()) {
                ContainerExtendedHandler handler =
                        (ContainerExtendedHandler) player.getCapability(PlayerInventoryCapability.CAPABILITY_EXTENDED_INVENTORY)
                                                                                    .orElseThrow(IllegalStateException::new);
                for (int i = 0; i < handler.getSlots(); i++) {
                    spawnDrops(handler.getStackInSlot(i), player.world, player.getPosition());
                    handler.setStackInSlot(i, ItemStack.EMPTY);
                }
            }
        }
    }

    private static void spawnDrops(ItemStack stack, World worldIn, BlockPos pos) {
        double xOffset = worldIn.getRandom().nextFloat() * 0.7F + 0.65F;
        double yOffset = worldIn.getRandom().nextFloat() * 0.7F + 0.65F;
        double zOffset = worldIn.getRandom().nextFloat() * 0.7F + 0.65F;
        ItemEntity entity = new ItemEntity(worldIn, pos.getX() + xOffset, pos.getY() + yOffset, pos.getZ() + zOffset, stack);
        entity.setPickupDelay(10);
        worldIn.addEntity(entity);
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntityLiving().world.isRemote) {
            return;
        }
        if (event.getEntityLiving() instanceof PlayerEntity) {
            EquipmentSlotType hitPart = this.damageMultipliers.remove(event.getSource());
            Evolution.LOGGER.debug("damage before = " + event.getAmount());
            Evolution.LOGGER.debug("hitPart = " + hitPart);
            EvolutionDamage.Type type = EvolutionDamage.Type.GENERIC;
            if (event.getSource() instanceof EvDamageSource) {
                type = ((EvDamageSource) event.getSource()).getType();
            }
            event.setAmount(PlayerHelper.getDamage(hitPart, (PlayerEntity) event.getEntityLiving(), event.getAmount(), type));
            Evolution.LOGGER.debug("damage after = " + event.getAmount());
        }
        if (event.getSource().getTrueSource() instanceof PlayerEntity) {
            event.getEntityLiving().hurtResistantTime = 0;
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            if (event.player.isCreative()) {
                event.player.getAttribute(PlayerEntity.REACH_DISTANCE).setBaseValue(12);
                event.player.getAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).removeModifier(EvolutionAttributes.WEIGHT_SLOWDOWN_MODIFIER);
                PLAYER_MASS_MAP.put(event.player.getUniqueID(), 0.0);
            }
            else {
                event.player.getAttribute(PlayerEntity.REACH_DISTANCE).setBaseValue(PlayerHelper.REACH_DISTANCE);
                double mass = event.player.getAttribute(EvolutionAttributes.MASS).getValue();
                double oldMass = PLAYER_MASS_MAP.getOrDefault(event.player.getUniqueID(), 0.0);
                if (mass != oldMass) {
                    PLAYER_MASS_MAP.put(event.player.getUniqueID(), mass);
                    IAttributeInstance speed = event.player.getAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
                    speed.removeModifier(EvolutionAttributes.WEIGHT_SLOWDOWN_MODIFIER);
                    speed.applyModifier(new AttributeModifier(EvolutionAttributes.WEIGHT_SLOWDOWN_MODIFIER,
                                                              "Weight slow down",
                                                              PlayerHelper.getSpeed(mass),
                                                              AttributeModifier.Operation.ADDITION));
                }
            }
        }
        else if (event.phase == TickEvent.Phase.END) {
            if (Evolution.PRONED_PLAYERS.getOrDefault(event.player.getUniqueID(), false)) {
                event.player.setSprinting(false);
                event.player.stepHeight = getStepHeight(event.player);
                try {
                    SET_POSE_METHOD.invoke(event.player, Pose.SWIMMING);
                }
                catch (IllegalAccessException | InvocationTargetException e) {
                    Evolution.LOGGER.error("Error when setting player {} to prone: {}", event.player.getDisplayNameAndUUID(), e);
                }
            }
            else {
                event.player.stepHeight = 0.6f;
            }
            if (!event.player.world.isRemote) {
                //Put off torches in Water
                if (event.player.areEyesInFluid(FluidTags.WATER)) {
                    ItemStack mainHand = event.player.getHeldItemMainhand();
                    ItemStack offHand = event.player.getHeldItemOffhand();
                    boolean torch = false;
                    if (mainHand.getItem() == EvolutionItems.torch.get()) {
                        int count = mainHand.getCount();
                        event.player.setItemStackToSlot(EquipmentSlotType.MAINHAND, new ItemStack(EvolutionItems.stick.get(), count));
                        torch = true;
                    }
                    if (offHand.getItem() == EvolutionItems.torch.get()) {
                        int count = offHand.getCount();
                        event.player.setItemStackToSlot(EquipmentSlotType.OFFHAND, new ItemStack(EvolutionItems.stick.get(), count));
                        torch = true;
                    }
                    if (torch) {
                        event.player.world.playSound(null,
                                                     event.player.getPosition(),
                                                     SoundEvents.BLOCK_FIRE_EXTINGUISH,
                                                     SoundCategory.BLOCKS,
                                                     1.0F,
                                                     2.6F + (event.player.world.rand.nextFloat() - event.player.world.rand.nextFloat()) * 0.8F);
                    }
                }
            }
        }
    }

    private static float getStepHeight(PlayerEntity player) {
        IAttributeInstance mass = player.getAttribute(EvolutionAttributes.MASS);
        int baseMass = (int) mass.getBaseValue();
        int totalMass = (int) mass.getValue();
        int equipMass = totalMass - baseMass;
        float stepHeight = 1.062_5f - equipMass * 0.001_14f;
        return MathHelper.clampMin(stepHeight, 0.6f);
    }

    @SubscribeEvent
    public void onLivingAttack(LivingAttackEvent event) {
        if (event.getEntityLiving().world.isRemote) {
            return;
        }
        DamageSource source = event.getSource();
        if (!(source instanceof EvDamageSource)) {
            Evolution.LOGGER.debug("Canceling vanilla damage source: {}", source.damageType);
            event.setCanceled(true);
            return;
        }
        if (IGNORED_DAMAGE_SOURCES.contains(source)) {
            return;
        }
        float damage = event.getAmount();
        Evolution.LOGGER.debug("amount = " + damage);
        LivingEntity hitEntity = event.getEntityLiving();
        //Raytracing projectile damage
        if (source instanceof EvIndirectEntityDamageSource && source.isProjectile()) {
            if (hitEntity instanceof PlayerEntity) {
                EquipmentSlotType type = PlayerHelper.getPartByPosition(source.getImmediateSource().getBoundingBox().minY, (PlayerEntity) hitEntity);
                Evolution.LOGGER.debug("type ranged = {}", type);
                this.damageMultipliers.put(source, type);
            }
            return;
        }
        //Raytracing melee damage
        if (source instanceof EvEntityDamageSource) {
            Entity trueSource = source.getTrueSource();
            if (trueSource instanceof PlayerEntity && !(hitEntity instanceof PlayerEntity)) {
                return;
            }
            double range = 3;
            if (trueSource instanceof IAgressive) {
                range = ((IAgressive) trueSource).getReach();
            }
            else if (trueSource instanceof PlayerEntity) {
                range = ((PlayerEntity) trueSource).getAttribute(PlayerEntity.REACH_DISTANCE).getValue();
            }
            EntityRayTraceResult rayTrace = MathHelper.rayTraceEntityFromEyes(trueSource, 1.0F, range);
            if (rayTrace == null) {
                event.setCanceled(true);
                return;
            }
            Entity rayTracedEntity = rayTrace.getEntity();
            if (hitEntity.equals(rayTracedEntity)) {
                if (hitEntity instanceof PlayerEntity) {
                    EquipmentSlotType type = PlayerHelper.getPartByPosition(rayTrace.getHitVec().y, (PlayerEntity) hitEntity);
                    Evolution.LOGGER.debug("type = {}", type);
                    this.damageMultipliers.put(source, type);
                }
            }
            else {
                rayTracedEntity.attackEntityFrom(source, damage);
                event.setCanceled(true);
            }
            return;
        }
        Evolution.LOGGER.debug("Damage Source not calculated: " + source.damageType);
    }
}
