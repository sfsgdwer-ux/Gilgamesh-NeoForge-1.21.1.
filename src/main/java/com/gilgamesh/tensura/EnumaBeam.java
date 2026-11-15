package com.gilgamesh.tensura;

import com.mojang.math.Transformation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Persistent 3D Enuma Elish visuals using built-in ItemDisplay entities.
 * Two elongated 3D meshes and three animated rings are recycled while held,
 * rather than respawning entities every frame. Original assets in this project;
 * no GeckoLib, OptiFine, KubeJS or third-party renderer dependency.
 */
@EventBusSubscriber(modid=GilgameshMod.MODID)
public final class EnumaBeam {
    private static final double RANGE = 64.0;
    private static final int CHARGE_TICKS = 12;
    private static final double OVERDRIVE_RIBBON_WIDTH = 1.76;
    private static final double OVERDRIVE_IMPACT_WIDTH = 2.3;
    // Keep the visible beam outside the first-person camera and in front of
    // the player's third-person silhouette. Hit detection still starts near the eye.
    private static final double VFX_FORWARD_OFFSET = 4.35;
    private static final double VFX_RADIUS_MIN = 0.58;
    private static final double VFX_RADIUS_MAX = 0.84;
    private static final float DAMAGE_PER_PULSE = 320f;
    private static final int MAX_BLOCKS_PER_SWEEP = 32;
    private static final DustParticleOptions CRIMSON =
        new DustParticleOptions(new Vector3f(1f,0.11f,0.16f),1.7f);
    private static final DustParticleOptions GOLD =
        new DustParticleOptions(new Vector3f(1f,0.78f,0.15f),1.15f);

    private static final Map<UUID,Visuals> VISUALS = new HashMap<>();

    private static final class Visuals {
        final ServerLevel level;
        final Display.ItemDisplay core;
        final Display.ItemDisplay aura;
        final Display.ItemDisplay[] rings = new Display.ItemDisplay[3];
        final Display.ItemDisplay[] spirals = new Display.ItemDisplay[2];
        final Display.ItemDisplay[] shocks = new Display.ItemDisplay[2];
        long heartbeat;

        Visuals(ServerPlayer player) {
            level = player.serverLevel();
            heartbeat = level.getGameTime();
            core = spawn(level,player,new ItemStack(GilgameshMod.ENUMA_BEAM_CORE.get()));
            aura = spawn(level,player,new ItemStack(GilgameshMod.ENUMA_BEAM_AURA.get()));
            for(int i=0;i<3;i++)
                rings[i] = spawn(level,player,new ItemStack(GilgameshMod.ENUMA_BEAM_RING.get()));
            for(int i=0;i<2;i++) {
                spirals[i] = spawn(level,player,new ItemStack(GilgameshMod.ENUMA_SPIRAL.get()));
                shocks[i] = spawn(level,player,new ItemStack(GilgameshMod.ENUMA_IMPACT.get()));
            }
        }
    }

    private EnumaBeam() {}

    private static Display.ItemDisplay spawn(ServerLevel level,ServerPlayer player,ItemStack stack) {
        Display.ItemDisplay display = new Display.ItemDisplay(EntityType.ITEM_DISPLAY,level);
        display.setItemStack(stack);
        display.setItemTransform(ItemDisplayContext.FIXED);
        display.setNoGravity(true);
        display.setPos(player.getX(),player.getY()+1.5,player.getZ());
        level.addFreshEntity(display);
        return display;
    }

    private static void clear(Visuals visuals) {
        if(!visuals.core.isRemoved()) visuals.core.discard();
        if(!visuals.aura.isRemoved()) visuals.aura.discard();
        for(Display.ItemDisplay d : visuals.rings)
            if(d!=null && !d.isRemoved()) d.discard();
        for(Display.ItemDisplay d : visuals.spirals)
            if(d!=null && !d.isRemoved()) d.discard();
        for(Display.ItemDisplay d : visuals.shocks)
            if(d!=null && !d.isRemoved()) d.discard();
    }

    public static void stop(ServerPlayer player) {
        Visuals visuals = VISUALS.remove(player.getUUID());
        if(visuals!=null) clear(visuals);
    }

