package com.gilgamesh.tensura;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.DustParticleOptions;
import org.joml.Vector3f;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.ClipContext;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.util.EnergyHelper;

/** Server-authoritative original gameplay approximations of the legacy modes. */
@EventBusSubscriber(modid=GilgameshMod.MODID)
public final class GilgameshAbilities {
    private static final String TERRITORY_KEY="gilgamesh_tensura_territory_v1";
    private static final String AXE_KEY="gilgamesh_tensura_axe_created";
    private static final Map<UUID,Binding> BINDS=new HashMap<>();
    private static final DustParticleOptions CHAIN_GOLD =
        new DustParticleOptions(new Vector3f(1.0f,0.78f,0.18f),1.0f);

    private record Binding(ServerLevel level, UUID victim,Vec3 anchor,long expiry) {}

    public static void passiveGoldenRule(LivingEntity entity) {
        if(!entity.level().isClientSide() && entity.tickCount%100==0) {
            entity.addEffect(new MobEffectInstance(MobEffects.LUCK, 125, 4, true, false, false));
        }
    }

    private static LivingEntity aimedTarget(ServerPlayer player, double reach) {
        Vec3 eyes=player.getEyePosition();
        Vec3 wanted=eyes.add(player.getLookAngle().normalize().scale(reach));
        var block=player.serverLevel().clip(new ClipContext(
            eyes,wanted,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player));
        Vec3 end=block.getType()==HitResult.Type.MISS?wanted:block.getLocation();
        AABB area=new AABB(eyes,end).inflate(1.5);
        LivingEntity nearest=null;
        double best=eyes.distanceToSqr(end);
        for(LivingEntity candidate:player.serverLevel().getEntitiesOfClass(LivingEntity.class,area)) {
            if(candidate==player || !candidate.isAlive())continue;
            var point=candidate.getBoundingBox().inflate(0.4).clip(eyes,end);
            if(point.isEmpty())continue;
            double dist=eyes.distanceToSqr(point.get());
            if(dist<best){best=dist;nearest=candidate;}
        }
        return nearest;
    }

    public static boolean isEnkiduBound(LivingEntity entity) {
        Binding bound=BINDS.get(entity.getUUID());
        return bound!=null && bound.level==entity.level()
            && bound.level.getGameTime()<bound.expiry;
    }

    /**
     * Resolve the selected world target once when Surround Attack begins.
     * Prefer an entity actually intersected by the player's crosshair;
     * otherwise respect the nearest solid block in the ray.
     */
    public static Vec3 selectSurroundTarget(ServerPlayer player) {
        LivingEntity target=aimedTarget(player,40);
        if(target!=null) return target.getBoundingBox().getCenter();
        Vec3 eye=player.getEyePosition();
        Vec3 end=eye.add(player.getLookAngle().normalize().scale(32));
        var block=player.serverLevel().clip(new ClipContext(
            eye,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player));
        return block.getType()==HitResult.Type.MISS?end:block.getLocation();
    }

    public static boolean hasEnkiduTarget(ServerPlayer p) {
        return aimedTarget(p,40)!=null;
    }

