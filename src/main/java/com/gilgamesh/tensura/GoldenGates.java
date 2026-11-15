package com.gilgamesh.tensura;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.commands.Commands;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.util.EnergyHelper;
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
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.ClipContext;
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
    private static final int MAX_GATES=9;
    private static final int WAVE=16;
    private static final int OPEN_TICKS=18;
    private static final int GATE_STAGGER_TICKS=3;
    private static final double COLUMN_GAP=3.65;
    private static final double ROW_GAP=3.45;
    // Fill the central portal first, then the sides, then above and below.
    // With nine portals this forms three complete rows of three.
    private static final int[][] GRID_ORDER = {
        {0,0}, {-1,0}, {1,0}, {0,1}, {0,-1}, {-1,1}, {1,1}, {-1,-1}, {1,-1}
    };
    private static final int NEXT_WAVE_DELAY=22;
    private static final int SHOT_LIFE=19;
    private static final double SHOT_SPEED=2.7;
    // Offsets are in blocks at the aim plane. First sword stays precisely
    // on the crosshair; additional swords spread horizontally and vertically.
    private static final double[][] FAN_OFFSETS={
        {0,0}, {-1,0}, {1,0}, {0,1}, {0,-1},
        {-1,1}, {1,1}, {-1,-1}, {1,-1},
        {-2,0}, {2,0}, {0,2}, {0,-2},
        {-2,1}, {2,1}, {-2,-1}
    };
    private static final double FAN_HORIZONTAL=1.2;
    private static final double FAN_VERTICAL=0.9;
    // Portals are driven by the number of stored weapons, not mastery.
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
        final float damage;
        Shot(Display.ItemDisplay d,Vec3 dir,ServerPlayer p,float damage) {
            display=d;direction=dir;owner=p.getUUID();level=p.serverLevel();
            this.damage=damage;
        }
    }
    private static final class Active {
        final UUID owner;
        final ServerLevel level;
        final Display.ItemDisplay[] gates = new Display.ItemDisplay[MAX_GATES];
        int gateCount;
        final long[] gateOpeningAt = new long[MAX_GATES];
        final boolean surround;
        final Vec3 target;
        final List<Shot> shots = new ArrayList<>();
        long heartbeat;
        long nextFire;
        int wave;
        Active(ServerPlayer p,boolean surround,int count) {
            owner=p.getUUID(); level=p.serverLevel();heartbeat=level.getGameTime();
            this.surround=surround; this.gateCount=Math.max(1,Math.min(MAX_GATES,count));
            this.target=surround?GilgameshAbilities.selectSurroundTarget(p):Vec3.ZERO;
            for(int i=0;i<gateCount;i++)
                gateOpeningAt[i]=heartbeat+i*GATE_STAGGER_TICKS;
            // Complete the staggered first opening before the initial volley.
            nextFire=heartbeat+OPEN_TICKS+(gateCount-1)*GATE_STAGGER_TICKS;
        }
    }

    /** Called from the native ManasCore/Tensura GilgameshSkill callbacks. */
    public static boolean isSkillActive(LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) return false;
        Active active = ACTIVE.get(player.getUUID());
        return active != null && active.level == player.serverLevel()
            && player.serverLevel().getGameTime() - active.heartbeat <= 9L;
    }

    /** Three weapons use one gate; every two beyond that adds one, up to 9. */
    public static int gatesForStorehouse(ServerPlayer player) {
        Container store=WeaponVault.get(player);
        int weapons=0;
        for(int i=0;i<store.getContainerSize();i++)
            if(WeaponVault.isWeapon(store.getItem(i))) weapons++;
        if(weapons==0) return 0;
        return Math.min(MAX_GATES,weapons<=3?1:1+(weapons-2)/2);
    }

    public static void skillHeld(LivingEntity entity,boolean surround,int count) {
        if (entity instanceof ServerPlayer player) hold(player, true, surround, count);
    }

    public static void skillReleased(LivingEntity entity) {
        if (entity instanceof ServerPlayer player) hold(player, false, false, 0);
    }

    private static void hold(ServerPlayer player, boolean pressed, boolean surround, int count) {
        UUID id=player.getUUID();
        if (!pressed) {
            Active old=ACTIVE.remove(id);
            if (old!=null) close(old);
            return;
        }
        Active a=ACTIVE.get(id);
        int wanted=Math.max(0,Math.min(MAX_GATES,count));
        if(wanted==0) {
            if(a!=null) close(a);
            ACTIVE.remove(id);
            return;
        }
        // Reuse old portal entities and only add/remove the difference when
        // the number of weapons changes during a held cast.
        if(a!=null && a.level==player.serverLevel() && a.surround==surround) {
            long now=player.serverLevel().getGameTime();
            if(wanted<a.gateCount) {
                for(int i=wanted;i<a.gateCount;i++) {
                    if(a.gates[i]!=null && !a.gates[i].isRemoved()) a.gates[i].discard();
                    a.gates[i]=null;
                }
            } else if(wanted>a.gateCount) {
                for(int i=a.gateCount;i<wanted;i++)
                    a.gateOpeningAt[i]=now+(i-a.gateCount)*GATE_STAGGER_TICKS;
            }
            a.gateCount=wanted;
            a.heartbeat=now;
            return;
        }
        if(a!=null)close(a);
        a=new Active(player,surround,wanted);
        ACTIVE.put(id,a);
        player.displayClientMessage(Component.literal(
            "Gate of Babylon: opening "+a.gateCount+" portals"),true);
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
    private static Vec3 gatePos(Active a,ServerPlayer p,int i,Vec3 forward,Vec3 right) {
        if(a.surround) {
            double t=2*Math.PI*i/a.gateCount;
            // A wider ring prevents the portals overlapping at high counts.
            double radius=Math.max(4.5,a.gateCount*0.65);
            return a.target.add(Math.cos(t)*radius,1.5+0.4*Math.sin(2*t),Math.sin(t)*radius);
        }
        int[] cell=GRID_ORDER[i];
        return p.position().subtract(forward.scale(4.0))
            .add(right.scale(cell[0]*COLUMN_GAP))
            .add(0,4.55+cell[1]*ROW_GAP,0);
    }
    private static void poseGate(Display.ItemDisplay gate,Vec3 flat,float scale) {
        Quaternionf rot=new Quaternionf().rotationTo(
            new Vector3f(0,0,1),new Vector3f((float)flat.x,(float)flat.y,(float)flat.z).normalize());
        gate.setTransformation(new Transformation(
            new Vector3f(),rot,new Vector3f(scale,scale,scale),new Quaternionf()));
    }
    private static void poseShot(Display.ItemDisplay shot,Vec3 dir,ItemStack item) {
        /*
         * A thrown axe/pickaxe looks like a boomerang if its 2D sprite is
         * aligned only by shortest-arc rotationTo(axis,dir): that leaves
         * the screen-facing roll unconstrained.
         *
         * Align the handle->head axis to local -Z once; then apply a stable
         * world-up projectile frame. The display gets NO per-tick angular
         * rotation. All axes, pickaxes, hoes, shovels and swords travel
         * straight with their business end forward, not spinning sideways.
         */
        String id=item.getDescriptionId().toLowerCase(java.util.Locale.ROOT);
        boolean axe=item.getItem() instanceof AxeItem || id.contains("_axe");
        boolean pick=item.getItem() instanceof PickaxeItem || id.contains("_pickaxe");
        boolean shovel=item.getItem() instanceof ShovelItem || id.contains("_shovel");
        boolean hoe=item.getItem() instanceof HoeItem || id.contains("_hoe");
        boolean tool=axe || pick || shovel || hoe;
        Vector3f handleToHead=new Vector3f(
            axe?-1f:1f,1f,0f).normalize();
        Quaternionf localToForward=new Quaternionf().rotationTo(
            handleToHead,new Vector3f(0f,0f,-1f));
        Vector3f forward=new Vector3f((float)dir.x,(float)dir.y,
            (float)dir.z).normalize();
        Vector3f stableUp=(Math.abs(forward.y)>0.96f)
            ? new Vector3f(0f,0f,-1f) : new Vector3f(0f,1f,0f);
        // lookAlong maps local -Z to forward and locks roll using stableUp.
        Quaternionf worldFrame=new Quaternionf().lookAlong(forward,stableUp);
        Quaternionf q=worldFrame.mul(localToForward);
        float scale=tool?1.12f:1.25f;
        shot.setTransformation(new Transformation(
            new Vector3f(),q,new Vector3f(scale,scale,scale),new Quaternionf()));
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
        for(int i=0;i<a.gateCount;i++) {
            if(now<a.gateOpeningAt[i]) continue;
            Vec3 center=gatePos(a,p,i,forward,side);
            Display.ItemDisplay gate=a.gates[i];
            // Spawn each gate at its own reveal time, not all at once.
            if(gate==null || gate.isRemoved()) {
                gate=new Display.ItemDisplay(EntityType.ITEM_DISPLAY,a.level);
                gate.setItemStack(new ItemStack(GilgameshMod.GATE_FRAME.get()));
                gate.setItemTransform(ItemDisplayContext.FIXED);
                gate.setNoGravity(true);
                gate.setPos(center.x,center.y,center.z);
                a.level.addFreshEntity(gate);
                a.gates[i]=gate;
            }
            float opening=Math.min(1f,Math.max(0.08f,
                (now-a.gateOpeningAt[i])/(float)OPEN_TICKS));
            gate.setPos(center.x,center.y,center.z);
            Vec3 orientation=a.surround?a.target.subtract(center).normalize():forward;
            poseGate(gate,orientation,2.6f*opening);
            if(now%3==0) drawPortal(a.level,center,side,opening);
        }
        Iterator<Shot> shots=a.shots.iterator();
        while(shots.hasNext()){
            Shot s=shots.next();
            s.ticks++;
            Vec3 previous=s.display.position();
            Vec3 desired=previous.add(s.direction.scale(SHOT_SPEED));
            BlockHitResult block=a.level.clip(new ClipContext(
                previous,desired,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
            boolean blocked=block.getType()!=HitResult.Type.MISS;
            Vec3 endpoint=blocked?block.getLocation():desired;
            // A broad-phase AABB and narrow-phase segment intersection prevent
            // tunneling through entities at 2.7 blocks/tick.
            AABB area=new AABB(previous,endpoint).inflate(0.55);
            LivingEntity closest=null;
            double closestDistance=Double.POSITIVE_INFINITY;
            for(LivingEntity candidate:a.level.getEntitiesOfClass(LivingEntity.class,area)) {
                if(!candidate.isAlive() || candidate.getUUID().equals(s.owner)
                    || p.isAlliedTo(candidate)) continue;
                var intersection=candidate.getBoundingBox().inflate(0.35).clip(previous,endpoint);
                if(intersection.isEmpty())continue;
                double distance=previous.distanceToSqr(intersection.get());
                if(distance<closestDistance) {
                    closestDistance=distance;
                    closest=candidate;
                }
            }
            if(closest!=null) {
                closest.hurt(p.damageSources().playerAttack(p),s.damage);
            }
            s.display.setPos(endpoint.x,endpoint.y,endpoint.z);
            if(s.ticks%2==0) {
                Vec3 trail=endpoint.subtract(s.direction.scale(0.6));
                a.level.sendParticles(ParticleTypes.END_ROD,
                    trail.x,trail.y,trail.z,1,0.03,0.03,0.03,0.003);
            }
            if(closest!=null || blocked || s.ticks>=SHOT_LIFE || s.display.isRemoved()) {
                if(!s.display.isRemoved()) s.display.discard();
                shots.remove();
            }
        }
        if(now>=a.nextFire) {
            fireWave(a,p,forward,side);
            // Only the first activation animates the portals opening.
            // Reuse these same open gate entities for every subsequent wave.
            a.nextFire=now+NEXT_WAVE_DELAY;
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
    /** Attack damage comes from THIS stored ItemStack's main-hand stats.
     * Includes flat, base-multiplier and total-multiplier item attributes,
     * plus the vanilla Sharpness bonus. Never uses a fixed sword damage cap.
     */
    private static float weaponDamage(ServerPlayer player,ItemStack stack) {
        double flat=1.0; // The default vanilla player attack attribute.
        double baseMultiplier=0.0;
        double totalMultiplier=1.0;
        ItemAttributeModifiers mods=stack.getOrDefault(
            DataComponents.ATTRIBUTE_MODIFIERS,ItemAttributeModifiers.EMPTY);
        for(ItemAttributeModifiers.Entry entry:mods.modifiers()) {
            if(!entry.attribute().equals(Attributes.ATTACK_DAMAGE)
                || !entry.slot().test(EquipmentSlot.MAINHAND)) continue;
            double amount=entry.modifier().amount();
            switch(entry.modifier().operation()) {
                case ADD_VALUE -> flat+=amount;
                case ADD_MULTIPLIED_BASE -> baseMultiplier+=amount;
                case ADD_MULTIPLIED_TOTAL -> totalMultiplier*=1.0+amount;
            }
        }
        double damage=Math.max(0.0,flat*(1.0+baseMultiplier)*totalMultiplier);
        // Sharpness is attached to the stored sword, not the caster's hand.
        var sharpness=player.registryAccess().registryOrThrow(Registries.ENCHANTMENT)
            .getHolderOrThrow(Enchantments.SHARPNESS);
        int level=EnchantmentHelper.getItemEnchantmentLevel(sharpness,stack);
        if(level>0) damage+=0.5*level+0.5;
        if(!Double.isFinite(damage)) return 1.0f;
        return (float)Math.max(1.0,Math.min(1_000_000.0,damage));
    }

    private static void fireWave(Active a,ServerPlayer p,Vec3 forward,Vec3 side) {
        Container store=WeaponVault.get(p);
        List<ItemStack> weapons=new ArrayList<>();
        for(int slot=0;slot<store.getContainerSize();slot++) {
            ItemStack stack=store.getItem(slot);
            if(WeaponVault.isWeapon(stack)) weapons.add(stack.copyWithCount(1));
        }
        if(weapons.isEmpty()) {a.wave=0;return;}
        List<Integer> readyGates=new ArrayList<>();
        long now=a.level.getGameTime();
        for(int i=0;i<a.gateCount;i++)
            if(a.gates[i]!=null && !a.gates[i].isRemoved()
                && now>=a.gateOpeningAt[i]+OPEN_TICKS) readyGates.add(i);
        if(readyGates.isEmpty()) return;
        int waves=(weapons.size()+WAVE-1)/WAVE;
        a.wave%=waves;
        int start=a.wave*WAVE;
        int limit=Math.min(start+WAVE,weapons.size());
        // Aim near the crosshair, not AT a single identical point. Each gate
        // has its own lane and each subsequent shot fans out across the target
        // plane. This creates visible spread while maintaining useful aiming.
        Vec3 aimPoint=a.surround?a.target:GilgameshAbilities.selectSurroundTarget(p);
        for(int k=start;k<limit;k++) {
            int index=k-start;
            int gate=readyGates.get(index%readyGates.size());
            Vec3 center=gatePos(a,p,gate,forward,side);
            int lane=index/readyGates.size();
            double[] fan=FAN_OFFSETS[lane%FAN_OFFSETS.length];
            // Keep the launch inside the opening while fanning out in flight.
            Vec3 spawn=center.add(side.scale(fan[0]*0.22))
                .add(0,fan[1]*0.18,0);
            Display.ItemDisplay d=new Display.ItemDisplay(EntityType.ITEM_DISPLAY,a.level);
            d.setItemStack(weapons.get(k).copy());
            d.setItemTransform(ItemDisplayContext.FIXED);
            d.setNoGravity(true);
            d.setPos(spawn.x,spawn.y,spawn.z);
            Vec3 spreadTarget;
            if(a.surround) {
                // Surround gates continue inward, but strike different parts
                // of the target area instead of collapsing onto a point.
                Vec3 radial=center.subtract(a.target);
                Vec3 flatRadial=new Vec3(radial.x,0,radial.z).normalize();
                spreadTarget=a.target.add(flatRadial.scale(1.2))
                    .add(side.scale(fan[0]*0.65))
                    .add(0,fan[1]*0.50,0);
            } else {
                int[] cell=GRID_ORDER[gate];
                spreadTarget=aimPoint.add(side.scale(
                    cell[0]*1.65+fan[0]*FAN_HORIZONTAL))
                    .add(0,cell[1]*1.20+fan[1]*FAN_VERTICAL,0);
            }
            Vec3 shotAim=spreadTarget.subtract(spawn).normalize();
            poseShot(d,shotAim,weapons.get(k));
            a.level.addFreshEntity(d);
            a.shots.add(new Shot(d,shotAim,p,weaponDamage(p,weapons.get(k))));
        }
        // All the entities above are created in one server tick (no per-item delays).
        a.level.sendParticles(ParticleTypes.FLASH,p.getX(),p.getY()+2,p.getZ(),1,0,0,0,0);
        a.wave=(a.wave+1)%waves;
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("gilgamesh")
            .then(Commands.literal("grant")
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    boolean learned = SkillAPI.getSkillsFrom(player).learnSkill(GilgameshMod.SKILL_ID);
                    player.sendSystemMessage(Component.literal(
                        learned ? "Gilgamesh Ultimate Skill learned" : "Unable to learn Gilgamesh skill"));
                    return learned ? 1 : 0;
                }))
            .then(Commands.literal("obtain").executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                var skills = SkillAPI.getSkillsFrom(player);
                if(skills.getSkill(GilgameshMod.SKILL_ID).isPresent()) {
                    player.sendSystemMessage(Component.literal("You already possess Gilgamesh"));
                    return 0;
                }
                if(!player.isCreative() &&
                   EnergyHelper.isOutOfMagiculeConsuming(player,750_000D,0.0D)>0.0D) {
                    player.sendSystemMessage(Component.literal(
                        "Gilgamesh requires 750,000 MP to obtain"));
                    return 0;
                }
                boolean learned=skills.learnSkill(GilgameshMod.SKILL_ID);
                player.sendSystemMessage(Component.literal(
                    learned ? "Gilgamesh learned (Ultimate Skill)" :
                        "Unable to learn Gilgamesh; check your skill capacity"));
                return learned?1:0;
            }))
            .then(Commands.literal("count").executes(ctx->{
                ServerPlayer p=ctx.getSource().getPlayerOrException();
                int count=0;
                Container store=WeaponVault.get(p);
                for(int i=0;i<store.getContainerSize();i++) if(WeaponVault.isWeapon(store.getItem(i))) count++;
                p.sendSystemMessage(Component.literal("Golden Gates: "+count+" weapons, "+
                    gatesForStorehouse(p)+" portals, "+
                    ((count+WAVE-1)/WAVE)+" waves (max "+WAVE+" per wave)"));
                return 1;
            })));
    }
}