    /** Called once per held tick by native ManasCore/Tensura Skill callbacks. */
    public static void fire(ServerPlayer player,int heldTicks) {
        ServerLevel level = player.serverLevel();
        Visuals visuals=VISUALS.get(player.getUUID());
        if(visuals!=null && visuals.level!=level) {
            clear(visuals);
            VISUALS.remove(player.getUUID());
            visuals=null;
        }
        if(visuals==null) {
            visuals=new Visuals(player);
            VISUALS.put(player.getUUID(),visuals);
        }
        visuals.heartbeat=level.getGameTime();

        Vec3 look=player.getLookAngle().normalize();
        Vec3 collisionStart=player.getEyePosition().add(look.scale(0.55));
        Vec3 far=collisionStart.add(look.scale(RANGE));
        var hit=level.clip(new ClipContext(collisionStart,far,
            ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player));
        Vec3 end=hit.getType()==HitResult.Type.MISS?far:hit.getLocation();
        double hitDistance=collisionStart.distanceTo(end);
        // The beam keeps the same game rules, but its rendered volume is
        // intentionally much thinner. Previously radius*5 blocked the camera.
        double damageRadius=1.25+Math.min(1.6,heldTicks*0.055);
        double visibleRadius=Math.min(VFX_RADIUS_MAX,
            VFX_RADIUS_MIN+heldTicks*0.006);
        Vec3 visualStart=collisionStart.add(look.scale(VFX_FORWARD_OFFSET));
        double length=Math.max(0.0,hitDistance-VFX_FORWARD_OFFSET);
        Vec3 center=visualStart.add(look.scale(length*0.5));

        Quaternionf facing=new Quaternionf().rotationTo(
            new Vector3f(0,0,1),
            new Vector3f((float)look.x,(float)look.y,(float)look.z).normalize());
        boolean firing=heldTicks>=CHARGE_TICKS && length>=0.25;
        if(!firing) {
            // Charge phase: keep the entire beam hidden until the sword is
            // ready. Show multiple tight rotating rings ahead of the camera.
            hide(visuals.core,visualStart,facing);
            hide(visuals.aura,visualStart,facing);
            for(Display.ItemDisplay strand:visuals.spirals)
                hide(strand,visualStart,facing);
            for(Display.ItemDisplay impact:visuals.shocks)
                hide(impact,visualStart,facing);
            double progress=Math.min(1,Math.max(0,heldTicks/(double)CHARGE_TICKS));
            double chargeScale=Math.max(0.05,progress*progress);
            for(int i=0;i<visuals.rings.length;i++) {
                Vec3 at=visualStart.add(look.scale(0.4+i*0.48));
                float pulse=(float)((0.44+0.10*i+chargeScale*0.5)*visibleRadius);
                pose(visuals.rings[i],at,
                    new Quaternionf(facing).rotateZ(heldTicks*(i==1?-0.16f:0.13f)),
                    pulse,pulse,0.14f);
            }
            if(heldTicks%2==0 && length>0.25)
                drawChargeParticles(level,visualStart,look,heldTicks,visibleRadius);
            return;
        }

        // Two solid-looking but translucent layers, decorated with two
        // independently rotating helices and crimson-black fractures.
        float breathing=(float)(1.0+0.045*Math.sin(heldTicks*0.18));
        pose(visuals.core,center,new Quaternionf(facing),
            (float)(visibleRadius*1.45),(float)(visibleRadius*1.45),(float)length);
        pose(visuals.aura,center,new Quaternionf(facing).rotateZ(heldTicks*0.095f),
            (float)(visibleRadius*1.35*breathing),
            (float)(visibleRadius*1.35*breathing),(float)length);
        for(int i=0;i<2;i++) {
            float roll=heldTicks*(i==0?0.165f:-0.13f)+(float)(i*Math.PI);
            float r=(float)(visibleRadius*OVERDRIVE_RIBBON_WIDTH*(i==0?1.0:0.92));
            pose(visuals.spirals[i],center,new Quaternionf(facing).rotateZ(roll),
                r,r,(float)length);
        }

        // Source/middle/impact runes rotate at independent speeds. Unlike the
        // old giant discs, all stay within a narrow band around the attack.
        for(int i=0;i<3;i++) {
            double offset=i==2?Math.max(0.5,length-1.2):
                Math.min(length*0.72,0.8+i*4.1);
            Vec3 ringPos=visualStart.add(look.scale(offset));
            float angle=heldTicks*(i==1?-0.12f:0.11f)+(float)(i*Math.PI/3.0);
            float size=(float)(visibleRadius*(1.35+0.18*i));
            pose(visuals.rings[i],ringPos,new Quaternionf(facing).rotateZ(angle),
                size,size,0.20f);
        }

        // Cinematic impact ripples, synchronized with the attack but never
        // enormous white flashes. The wave alternates with a gold outline.
        for(int i=0;i<visuals.shocks.length;i++) {
            float wave=((heldTicks-CHARGE_TICKS+i*6)%12)/12f;
            float scale=(float)(visibleRadius*OVERDRIVE_IMPACT_WIDTH*
                (0.48+wave*1.35));
            pose(visuals.shocks[i],end.subtract(look.scale(0.28)),
                new Quaternionf(facing).rotateZ(
                    heldTicks*(i==0?0.055f:-0.08f)+i*0.8f),
                scale,scale,0.24f);
        }

        if(heldTicks%2==0) {
            drawParticles(level,visualStart,look,length,visibleRadius,heldTicks);
            damageEntities(level,player,collisionStart,end,damageRadius);
        }
        if(heldTicks%5==0) {
            destroyAlongBeam(level,player,collisionStart,end,damageRadius*0.82);
        }
    }

