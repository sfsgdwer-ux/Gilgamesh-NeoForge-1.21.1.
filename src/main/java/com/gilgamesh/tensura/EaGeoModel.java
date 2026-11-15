package com.gilgamesh.tensura;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** GeckoLib 4 geometry/texture/animations; third-party art is not in public source. */
public final class EaGeoModel extends GeoModel<EaAnimatedItem> {
    @Override
    public ResourceLocation getModelResource(EaAnimatedItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(
            GilgameshMod.MODID, "geo/ea_sword.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(EaAnimatedItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(
            GilgameshMod.MODID, "textures/item/ea_animated.png");
    }

    @Override
    public ResourceLocation getAnimationResource(EaAnimatedItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(
            GilgameshMod.MODID, "animations/ea_sword.animation.json");
    }
}
