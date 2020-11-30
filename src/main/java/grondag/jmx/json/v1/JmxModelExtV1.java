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

package grondag.jmx.json.v1;

import com.google.gson.*;
import com.mojang.datafixers.util.Either;
import grondag.frex.api.material.MaterialLoader;
import grondag.jmx.JsonModelExtensions;
import grondag.jmx.json.JmxModelExt;
import grondag.jmx.json.ext.JmxExtension;
import grondag.jmx.json.model.BakedQuadFactoryExt;
import grondag.jmx.json.model.JmxBakedModel;
import grondag.jmx.target.FrexHolder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.ModelBakeSettings;
import net.minecraft.client.render.model.json.*;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.Direction;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;

public class JmxModelExtV1 extends JmxModelExt<JmxModelExtV1> {
    private static final Renderer RENDERER = RendererAccess.INSTANCE.getRenderer();
    public static final RenderMaterial STANDARD_MATERIAL = RENDERER.materialById(RenderMaterial.MATERIAL_STANDARD);

	private static final boolean FREX_RENDERER = FrexHolder.target().isFrexRendererAvailable();

	public final Map<String, Either<String, Identifier>> materialMap;
	private final Map<String, Either<String, Integer>> colorMap;
	private final Map<String, Either<String, Integer>> tagMap;
	@Nullable
	private final Identifier quadTransformId;


	private JmxModelExtV1(Map<String, Either<String, Identifier>> materialMap, Map<String, Either<String, Integer>> colorMap, Map<String, Either<String, Integer>> tagMap, @Nullable Identifier quadTransformId) {
		this.materialMap = materialMap;
        this.colorMap = colorMap;
        this.tagMap = tagMap;
        this.quadTransformId = quadTransformId;
	}

    @Override
    public int version() {
        return 1;
    }

    @Override
    public boolean selfIsEmpty() {
        return getQuadTransformId() == null && materialMap.isEmpty() && colorMap.isEmpty() && tagMap.isEmpty();
    }

    @Override
    public BakedModel buildModel(ModelOverrideList modelOverrideList, boolean hasDepth, Sprite particleSprite, ModelBakeSettings bakeProps, Identifier modelId, JsonUnbakedModel me, Function<SpriteIdentifier, Sprite> textureGetter) {
        final JmxBakedModel.Builder builder = (new JmxBakedModel.Builder(me, modelOverrideList, hasDepth, getQuadTransformId()))
            .setParticle(particleSprite);

        for (ModelElement element : me.getElements()) {

            for (Direction face : element.faces.keySet()) {
                final ModelElementFace elementFace = element.faces.get(face);

                final Direction cullFace;
                if (elementFace.cullFace != null) {
                    cullFace = Direction.transform(bakeProps.getRotation().getMatrix(), elementFace.cullFace);
                } else {
                    cullFace = null;
                }

                emitFace(builder.emitter, QUADFACTORY_EXT, cullFace, me::resolveSprite, textureGetter, element, elementFace, face, bakeProps, modelId);
            }
        }

        return builder.build();
    }

    @Nullable
    private Identifier getQuadTransformId() {
	    if (quadTransformId != null) {
	        return quadTransformId;
        }
	    if (parent != null) {
	        return parent.getQuadTransformId();
        }
	    return null;
	}

    public int resolveColor(String name) {
	    return this.resolve(
	        name,
            ext -> ext.colorMap,
            i -> i
        ).orElseGet(() -> {
            if (shouldLogUnresolved()) {
                JsonModelExtensions.LOG.warn("Unable to resolve color {}", name);
            }
            return 0xFFFFFFFF;
        });
    }

    public int resolveTag(String name) {
	    return this.resolve(
	        name,
            ext -> ext.tagMap,
            i -> i
        ).orElseGet(() -> {
            if (shouldLogUnresolved()) {
                JsonModelExtensions.LOG.warn("Unable to resolve tag {}",  name);
            }
            return 0;
        });
    }

    public RenderMaterial resolveMaterial(String name) {
        return this.resolve(
            name,
            ext -> ext.materialMap,
            MaterialLoader::getOrLoadMaterial
        ).orElseGet(() -> {
            if (shouldLogUnresolved()) {
                JsonModelExtensions.LOG.warn("Unable to resolve material {}", name);
            }
            return STANDARD_MATERIAL;
        });
    }
    
    private boolean shouldLogUnresolved() {
	    return false;
    }

    private <T, S> Optional<T> resolve(String name, Function<JmxModelExtV1, Map<String, Either<String, S>>> getMap, Function<S, T> loader) {
	    return resolveInner(name, getMap, loader, new ResolutionContext(this)).right().flatMap(x -> x);
    }

    public <T, S> Either<String, Optional<T>> resolveInner(String name, Function<JmxModelExtV1, Map<String, Either<String, S>>> getMap, Function<S, T> loader, ResolutionContext context) {
        if (context.current == this) {
            return Either.right(Optional.empty());
        }

        final Map<String, Either<String, S>> map = getMap.apply(this);
        @Nullable Either<String, S> found = map.get(name.substring(1));

        if (found == null) {
            //noinspection ConstantConditions // vanilla parent models will technically be V0, this should be removed when V0 is removed
            if (parent instanceof JmxModelExtV1) {
                return parent.resolveInner(name, getMap, loader, context);
            } else {
                return Either.right(Optional.empty());
            }
        }

        return found.map(
            (String nextReference) -> {
                context.current = this;
                return context.root.resolveInner(nextReference, getMap, loader, context);
            },
            (S storedValue) -> Either.right(Optional.ofNullable(loader.apply(storedValue)))
        );
    }

    protected static final class ResolutionContext {
        public final JmxModelExtV1 root;
        public JmxModelExtV1 current;