    private static void hide(Display.ItemDisplay d,Vec3 at,Quaternionf facing) {
        pose(d,at,new Quaternionf(facing),0.001f,0.001f,0.001f);
    }

    private static void drawChargeParticles(ServerLevel level,Vec3 origin,Vec3 look,
            int ticks,double radius) {
        Vec3 side=look.cross(new Vec3(0,1,0));
        if(side.lengthSqr()<0.00001) side=new Vec3(1,0,0);
        side=side.normalize();
        Vec3 up=side.cross(look).normalize();
        for(int i=0;i<8;i++) {
            double a=(i*2*Math.PI/8)+ticks*0.22;
            double r=radius*(1.8-ticks*0.075);
            Vec3 spark=origin.add(side.scale(Math.cos(a)*r))
                .add(up.scale(Math.sin(a)*r));
            level.sendParticles(i%3==0?GOLD:CRIMSON,
                spark.x,spark.y,spark.z,1,0,0,0,0);
        }
    }

    private static void pose(Display.ItemDisplay display,Vec3 position,
            Quaternionf rotation,float sx,float sy,float sz) {
        display.setPos(position.x,position.y,position.z);
        display.setTransformation(new Transformation(
            new Vector3f(),rotation,new Vector3f(sx,sy,sz),new Quaternionf()));
    }

    private static void drawParticles(ServerLevel level,Vec3 start,Vec3 look,
            double length,double radius,int ticks) {
        Vec3 side=look.cross(new Vec3(0,1,0));
        if(side.lengthSqr()<0.00001) side=new Vec3(1,0,0);
        side=side.normalize();
        Vec3 up=side.cross(look).normalize();
        // Keep particle packets bounded; 3D model provides the solid beam.
        int samples=Math.min(20,Math.max(4,(int)(length/3.6)));
        for(int i=0;i<samples;i++) {
            double dist=length*(i+0.5)/samples;
            Vec3 at=start.add(look.scale(dist));
            double a=ticks*0.42 + i*0.92;
            double r=radius*(0.82+0.12*Math.sin(ticks*0.09+i));
            Vec3 spark=at.add(side.scale(Math.cos(a)*r)).add(up.scale(Math.sin(a)*r));
            level.sendParticles(i%3==0?GOLD:CRIMSON,spark.x,spark.y,spark.z,1,0,0,0,0);
        }
        Vec3 tip=start.add(look.scale(length));
        level.sendParticles(ParticleTypes.END_ROD,tip.x,tip.y,tip.z,2,
            radius*0.13,radius*0.13,radius*0.13,0.013);
        // Impact spirals remain saturated crimson and gold; no white screen flash.
        if(ticks%4==0) for(int i=0;i<6;i++) {
            double a=i*Math.PI/3.0+ticks*0.23;
            Vec3 impact=tip.add(side.scale(Math.cos(a)*radius*1.3))
                .add(up.scale(Math.sin(a)*radius*1.3));
            level.sendParticles(i%2==0?GOLD:CRIMSON,
                impact.x,impact.y,impact.z,1,0,0,0,0);
        }
    }