    public static boolean enkidu(ServerPlayer p, boolean mastered) {
        LivingEntity victim=aimedTarget(p,40);
        if(victim==null)return false;
        long until=p.serverLevel().getGameTime()+(mastered?480:240)*20L;
        BINDS.put(victim.getUUID(),new Binding(p.serverLevel(),victim.getUUID(),victim.position(),until));
        victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,200,255,false,true,true));
        p.displayClientMessage(Component.literal("Enkidu: holy chains bound target"),true);
        return true;
    }

    public static boolean hasTerritory(ServerPlayer p) {
        return p.getPersistentData().contains(TERRITORY_KEY);
    }

    public static void establishTerritory(ServerPlayer p) {
        if(hasTerritory(p))return;
        CompoundTag t=new CompoundTag();
        t.putString("dimension",p.serverLevel().dimension().location().toString());
        t.putInt("x",p.blockPosition().getX());
        t.putInt("z",p.blockPosition().getZ());
        p.getPersistentData().put(TERRITORY_KEY,t);
        p.displayClientMessage(Component.literal("Your permanent 250 x 250 territory has been established"),false);
    }

    public static boolean hasCreatedAxe(ServerPlayer p) {
        return p.getPersistentData().getBoolean(AXE_KEY);
    }
    public static void giveAxe(ServerPlayer p) {
        if(hasCreatedAxe(p))return;
        p.getPersistentData().putBoolean(AXE_KEY,true);
        ItemStack stack=new ItemStack(GilgameshMod.GENESIS_AXE.get());
        stack.set(net.minecraft.core.component.DataComponents.UNBREAKABLE,
                  new net.minecraft.world.item.component.Unbreakable(true));
        p.getInventory().placeItemBackInInventory(stack);
    }
    public static void giveEa(ServerPlayer p) {
        ItemStack stack=new ItemStack(GilgameshMod.EA_SWORD.get());
        p.getInventory().placeItemBackInInventory(stack);
    }
    public static boolean holdsEa(ServerPlayer p) {
        return p.getMainHandItem().is(GilgameshMod.EA_SWORD.get()) ||
               p.getOffhandItem().is(GilgameshMod.EA_SWORD.get());
    }

    /** Beam with golden visual streak; damage once per second. */
    public static void enumaElish(ServerPlayer p) {
        Vec3 start=p.getEyePosition();
        Vec3 look=p.getLookAngle().normalize();
        ServerLevel level=p.serverLevel();
        Vec3 far=start.add(look.scale(57));
        var block=level.clip(new ClipContext(
            start,far,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
        double beamLength=block.getType()==HitResult.Type.MISS?57:
            start.distanceTo(block.getLocation());
        for(int k=0;k<Math.ceil(beamLength/1.6);k++) {
            Vec3 v=start.add(look.scale(k*1.6));
            level.sendParticles(ParticleTypes.END_ROD,v.x,v.y,v.z,3,0.14,0.14,0.14,0.002);
            level.sendParticles(ParticleTypes.FLAME,v.x,v.y,v.z,1,0.05,0.05,0.05,0.005);
        }
        LivingEntity target=aimedTarget(p,57);
        if(target!=null) {
            target.hurt(level.damageSources().source(GilgameshMod.GENESIS_DAMAGE,p),2000f);
            // The wiki specifies 0.05% of target's maximum Magicules per second.
            double drain=Math.max(0,EnergyHelper.getMaxMagicule(target)*0.0005);
            EnergyHelper.isOutOfMagiculeConsuming(target,drain,0.0D);
        }
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        Iterator<Map.Entry<UUID,Binding>> it=BINDS.entrySet().iterator();
        while(it.hasNext()){
            Binding b=it.next().getValue();
            if(b.level.getGameTime()>=b.expiry) {it.remove();continue;}
            Entity ent=b.level.getEntity(b.victim);
            if(!(ent instanceof LivingEntity target)||!target.isAlive()) {it.remove();continue;}
            target.setDeltaMovement(Vec3.ZERO);
            target.setPos(b.anchor.x,b.anchor.y,b.anchor.z);
            if(b.level.getGameTime()%20==0) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,60,255,false,false));
                // Two intertwined gold spirals evoke Enkidu's holy chains.
                double height=Math.max(1.0,target.getBbHeight());
                for(int i=0;i<22;i++) {
                    double t=i/21.0;
                    for(int turn=0;turn<2;turn++) {
                        double phi=8*Math.PI*t+Math.PI*turn;
                        double x=b.anchor.x+0.65*Math.cos(phi);
                        double y=b.anchor.y+0.12+height*0.86*t;
                        double z=b.anchor.z+0.65*Math.sin(phi);
                        b.level.sendParticles(CHAIN_GOLD,x,y,z,1,0.005,0.005,0.005,0);
                    }
                }
                b.level.sendParticles(ParticleTypes.END_ROD,
                    b.anchor.x,b.anchor.y+height/2,b.anchor.z,4,0.3,0.3,0.3,0.01);
            }
        }
        if(event.getServer().getTickCount()%60!=0)return;
        for(ServerPlayer owner:event.getServer().getPlayerList().getPlayers()){
            if(!hasTerritory(owner)) continue;
            CompoundTag tag=owner.getPersistentData().getCompound(TERRITORY_KEY);
            if(!owner.serverLevel().dimension().location().toString().equals(tag.getString("dimension"))) continue;
            double cx=tag.getInt("x"),cz=tag.getInt("z");
            for(ServerPlayer ally:event.getServer().getPlayerList().getPlayers()){
                if(ally.serverLevel()!=owner.serverLevel())continue;
                if(ally!=owner && !owner.isAlliedTo(ally)) continue;
                if(Math.abs(ally.getX()-cx)>125 || Math.abs(ally.getZ()-cz)>125)continue;
                ally.heal(ally.getMaxHealth());
                EnergyHelper.gainMagicule(ally, EnergyHelper.getMaxMagicule(ally),
                    EnergyHelper.GainType.NORMAL);
                ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION,90,2,true,false));
            }
        }
    }
}
