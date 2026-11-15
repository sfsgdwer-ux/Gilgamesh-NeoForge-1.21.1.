package com.gilgamesh.tensura;

import software.bernie.geckolib.renderer.GeoItemRenderer;

/** Render Ea as animated geometry instead of a static sprite item. */
public final class EaGeoRenderer extends GeoItemRenderer<EaAnimatedItem> {
    public EaGeoRenderer() {
        super(new EaGeoModel());
    }
}