    private static void damageEntities(ServerLevel level,ServerPlayer caster,
            Vec3 from,Vec3 to,double radius) {
        AABB area=new AABB(from,to).inflate(radius+2.0);
        for(LivingEntity target:level.getEntitiesOfClass(LivingEntity.class,area)) {
            if(target==caster || !target.isAlive() || caster.isAlliedTo(target)) continue;
            // Sphere-vs-segment is cheap and catches large entity hitboxes.
            Vec3 center=target.getBoundingBox().getCenter();
            double reach=radius + target.getBbWidth()*0.5;
            if(distanceSqr(center,from,to) <= reach*reach)
                target.hurt(level.damageSources().source(GilgameshMod.GENESIS_DAMAGE,caster),
                    DAMAGE_PER_PULSE);
        }
    }

    private static double distanceSqr(Vec3 point,Vec3 a,Vec3 b) {
        Vec3 ab=b.subtract(a);
        double length2=ab.lengthSqr();
        if(length2<0.000001) return point.subtract(a).lengthSqr();
        double projection=Math.max(0,Math.min(1,point.subtract(a).dot(ab)/length2));
        return point.subtract(a.add(ab.scale(projection))).lengthSqr();
    }

    /**
     * Expands through space only after the previous blocks are removed by
     * successive pulses. Caps work and respects unbreakables/block entities.
     */
    private static void destroyAlongBeam(ServerLevel level,ServerPlayer caster,
            Vec3 from,Vec3 to,double radius) {
        if(from.distanceToSqr(to)<0.2) return;
        Vec3 delta=to.subtract(from);
        Vec3 dir=delta.normalize();
        double length=from.distanceTo(to);
        Set<BlockPos> visited=new HashSet<>();
        int destroyed=0;
        for(double d=1.0;d<length && destroyed<MAX_BLOCKS_PER_SWEEP;d+=1.1) {
            Vec3 center=from.add(dir.scale(d));
            int minX=(int)Math.floor(center.x-radius), maxX=(int)Math.ceil(center.x+radius);
            int minY=(int)Math.floor(center.y-radius), maxY=(int)Math.ceil(center.y+radius);
            int minZ=(int)Math.floor(center.z-radius), maxZ=(int)Math.ceil(center.z+radius);
            for(int x=minX;x<=maxX && destroyed<MAX_BLOCKS_PER_SWEEP;x++)
                for(int y=minY;y<=maxY && destroyed<MAX_BLOCKS_PER_SWEEP;y++)
                    for(int z=minZ;z<=maxZ && destroyed<MAX_BLOCKS_PER_SWEEP;z++) {
                        double dx=x+0.5-center.x,dy=y+0.5-center.y,dz=z+0.5-center.z;
                        if(dx*dx+dy*dy+dz*dz>radius*radius) continue;
                        BlockPos pos=new BlockPos(x,y,z);
                        if(!visited.add(pos) || !level.hasChunkAt(pos)) continue;
                        BlockState state=level.getBlockState(pos);
                        if(state.isAir() || state.getDestroySpeed(level,pos)<0 ||
                            level.getBlockEntity(pos)!=null) continue;
                        if(level.destroyBlock(pos,false,caster)) destroyed++;
                    }
        }
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        Iterator<Map.Entry<UUID,Visuals>> it=VISUALS.entrySet().iterator();
        while(it.hasNext()) {
            var entry=it.next();
            Visuals visuals=entry.getValue();
            ServerPlayer player=event.getServer().getPlayerList().getPlayer(entry.getKey());
            if(player==null || player.isRemoved() || player.serverLevel()!=visuals.level ||
               visuals.level.getGameTime()-visuals.heartbeat>8) {
                clear(visuals);
                it.remove();
            }
        }
    }
}
