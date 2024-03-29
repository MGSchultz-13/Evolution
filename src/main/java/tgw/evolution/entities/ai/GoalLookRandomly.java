//package tgw.evolution.entities.ai;
//
//import net.minecraft.util.Mth;
//import net.minecraft.world.entity.ai.goal.Goal;
//import tgw.evolution.entities.EntityGenericCreature;
//
//import java.util.EnumSet;
//
//public class GoalLookRandomly extends Goal {
//    private final EntityGenericCreature entity;
//    private int idleTime;
//    private float lookX;
//    private float lookZ;
//
//    public GoalLookRandomly(EntityGenericCreature entitylivingIn) {
//        this.entity = entitylivingIn;
//        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
//    }
//
//    @Override
//    public boolean canContinueToUse() {
//        return this.idleTime >= 0 && !this.entity.isDead() && !this.entity.isSleeping();
//    }
//
//    @Override
//    public boolean canUse() {
//        return this.entity.getRandom().nextFloat() < 0.02F && !this.entity.isDead() && !this.entity.isSleeping();
//    }
//
//    @Override
//    public void start() {
//        float d0 = Mth.TWO_PI * this.entity.getRandom().nextFloat();
//        this.lookX = Mth.cos(d0);
//        this.lookZ = Mth.sin(d0);
//        this.idleTime = 20 + this.entity.getRandom().nextInt(20);
//    }
//
//    @Override
//    public void tick() {
//        --this.idleTime;
//        this.entity.getLookControl()
//                   .setLookAt(this.entity.getX() + this.lookX, this.entity.getY() + this.entity.getEyeHeight(), this.entity.getZ() + this.lookZ);
//    }
//}