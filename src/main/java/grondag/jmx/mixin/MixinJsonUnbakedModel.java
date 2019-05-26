package grondag.jmx.mixin;

import java.util.Iterator;
import java.util.function.Function;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import grondag.jmx.json.model.BasicBakedModel;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.ModelBakeSettings;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.client.render.model.json.ModelElement;
import net.minecraft.client.render.model.json.ModelElementFace;
import net.minecraft.client.render.model.json.ModelItemPropertyOverrideList;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

@Mixin(JsonUnbakedModel.class)
public abstract class MixinJsonUnbakedModel {
    @Shadow
    protected abstract ModelItemPropertyOverrideList compileOverrides(ModelLoader modelLoader, JsonUnbakedModel jsonUnbakedModel);

    @Inject(at = @At("HEAD"), method = "Lnet/minecraft/client/render/model/json/JsonUnbakedModel;bake(Lnet/minecraft/client/render/model/ModelLoader;Lnet/minecraft/client/render/model/json/JsonUnbakedModel;Ljava/util/function/Function;Lnet/minecraft/client/render/model/ModelBakeSettings;)Lnet/minecraft/client/render/model/BakedModel;", cancellable = true)
    public void onBake(ModelLoader modelLoader, JsonUnbakedModel unbakedModel, Function<Identifier, Sprite> spriteFunc, ModelBakeSettings bakeProps, CallbackInfoReturnable<BakedModel> ci) {
        final JsonUnbakedModel me = (JsonUnbakedModel)(Object)this;
        if (me.getRootModel() == ModelLoader.BLOCK_ENTITY_MARKER) {
            // leave vanilla logic for built-ins
            return;
        } else {
            Sprite particleSprite = (Sprite)spriteFunc.apply(new Identifier(me.resolveTexture("particle")));
            BasicBakedModel.Builder builder = (new BasicBakedModel.Builder(me, compileOverrides(modelLoader, unbakedModel))).setParticle(particleSprite);
            Iterator<ModelElement> elements = me.getElements().iterator();

            while(elements.hasNext()) {
                ModelElement element = elements.next();
                Iterator<Direction> faces = element.faces.keySet().iterator();

                while(faces.hasNext()) {
                    Direction face = faces.next();
                    ModelElementFace elementFace = element.faces.get(face);
                    Sprite sprite = spriteFunc.apply(new Identifier(me.resolveTexture(elementFace.textureId)));
                    if (elementFace.cullFace == null) {
                        builder.addQuad(null, element, elementFace, sprite, face, bakeProps);
                    } else {
                        builder.addQuad(bakeProps.getRotation().apply(elementFace.cullFace), element, elementFace, sprite, face, bakeProps);
                    }
                }
            }

            ci.setReturnValue(builder.build());
        }
    }
}