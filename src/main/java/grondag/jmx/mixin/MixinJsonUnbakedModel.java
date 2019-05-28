package grondag.jmx.mixin;

import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.google.common.collect.Sets;

import grondag.jmx.JsonModelExtensions;
import grondag.jmx.json.ext.FaceExtData;
import grondag.jmx.json.ext.JmxExtension;
import grondag.jmx.json.ext.JmxModelExt;
import grondag.jmx.json.ext.JsonUnbakedModelExt;
import grondag.jmx.json.model.BasicBakedModel;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.ModelBakeSettings;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.client.render.model.json.ModelElement;
import net.minecraft.client.render.model.json.ModelElementFace;
import net.minecraft.client.render.model.json.ModelItemPropertyOverrideList;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

@Mixin(JsonUnbakedModel.class)
public abstract class MixinJsonUnbakedModel implements JsonUnbakedModelExt {
    @Shadow
    protected abstract ModelItemPropertyOverrideList compileOverrides(ModelLoader modelLoader, JsonUnbakedModel jsonUnbakedModel);

    @Shadow protected Identifier parentId;
    
    private JsonUnbakedModelExt jmxParent;
    private JmxModelExt jmxModelExt;

    @Override
    public JmxModelExt jmx_modelExt() {
        return jmxModelExt;
    }

    @Override
    public JsonUnbakedModelExt jmx_parent() {
        return jmxParent;
    }

    @Override
    public Identifier jmx_parentId() {
        return parentId;
    }

    @Override
    public void jmx_parent(JsonUnbakedModelExt parent) {
        jmxParent = parent;
        if(jmxModelExt != null) {
            jmxModelExt.parent = parent.jmx_modelExt();
        }
    }

    @Inject(at = @At("RETURN"), method = "<init>") 
    private void onInit(CallbackInfo ci) {
        jmxModelExt = JmxModelExt.TRANSFER.get();
    }

    /**
     * We don't need the collection of material dependencies - this is just to map parent relationships.
     */
    @SuppressWarnings("unlikely-arg-type")
    @Inject(at = @At("RETURN"), method = "getTextureDependencies") 
    private void onGetTextureDependencies(Function<Identifier, UnbakedModel> modelFunc, Set<String> errors, CallbackInfoReturnable<Identifier> ci) {
        Set<JsonUnbakedModelExt> set = Sets.newLinkedHashSet();

        for(JsonUnbakedModelExt model = (JsonUnbakedModelExt)(Object)this; model.jmx_parentId() != null && model.jmx_parent() == null; model = model.jmx_parent()) {
            set.add(model);
            UnbakedModel parentModel = (UnbakedModel)modelFunc.apply(model.jmx_parentId());
            if (parentModel == null) {
                JsonModelExtensions.LOG.warn("No parent '{}' while loading model '{}'", parentId, model);
            }

            if (set.contains(parentModel)) {
                JsonModelExtensions.LOG.warn("Found 'parent' loop while loading model '{}' in chain: {} -> {}", model, set.stream().map(Object::toString).collect(Collectors.joining(" -> ")), parentId);
                parentModel = null;
            }

            if (parentModel != null && !(parentModel instanceof JsonUnbakedModel)) {
                throw new IllegalStateException("BlockModel parent has to be a block model.");
            }

            model.jmx_parent((JsonUnbakedModelExt)parentModel);
        }
    }

    @SuppressWarnings("unchecked")
    @Inject(at = @At("HEAD"), method = "Lnet/minecraft/client/render/model/json/JsonUnbakedModel;bake(Lnet/minecraft/client/render/model/ModelLoader;Lnet/minecraft/client/render/model/json/JsonUnbakedModel;Ljava/util/function/Function;Lnet/minecraft/client/render/model/ModelBakeSettings;)Lnet/minecraft/client/render/model/BakedModel;", cancellable = true)
    public void onBake(ModelLoader modelLoader, JsonUnbakedModel unbakedModel, Function<Identifier, Sprite> spriteFunc, ModelBakeSettings bakeProps, CallbackInfoReturnable<BakedModel> ci) {
        final JsonUnbakedModel me = (JsonUnbakedModel)(Object)this;
        if (me.getRootModel() == ModelLoader.BLOCK_ENTITY_MARKER) {
            // leave vanilla logic for built-ins
            return;
        } else {
            Function<String, Sprite> spriteFuncInner = s -> spriteFunc.apply(new Identifier(me.resolveTexture(s)));
            Sprite particleSprite = spriteFuncInner.apply("particle");
            BasicBakedModel.Builder builder = (new BasicBakedModel.Builder(me, compileOverrides(modelLoader, unbakedModel))).setParticle(particleSprite);
            Iterator<ModelElement> elements = me.getElements().iterator();
            while(elements.hasNext()) {
                ModelElement element = elements.next();
                Iterator<Direction> faces = element.faces.keySet().iterator();

                while(faces.hasNext()) {
                    Direction face = faces.next();
                    ModelElementFace elementFace = element.faces.get(face);
                    FaceExtData extData = ((JmxExtension<FaceExtData>)elementFace).jmx_ext();

                    String tex0 = extData.jmx_tex0 == null ? elementFace.textureId : extData.jmx_tex0;

                    Sprite sprite = spriteFuncInner.apply(tex0);
                    if (elementFace.cullFace == null) {
                        builder.addQuad(null, jmxModelExt, spriteFuncInner, element, elementFace, sprite, face, bakeProps);
                    } else {
                        builder.addQuad(bakeProps.getRotation().apply(elementFace.cullFace), jmxModelExt, spriteFuncInner, element, elementFace, sprite, face, bakeProps);
                    }
                }
            }

            ci.setReturnValue(builder.build());
        }
    }
}