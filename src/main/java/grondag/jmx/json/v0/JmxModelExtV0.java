/*******************************************************************************
 * Copyright 2019 grondag
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/

package grondag.jmx.json.v0;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import grondag.jmx.JsonModelExtensions;
import grondag.jmx.json.JmxModelExt;
import grondag.jmx.json.ext.JmxExtension;
import grondag.jmx.json.model.JmxBakedModel;
import grondag.jmx.target.FrexHolder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.MaterialFinder;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.ModelBakeSettings;
import net.minecraft.client.render.model.json.*;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

public class JmxModelExtV0 extends JmxModelExt<JmxModelExtV0> {
    private static final boolean FREX_RENDERER = FrexHolder.target().isFrexRendererAvailable();

    private final Map<String, Object> materialMap;
    @Nullable
    private final Identifier quadTransformId;

    private JmxModelExtV0(Map<String, Object> materialMap, @Nullable Identifier quadTransformId) {
        this.materialMap = materialMap;
        this.quadTransformId = quadTransformId;
    }

    public static JmxModelExtV0 deserializeV0(JsonObject obj) {
        if(FREX_RENDERER && obj.has("frex")) {
            return deserializeInner(obj.getAsJsonObject("frex"));
        } else if(obj.has("jmx")) {
            return deserializeInner(obj.getAsJsonObject("jmx"));
        } else {
            return new JmxModelExtV0(Collections.emptyMap(), null);
        }
    }

    public boolean isEmpty() {
        return materialMap.isEmpty() && getQuadTransformId() == null;
    }

    @Nullable
    private Identifier getQuadTransformId() {
        return quadTransformId == null && parent != null ? parent.getQuadTransformId() : quadTransformId;
    }

    public JmxMaterial resolveMaterial(String matName) {
        return matName == null || materialMap == null ? JmxMaterial.DEFAULT : resolveMaterialInner(matName);
    }

    private JmxMaterial resolveMaterialInner(String matName) {
        if (!isMaterialReference(matName)) {
            matName = '#' + matName;
        }

        final Object result = resolveMaterial(matName, new MaterialResolutionContext(this));
        return result instanceof JmxMaterial ? (JmxMaterial) result : JmxMaterial.DEFAULT;
    }

    private Object resolveMaterial(Object val, MaterialResolutionContext context) {
        if (isMaterialReference(val)) {
            if (this == context.current) {
                JsonModelExtensions.LOG.warn("Unable to resolve material due to upward reference: {}", val);
                return JmxMaterial.DEFAULT;
            } else {
                Object result = materialMap.get(((String)val).substring(1));
                if(result instanceof JmxMaterial) {
                    return result;
                }

                if (result == null && parent != null) {
                    result = parent.resolveMaterial(val, context);
                }

                if (isMaterialReference(result)) {
                    context.current = this;
                    result = context.root.resolveMaterial(result, context);
                }

                return result;
            }
        } else {
            return val;
        }
    }

    public static boolean isMaterialReference(Object val) {
        return val instanceof String && ((String)val).charAt(0) == '#';
    }

    public static final class MaterialResolutionContext {
        public final JmxModelExtV0 root;
        public JmxModelExtV0 current;

        private MaterialResolutionContext(JmxModelExtV0 root) {
            this.root = root;
        }
    }

    private static JmxModelExtV0 deserializeInner(JsonObject jsonObj) {
        final Object2ObjectOpenHashMap<String, Object> map = new Object2ObjectOpenHashMap<>();
        if (jsonObj.has("materials")) {
            final JsonObject job = jsonObj.getAsJsonObject("materials");
            for (final Entry<String, JsonElement> e : job.entrySet()) {
                if (e.getValue().isJsonObject()) {
                    map.put(e.getKey(), new JmxMaterial(e.getKey(), e.getValue().getAsJsonObject()));
                } else {
                    map.put(e.getKey(), e.getValue().getAsString());
                }
            }
        }

        final String idString = JsonHelper.getString(jsonObj, "quad_transform", null);
        final Identifier quadTransformId;
        if (idString != null) {
            quadTransformId = Identifier.tryParse(idString);
        } else {
            quadTransformId = null;
        }

        return new JmxModelExtV0(map, quadTransformId);
    }

    @Override
    public BakedModel buildModel(ModelOverrideList modelOverrideList, boolean hasDepth, Sprite particleSprite, ModelBakeSettings bakeProps, Identifier modelId, JsonUnbakedModel me, Function<SpriteIdentifier, Sprite> textureGetter) {
        final Function<String, Sprite> innerSpriteFunc = s -> textureGetter.apply(me.resolveSprite(s));

        final JmxBakedModel.Builder builder = (new JmxBakedModel.Builder(me, modelOverrideList, hasDepth, getQuadTransformId()))
            .setParticle(particleSprite);

        final MaterialFinder finder = RendererAccess.INSTANCE.getRenderer().materialFinder();

        for (ModelElement element : me.getElements()) {

            for (Direction face : element.faces.keySet()) {
                final ModelElementFace elementFace = element.faces.get(face);
                //noinspection unchecked
                final FaceExtDataV0 extData = ((JmxExtension<FaceExtDataV0>) elementFace).jmx_ext();

                final String extTex = extData.getTex(0);
                final String tex = extTex == null ? elementFace.textureId : extTex;

                Sprite sprite = textureGetter.apply(me.resolveSprite(tex));

                final Direction cullFace = elementFace.cullFace == null ? null : Direction.transform(bakeProps.getRotation().getMatrix(), elementFace.cullFace);

                addQuad(bakeProps, modelId, innerSpriteFunc, builder, finder, element, face, elementFace, extData, sprite, cullFace);
            }
        }

        return builder.build();
    }

    private void addQuad(ModelBakeSettings bakeProps, Identifier modelId, Function<String, Sprite> innerSpriteFunc, JmxBakedModel.Builder builder, MaterialFinder finder, ModelElement element, Direction face, ModelElementFace elementFace, FaceExtDataV0 extData, Sprite sprite, Direction cullFace) {
        final JmxMaterial jmxMat = resolveMaterial(extData.jmx_material);

        final QuadEmitter emitter = builder.emitter;

        final int depth = Math.max(extData.getDepth(), jmxMat.getDepth());

        for (int spriteIndex = 0; spriteIndex < depth; spriteIndex++) {
            if (spriteIndex != 0) {
                sprite = getSprite(spriteIndex, extData, innerSpriteFunc);

                if (sprite == null) {
                    continue; // don't add quads with no sprite
                }
            }

            emitter.material(loadMaterial(finder, jmxMat, element, builder.usesAo, spriteIndex));
            emitter.cullFace(cullFace);

            if(jmxMat.tag != 0) {
                emitter.tag(jmxMat.tag);
            }

            final ModelElementTexture texData = extData.getTexData(spriteIndex, elementFace.textureData);

            QUADFACTORY_EXT.jmx_bake(emitter, 0, element, elementFace, texData, sprite, face, bakeProps, modelId);

            final int color = jmxMat.getColor(spriteIndex);
            emitter.spriteColor(0, color, color, color, color);

            emitter.colorIndex(elementFace.tintIndex);

            emitter.emit();
        }
    }

    @Nullable
    private Sprite getSprite(int spriteIndex, FaceExtDataV0 extData, Function<String, Sprite> spriteFunc) {
        final String tex = extData.getTex(spriteIndex);

        if (tex == null) {
            return null;
        }

        final Sprite sprite = spriteFunc.apply(tex);

        if (sprite.getId().equals(MissingSprite.getMissingSpriteId())) {
            return null;
        }

        return sprite;
    }

    private static RenderMaterial loadMaterial(MaterialFinder finder, JmxMaterial jmxMat, ModelElement element, boolean usesAo, int spriteIndex) {
        finder.clear();

        final TriState diffuse = jmxMat.getDiffuse(spriteIndex);
        final boolean disableDiffuse = diffuse == TriState.DEFAULT ? !element.shade : !diffuse.get();
        finder.disableDiffuse(0, disableDiffuse);

        final TriState ao = jmxMat.getAo(spriteIndex);
        final boolean disableAo = ao == TriState.DEFAULT ? !usesAo : !ao.get();
        finder.disableAo(0, disableAo);

        finder.emissive(0, jmxMat.getEmissive(spriteIndex).get());

        if (jmxMat.getColorIndex(spriteIndex) == TriState.FALSE) {
            finder.disableColorIndex(0, true);
        }

        final BlendMode layer = jmxMat.getLayer(spriteIndex);
        if (layer != null) {
            finder.blendMode(0, layer);
        }

        return finder.find();
    }
}
