package com.gilgamesh.tensura;

import com.mojang.brigadier.CommandDispatcher;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import com.mojang.math.Transformation;

/**
 * All weapons in a wave spawn on the SAME tick, travel with a fixed world vector
 * and never use ItemEntity physics. Active gates are refreshed from the owner
 * each tick and old displays are always discarded.
 */
@EventBusSubscriber(modid=GilgameshMod.MODID)
public final class GoldenGates {
    private static final int GATES=5;
    private static final int WAVE=16;
    private static final int OPEN_TICKS=18;
    private static final int NEXT_WAVE_DELAY=22;
    private static final int SHOT_LIFE=19;
    private static final double SHOT_SPEED=2.7;
    private static final double[] X={-2.7,-1.38,0,1.38,2.7};
    private static final double[] Y={0.1,0.65,0.9,0.65,0.1};
    private static final Map<UUID,Active> ACTIVE=new HashMap<>();
    private static final DustParticleOptions GOLD =
        new DustParticleOptions(new Vector3f(1.0f,0.78f,0.11f),1.3f);
    private static final DustParticleOptions INNER =
        new DustParticleOptions(new Vector3f(1.0f,0.94f,0.40f),1.0f);

    private static final class Shot {
        final Display.ItemDisplay display;
        final Vec3 direction;
        final UUID owner;
        final ServerLevel level;
        int ticks;
        Shot(Display.ItemDisplay d,Vec3 dir,ServerPlayer p) {
            display=d;direction=dir;owner=p.getUUID();level=p.serverLevel();
        }
    }
    private static final class Active {
        final UUID owner;
        final ServerLevel level;
        final Display.ItemDisplay[] gates = new Display.ItemDisplay[GATES];
        final List<Shot> shots = new ArrayList<>();
        long heartbeat;
        long nextFire;
        long openingAt;
        int wave;
        Active(ServerPlayer p) {
            owner=p.getUUID(); level=p.serverLevel();heartbeat=level.getGameTime();
            openingAt=heartbeat; nextFire=heartbeat+OPEN_TICKS;
        }
    }

    public static void hold(ServerPlayer player, boolean pressed) {
        UUID id=player.getUUID();
        if (!pressed) {
            Active old=ACTIVE.remove(id);
            if (old!=null) close(old);
            return;
        }
        Active a=ACTIVE.get(id);
        if(a!=null) {
            a.heartbeat=player.serverLevel().getGameTime();
            return;
        }
        a=new Active(player);
        ACTIVE.put(id,a);
        for(int i=0;i<GATES;i++) {
            Display.ItemDisplay d=new Display.ItemDisplay(EntityType.ITEM_DISPLAY,player.serverLevel());
            d.setItemStack(new ItemStack(GilgameshMod.GATE_FRAME.get()));
            d.setItemTransform(ItemDisplayContext.FIXED);
            d.setNoGravity(true);
            d.setPos(player.getX(),player.getY()+2.1,player.getZ());
            player.serverLevel().addFreshEntity(d);
            a.gates[i]=d;
        }
        player.displayClientMessage(Component.literal("Golden Gates: opening 5 portals"),true);
    }

    private static void close(Active a) {
        for(Display.ItemDisplay g:a.gates) if(g!=null && !g.isRemoved()) g.discard();
        for(Shot s:a.shots) if(!s.display.isRemoved()) s.display.discard();
        a.shots.clear();
    }

