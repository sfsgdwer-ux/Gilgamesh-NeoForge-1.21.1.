package com.gilgamesh.tensura;

import com.mojang.math.Transformation;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Original 3D Enkidu chain-link visuals (not copied from the old TRMysticism).
 * Links are ordinary server-side ItemDisplay entities, compatible with
 * Minecraft 1.21.1 / NeoForge and vanilla clients with this mod.
 */
@EventBusSubscriber(modid = GilgameshMod.MODID)
public final class EnkiduChains {
    private static final int LINKS_PER_STRAND = 18;
    private static final int STRANDS = 2;
    private static final int TOTAL_LINKS = LINKS_PER_STRAND * STRANDS;
    private static final int UPDATE_EVERY_TICKS = 3;
    private static final DustParticleOptions ENKIDU_GLOW =
        new DustParticleOptions(new Vector3f(1f, 0.75f, 0.13f), 1.1f);

    private static final Map<UUID, Visual> ACTIVE = new HashMap<>();

    private static final class Visual {
        final ServerLevel level;
        final UUID victim;
        final Display.ItemDisplay[] links = new Display.ItemDisplay[TOTAL_LINKS];
        final long expiry;

        Visual(ServerLevel level, UUID victim, long expiry) {
            this.level = level;
            this.victim = victim;
            this.expiry = expiry;
        }
    }

    private EnkiduChains() {}

    /** Summoned on successful Enkidu binding. */
    public static void summon(ServerPlayer caster, LivingEntity victim, long expiry) {
        remove(victim.getUUID());
        ServerLevel level = caster.serverLevel();
        Visual visual = new Visual(level, victim.getUUID(), expiry);
        Vec3 at = victim.position();
        for(int i=0; i<TOTAL_LINKS; i++) {
            Display.ItemDisplay link = new Display.ItemDisplay(EntityType.ITEM_DISPLAY, level);
            link.setItemStack(new ItemStack(GilgameshMod.ENKIDU_CHAIN_LINK.get()));
            link.setItemTransform(ItemDisplayContext.FIXED);
            link.setNoGravity(true);
            link.setPos(at.x, at.y + 1.0, at.z);
            level.addFreshEntity(link);
            visual.links[i] = link;
        }
        ACTIVE.put(victim.getUUID(), visual);
        reposition(visual, victim, 0);

        // Summoning flash and trail leading from the caster to the target.
        Vec3 start = caster.getEyePosition();
        Vec3 end = victim.getBoundingBox().getCenter();
        for(int k=1; k<=16; k++) {
            Vec3 p = start.lerp(end, k / 16.0);
            level.sendParticles(ENKIDU_GLOW, p.x, p.y, p.z,
                1, 0.055, 0.055, 0.055, 0.0);
        }
        level.sendParticles(ParticleTypes.ENCHANT,
            end.x, end.y, end.z, 24, 0.4, 0.45, 0.4, 0.07);
    }

    public static void remove(UUID target) {
        Visual existing = ACTIVE.remove(target);
        if(existing != null) clear(existing);
    }

    private static void clear(Visual visual) {
        for(Display.ItemDisplay link : visual.links)
            if(link != null && !link.isRemoved()) link.discard();
    }

    private static void reposition(Visual visual, LivingEntity victim, long time) {
        Vec3 center = victim.position();
        double height = Math.max(1.25, Math.min(3.7, victim.getBbHeight()));
        double radius = Math.min(0.82, Math.max(0.40, victim.getBbWidth()*0.52+0.24));
        double sway = time * 0.024;
        for(int strand=0; strand<STRANDS; strand++) {
            for(int i=0; i<LINKS_PER_STRAND; i++) {
                double t = (i + 0.5) / LINKS_PER_STRAND;
                double phase = Math.PI * strand + 4 * Math.PI * t + sway;
                double y = center.y + height * (0.10 + 0.83 * t);
                Vec3 at = new Vec3(center.x + radius*Math.cos(phase),
                    y, center.z + radius*Math.sin(phase));
                // Tangent of the helix, so every individual link follows the chain.
                Vector3f tangent = new Vector3f(
                    (float)(-radius*Math.sin(phase)*4*Math.PI),
                    (float)(height*0.83),
                    (float)(radius*Math.cos(phase)*4*Math.PI)
                ).normalize();
                Quaternionf orientation = new Quaternionf().rotationTo(
                    new Vector3f(0,1,0), tangent);
                float scale = (float)Math.min(0.72, Math.max(0.50,height/2.6));
                Display.ItemDisplay link = visual.links[strand*LINKS_PER_STRAND + i];
                if(link == null || link.isRemoved()) continue;
                link.setPos(at.x,at.y,at.z);
                link.setTransformation(new Transformation(
                    new Vector3f(),orientation,
                    new Vector3f(scale,scale,scale),new Quaternionf()));
            }
        }
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        if(ACTIVE.isEmpty()) return;
        Iterator<Map.Entry<UUID,Visual>> iterator = ACTIVE.entrySet().iterator();
        while(iterator.hasNext()) {
            Visual visual = iterator.next().getValue();
            Entity entity = visual.level.getEntity(visual.victim);
            if(visual.level.getGameTime() >= visual.expiry
                || !(entity instanceof LivingEntity victim)
                || !victim.isAlive()
                || !GilgameshAbilities.isEnkiduBound(victim)) {
                clear(visual);
                iterator.remove();
                continue;
            }
            if(visual.level.getGameTime()%UPDATE_EVERY_TICKS == 0)
                reposition(visual, victim, visual.level.getGameTime());
        }
    }
}
