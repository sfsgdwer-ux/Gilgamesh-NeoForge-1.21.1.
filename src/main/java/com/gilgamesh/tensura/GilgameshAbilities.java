package com.gilgamesh.tensura;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import io.github.manasmods.manascore.skill.api.SkillAPI;

/** Server-authoritative original gameplay approximations of the legacy modes. */
@EventBusSubscriber(modid=GilgameshMod.MODID)
public final class GilgameshAbilities {
    private static final String TERRITORY_KEY="gilgamesh_tensura_territory_v1";
    private static final String AXE_KEY="gilgamesh_tensura_axe_created";
    private static final Map<UUID,Binding> BINDS=new HashMap<>();

    private record Binding(ServerLevel level, UUID victim,Vec3 anchor,long expiry) {}

    public static void passiveGoldenRule(LivingEntity entity) {
        if(!entity.level().isClientSide() && entity.tickCount%100==0) {
            entity.addEffect(new MobEffectInstance(MobEffects.LUCK, 125, 4, true, false, false));
        }
    }

    private static LivingEntity aimedTarget(ServerPlayer player, double reach) {
        Vec3 eyes=player.getEyePosition();
        Vec3 end=eyes.add(player.getLookAngle().scale(reach));
        AABB area=new AABB(eyes,end).inflate(1.5);
        LivingEntity nearest=null;
        double best=reach*reach;
        for(LivingEntity candidate:player.serverLevel().getEntitiesOfClass(LivingEntity.class,area)) {
            if(candidate==player || !candidate.isAlive())continue;
            var point=candidate.getBoundingBox().inflate(0.4).clip(eyes,end);
            if(point.isEmpty())continue;
            double dist=eyes.distanceToSqr(point.get());
            if(dist<best){best=dist;nearest=candidate;}
        }
        return nearest;
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
        for(int k=0;k<36;k++) {
            Vec3 v=start.add(look.scale(k*1.6));
            level.sendParticles(ParticleTypes.END_ROD,v.x,v.y,v.z,3,0.14,0.14,0.14,0.002);
            level.sendParticles(ParticleTypes.FLAME,v.x,v.y,v.z,1,0.05,0.05,0.05,0.005);
        }
        LivingEntity target=aimedTarget(p,57);
        if(target!=null) {
            // TODO: true Genesis damage type and percentage-based magicule drain.
            target.hurt(p.damageSources().magic(),2000f);
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
                b.level.sendParticles(ParticleTypes.END_ROD,b.anchor.x,b.anchor.y+1,b.anchor.z,6,0.35,0.7,0.35,0.01);
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
                ally.heal(ally.getMaxHealth()*0.05f);
                ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION,90,2,true,false));
            }
        }
    }
}