        public ResolutionContext(JmxModelExtV1 root) {
            this.root = root;
        }
    }

    public void emitFace(
        QuadEmitter emitter,
        BakedQuadFactoryExt quadFactoryExt,
        Direction cullFace,
        Function<String, SpriteIdentifier> resolver,
        Function<SpriteIdentifier, Sprite> textureGetter,
        ModelElement element,
        ModelElementFace elementFace,
        Direction face,
        ModelBakeSettings bakeProps,
        Identifier modelId
    ) {
        @SuppressWarnings("unchecked")
        final FaceExtDataV1 extData = ObjectUtils.defaultIfNull(((JmxExtension<FaceExtDataV1>)elementFace).jmx_ext(), FaceExtDataV1.EMPTY);

        final int depth = Math.max(extData.getDepth(), 1);

        for (int i = 0; i < depth; i++) {
            final @Nullable FaceExtDataV1.LayerData layer = extData.getLayer(i);

            final Sprite sprite;
            if (layer != null) {
                final SpriteIdentifier spriteId = resolver.apply(layer.texture);

                // workaround for having different # of quads in "jmx" and "frex"
                // reference equals is OK because JmxTexturesExt inserts this same field
                if (spriteId == JmxTexturesExtV1.DUMMY_SPRITE) {
                    continue;
                } else if (spriteId.getTextureId() == MissingSprite.getMissingSpriteId()) {
                    continue;
                }

                sprite = textureGetter.apply(spriteId);
                if (sprite == null) {
                    continue; // don't add quads with no sprite
                }
            } else {
                sprite = textureGetter.apply(resolver.apply(elementFace.textureId));
            }

            final ModelElementTexture texData;
            if (layer == null) {
                texData = elementFace.textureData;
            } else {
                texData = ObjectUtils.defaultIfNull(layer.texData, elementFace.textureData);
            }
            quadFactoryExt.jmx_bake(emitter, 0, element, elementFace, texData, sprite, face, bakeProps, modelId);

            if (layer != null) {
                if (layer.material != null) {
                    emitter.material(resolveMaterial(layer.material));
                }
                if (layer.tag != null) {
                    emitter.tag(resolveTag(layer.tag));
                }

                if (layer.color != null) {
                    final int color = resolveColor(layer.color);
                    emitter.spriteColor(0, color, color, color, color);
                }
            }

            emitter.cullFace(cullFace);
            emitter.colorIndex(elementFace.tintIndex);

            emitter.emit();
        }
    }

    public static JmxModelExtV1 deserializeV1(JsonObject jsonObjIn) {
		if(FREX_RENDERER && jsonObjIn.has("frex")) {
			return deserializeInner(jsonObjIn.getAsJsonObject("frex"));
		} else if(jsonObjIn.has("jmx")) {
			return deserializeInner(jsonObjIn.getAsJsonObject("jmx"));
		} else {
			return new JmxModelExtV1(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), null);
		}
	}

	private static boolean isReference(JsonElement element) {
	    if (!element.isJsonPrimitive()) {
	        return false;
        }
	    final JsonPrimitive primitive = element.getAsJsonPrimitive();
	    if (!primitive.isString()) {
	        return false;
        }
	    return primitive.getAsString().charAt(0) == '#';
    }

	private static <S> Map<String, Either<String, S>> deserializeLayers(JsonObject obj, String key, Function<JsonElement, S> deserializer, S _default) {
        final Object2ObjectOpenHashMap<String, Either<String, S>> map = new Object2ObjectOpenHashMap<>();

        if (obj.has(key)) {
            final JsonArray arr = obj.getAsJsonArray(key);

            for (int i = 0, size = arr.size(); i < size; i++) {
                final JsonObject layer = arr.get(i).getAsJsonObject();

                for (final Entry<String, JsonElement> e : layer.entrySet()) {
                    if (e.getValue().isJsonNull()) {
                        map.put(e.getKey() + i, Either.right(_default));
                    } else if (isReference(e.getValue())) {
                        map.put(e.getKey() + i, Either.left(e.getValue().getAsString() + i));
                    } else {
                        map.put(e.getKey() + i, Either.right(deserializer.apply(e.getValue())));
                    }
                }
            }
        }

        return map;
    }

	private static JmxModelExtV1 deserializeInner(JsonObject obj) {
	    final Map<String, Either<String, Identifier>> materials = deserializeLayers(obj, "materials", el -> new Identifier(el.getAsString()), RenderMaterial.MATERIAL_STANDARD);
	    final Map<String, Either<String, Integer>> tags = deserializeLayers(obj, "tags", JsonElement::getAsInt, 0);
	    final Map<String, Either<String, Integer>> colors = deserializeLayers(obj, "colors", JmxModelExtV1::parseColor, 0xFFFFFFFF);

		final String idString = JsonHelper.getString(obj, "quad_transform", null);
		final Identifier quadTransformId;
		if (idString != null) {
			quadTransformId = Identifier.tryParse(idString);
		} else {
			quadTransformId = null;
		}

		return new JmxModelExtV1(materials, colors, tags, quadTransformId);
	}

    private static int parseColor(JsonElement element) {
        if (element.isJsonPrimitive()) {
            final JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isNumber()) {
                return primitive.getAsInt();
            } else if (primitive.isString()) {
                final String colorStr = primitive.getAsString();
                return colorStr.startsWith("0x") ? Integer.parseUnsignedInt(colorStr.substring(2), 16) : Integer.parseInt(colorStr);
            } else {
                throw new JsonParseException("Invalid color: " + element);
            }
        } else {
            throw new JsonParseException("Invalid color: " + element);
        }
    }
}
