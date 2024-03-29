package tgw.evolution.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Score;
import org.jetbrains.annotations.Nullable;
import tgw.evolution.init.EvolutionDamage;
import tgw.evolution.init.EvolutionStats;
import tgw.evolution.util.physics.SI;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

public final class PlayerHelper {

    /**
     * The base attack damage of the Player, in HP.
     */
    public static final double ATTACK_DAMAGE = 2.5;
    /**
     * The base attack speed of the Player, in attacks / s.
     */
    public static final double ATTACK_SPEED = 2;
    /**
     * The base reach distance of the Player, in metres.
     */
    public static final double REACH_DISTANCE = 3.5;
    /**
     * The base Player mass in kg. Used in various kinetic calculations.
     */
    public static final double MASS = 70;
    /**
     * The Player max health, in HP.
     */
    public static final double MAX_HEALTH = 100;
    /**
     * The force the player uses to push its feet against the ground.
     */
    public static final double WALK_FORCE = 1_000 * SI.NEWTON;
    public static final double VOLUME = 84_000 * SI.CUBIC_CENTIMETER;
    public static final double LUNG_CAPACITY = 13.8 * SI.LITER;
    public static final EntityDimensions STANDING_SIZE = EntityDimensions.scalable(0.65F, 1.8F);
    public static final EntityDimensions SWIMMING_SIZE = EntityDimensions.scalable(0.65F, 0.65F);
    public static final Map<Pose, EntityDimensions> SIZE_BY_POSE = new EnumMap<>(Pose.class);
    private static final Scorer SCORER = new Scorer();

    static {
        SIZE_BY_POSE.put(Pose.STANDING, STANDING_SIZE);
        SIZE_BY_POSE.put(Pose.SLEEPING, EntityDimensions.fixed(0.2F, 0.2F));
        SIZE_BY_POSE.put(Pose.FALL_FLYING, EntityDimensions.scalable(0.65F, 0.65F));
        SIZE_BY_POSE.put(Pose.SWIMMING, SWIMMING_SIZE);
        SIZE_BY_POSE.put(Pose.SPIN_ATTACK, EntityDimensions.scalable(0.65F, 0.65F));
        SIZE_BY_POSE.put(Pose.CROUCHING, EntityDimensions.scalable(0.65F, 1.5F));
        SIZE_BY_POSE.put(Pose.DYING, EntityDimensions.fixed(0.2F, 0.2F));
    }

    private PlayerHelper() {
    }

    public static void addStat(Player player, ResourceLocation resLoc, float amount) {
        addStat(player, Stats.CUSTOM, resLoc, amount);
    }

    public static <T> void addStat(Player player, StatType<T> statType, T type, float amount) {
        if (player instanceof ServerPlayer p) {
            Stat<T> stat = statType.get(type);
            ServerStatsCounter stats = p.getStats();
            stats.incrementPartial(stat, amount);
            try (Scorer scorer = SCORER.setup(stats, stat)) {
                player.getScoreboard().forAllObjectives(stat, player.getScoreboardName(), scorer);
            }
        }
    }

    public static void applyDamage(Player player, float amount, EvolutionDamage.Type type, LivingEntity entity) {
        addStat(player, EvolutionStats.DAMAGE_DEALT_BY_TYPE.get(type), amount);
        addStat(player, EvolutionStats.DAMAGE_DEALT_BY_TYPE.get(EvolutionDamage.Type.MELEE), amount);
        addStat(player, EvolutionStats.DAMAGE_DEALT_BY_TYPE.get(EvolutionDamage.Type.TOTAL), amount);
        addStat(player, EvolutionStats.DAMAGE_DEALT, entity.getType(), amount);
    }

    public static void takeStat(Player player, Stat<?> stat) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.getStats().setValue_(stat, 0);
            player.getScoreboard().forAllObjectives(stat, player.getScoreboardName(), Score::reset);
        }
    }

    public static final class Scorer implements Consumer<Score>, AutoCloseable {

        private @Nullable Stat<?> stat;
        private @Nullable ServerStatsCounter stats;

        @Override
        public void accept(Score score) {
            assert this.stats != null;
            assert this.stat != null;
            score.setScore((int) this.stats.getValue_(this.stat));
        }

        @Override
        public void close() {
            this.stat = null;
            this.stats = null;
        }

        public Scorer setup(ServerStatsCounter stats, Stat<?> stat) {
            assert this.stats == null;
            assert this.stat == null;
            this.stats = stats;
            this.stat = stat;
            return this;
        }
    }
}
