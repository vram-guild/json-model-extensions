package grondag.jmx.json.model;

import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.render.model.ModelBakeSettings;
import net.minecraft.client.render.model.json.ModelElement;
import net.minecraft.client.render.model.json.ModelElementFace;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Direction;

public interface BakedQuadFactoryExt {
    void bake(QuadEmitter q, ModelElement element, ModelElementFace elementFace, Sprite sprite, Direction face, ModelBakeSettings modelBakeSettings_1);
}