    private static Vec3 horizontal(ServerPlayer p) {
        Vec3 look=p.getLookAngle();
        Vec3 flat=new Vec3(look.x,0,look.z);
        if(flat.lengthSqr()<0.0001) {
            float rad=(float)Math.toRadians(p.getYRot());
            flat=new Vec3(-Math.sin(rad),0,Math.cos(rad));
        }
        return flat.normalize();
    }
    private static Vec3 right(Vec3 forward) {
        return new Vec3(forward.z,0,-forward.x);
    }
    private static Vec3 gatePos(ServerPlayer p,int i,Vec3 forward,Vec3 right) {
        return p.position().subtract(forward.scale(2.55))
            .add(right.scale(X[i])).add(0,2.05+Y[i],0);
    }
    private static void poseGate(Display.ItemDisplay gate,Vec3 flat,float scale) {
        Quaternionf rot=new Quaternionf().rotationTo(
            new Vector3f(0,0,1),new Vector3f((float)flat.x,0,(float)flat.z));
        gate.setTransformation(new Transformation(
            new Vector3f(),rot,new Vector3f(scale,scale,scale),new Quaternionf()));
    }
    private static void poseShot(Display.ItemDisplay shot,Vec3 dir,ItemStack item) {
        // Vanilla 2D tool art is drawn diagonally; +Y alone makes the sprite
        // appear to fly sideways. Use the local hilt -> head diagonal.
        String id=item.getDescriptionId().toLowerCase(java.util.Locale.ROOT);
        boolean axe=item.getItem() instanceof AxeItem || id.contains("_axe");
        Vector3f modelAxis = axe
            ? new Vector3f(-1f,1f,0f).normalize()
            : new Vector3f(1f,1f,0f).normalize();
        Quaternionf q=new Quaternionf().rotationTo(
            modelAxis,
            new Vector3f((float)dir.x,(float)dir.y,(float)dir.z));
        shot.setTransformation(new Transformation(
            new Vector3f(),q,new Vector3f(1.25f,1.25f,1.25f),new Quaternionf()));
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post e) {
        Iterator<Map.Entry<UUID,Active>> iter=ACTIVE.entrySet().iterator();
        while(iter.hasNext()) {
            Active a=iter.next().getValue();
            ServerPlayer p=e.getServer().getPlayerList().getPlayer(a.owner);
            if(p==null || p.isRemoved() || p.serverLevel()!=a.level
               || a.level.getGameTime()-a.heartbeat>9) {
                close(a);iter.remove();continue;
            }
            try {update(a,p);} catch(Exception ex) {
                GilgameshLog.error("Golden Gates stopped safely",ex);
                close(a);iter.remove();
            }
        }
    }
    private static void update(Active a,ServerPlayer p) {
        long now=a.level.getGameTime();
        Vec3 forward=horizontal(p);
        Vec3 side=right(forward);
        float opening=Math.min(1f,Math.max(0.08f,(now-a.openingAt)/(float)OPEN_TICKS));
        for(int i=0;i<GATES;i++) {
            Display.ItemDisplay gate=a.gates[i];
            if(gate==null || gate.isRemoved()) continue;
            Vec3 center=gatePos(p,i,forward,side);
            gate.setPos(center.x,center.y,center.z);
            poseGate(gate,forward,2.6f*opening);
            if(now%3==0) drawPortal(a.level,center,side,opening);
        }
        Iterator<Shot> shots=a.shots.iterator();
        while(shots.hasNext()){
            Shot s=shots.next();
            s.ticks++;
            Vec3 next=s.display.position().add(s.direction.scale(SHOT_SPEED));
            s.display.setPos(next.x,next.y,next.z);
            if(s.ticks%2==0) {
                Vec3 trail=next.subtract(s.direction.scale(0.6));
                a.level.sendParticles(ParticleTypes.END_ROD,
                    trail.x,trail.y,trail.z,1,0.03,0.03,0.03,0.003);
            }
            boolean hit=false;
            // Sweep between positions: checking only 'next' skipped targets
            // when projectiles moved more than one block in one tick.
            AABB area=new AABB(s.display.position(),next).inflate(0.55);
            for(LivingEntity target:a.level.getEntitiesOfClass(LivingEntity.class,area)) {
                if(target.getUUID().equals(s.owner) || !target.isAlive()) continue;
                target.hurt(p.damageSources().playerAttack(p),7.0f);
                hit=true;break;
            }
            if(hit || s.ticks>=SHOT_LIFE || s.display.isRemoved()) {
                if(!s.display.isRemoved()) s.display.discard();
                shots.remove();
            }
        }
        if(now>=a.nextFire) {
            fireWave(a,p,forward,side);
            a.nextFire=now+NEXT_WAVE_DELAY+OPEN_TICKS;
            a.openingAt=now+NEXT_WAVE_DELAY;
        }
    }
    private static void drawPortal(ServerLevel level,Vec3 center,Vec3 right,float progress) {
        double rw=1.1*progress;
        double rh=1.4*progress;
        for(int j=0;j<14;j++) {
            double ang=2*Math.PI*j/14;
            Vec3 v=center.add(right.scale(Math.cos(ang)*rw)).add(0,Math.sin(ang)*rh,0);
            level.sendParticles(j%3==0?INNER:GOLD,v.x,v.y,v.z,1,0.01,0.02,0.01,0);
        }
        level.sendParticles(ParticleTypes.END_ROD,center.x,center.y,center.z,1,0.12,0.24,0.12,0.01);
    }
    private static void fireWave(Active a,ServerPlayer p,Vec3 forward,Vec3 side) {
        Container store=p.getEnderChestInventory();
        List<ItemStack> weapons=new ArrayList<>();
        for(int slot=0;slot<store.getContainerSize();slot++) {
            ItemStack stack=store.getItem(slot);
            if(!stack.isEmpty()) weapons.add(stack.copyWithCount(1));
        }
        if(weapons.isEmpty()) {a.wave=0;return;}
        int waves=(weapons.size()+WAVE-1)/WAVE;
        a.wave%=waves;
        int start=a.wave*WAVE;
        int limit=Math.min(start+WAVE,weapons.size());
        Vec3 aim=p.getLookAngle().normalize();
        for(int k=start;k<limit;k++) {
            int index=k-start;
            int gate=index%GATES;
            Vec3 center=gatePos(p,gate,forward,side);
            int row=index/GATES;
            double xOffset=(row-1)*0.34;
            double yOffset=(row%2==0 ? -0.1:0.18);
            Vec3 spawn=center.add(side.scale(xOffset)).add(0,yOffset,0);
            Display.ItemDisplay d=new Display.ItemDisplay(EntityType.ITEM_DISPLAY,a.level);
            d.setItemStack(weapons.get(k).copy());
            d.setItemTransform(ItemDisplayContext.FIXED);
            d.setNoGravity(true);
            d.setPos(spawn.x,spawn.y,spawn.z);
            poseShot(d,aim,weapons.get(k));
            a.level.addFreshEntity(d);
            a.shots.add(new Shot(d,aim,p));
        }
        // All the entities above are created in one server tick (no per-item delays).
        a.level.sendParticles(ParticleTypes.FLASH,p.getX(),p.getY()+2,p.getZ(),1,0,0,0,0);
        a.wave=(a.wave+1)%waves;
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("gilgamesh")
            .then(Commands.literal("on").executes(ctx->{
                ServerPlayer p=ctx.getSource().getPlayerOrException();
                hold(p,true);return 1;
            }))
            .then(Commands.literal("off").executes(ctx->{
                ServerPlayer p=ctx.getSource().getPlayerOrException();
                hold(p,false);return 1;
            }))
            .then(Commands.literal("count").executes(ctx->{
                ServerPlayer p=ctx.getSource().getPlayerOrException();
                int count=0;
                Container store=p.getEnderChestInventory();
                for(int i=0;i<store.getContainerSize();i++) if(!store.getItem(i).isEmpty()) count++;
                p.sendSystemMessage(Component.literal("Golden Gates: "+count+" filled slots, "+
                    ((count+WAVE-1)/WAVE)+" waves (max "+WAVE+" per wave)"));
                return 1;
            })));
    }
}
