package grondag.jmx.api;

import net.fabricmc.fabric.api.renderer.v1.render.RenderContext.QuadTransform;

public interface TransformableModelContext {
    SpriteTransform spriteTransform();
    QuadTransform quadTransform();
    InverseStateMap inverseStateMap();
}
