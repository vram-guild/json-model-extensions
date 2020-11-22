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

package grondag.jmx.json.ext;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import com.google.gson.*;
import com.mojang.datafixers.util.Either;
import grondag.frex.api.material.MaterialLoader;
import grondag.jmx.JsonModelExtensions;
import grondag.jmx.target.FrexHolder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

public class JmxModelExt {
    private static final Renderer RENDERER = RendererAccess.INSTANCE.getRenderer();
    private static final RenderMaterial STANDARD_MATERIAL = RENDERER.materialById(RenderMaterial.MATERIAL_STANDARD);

	public static final ThreadLocal<JmxModelExt> TRANSFER  = new ThreadLocal<>();

	private static final boolean FREX_RENDERER = FrexHolder.target().isFrexRendererAvailable();

	public JmxModelExt parent;

	private final Map<String, Either<String, Identifier>> materialMap;
	private final Map<String, Either<String, Integer>> colorMap;
	private final Map<String, Either<String, Integer>> tagMap;
	@Nullable
	private final Identifier quadTransformId;


	private JmxModelExt(Map<String, Either<String, Identifier>> materialMap, Map<String, Either<String, Integer>> colorMap, Map<String, Either<String, Integer>> tagMap, @Nullable Identifier quadTransformId) {
		this.materialMap = materialMap;
        this.colorMap = colorMap;
        this.tagMap = tagMap;
        this.quadTransformId = quadTransformId;
	}

	public boolean isEmpty() {
		return (parent == null || parent.isEmpty()) && materialMap.isEmpty() && getQuadTransformId() == null;
	}

	@Nullable
	public Identifier getQuadTransformId() {
		return quadTransformId == null && parent != null ? parent.getQuadTransformId() : quadTransformId;
	}

    public int resolveColor(String name) {
	    return this.resolve(
	        name,
            ext -> ext.colorMap,
            i -> i,
            0xFFFFFFFF,
            "color"
        );
    }

    public int resolveTag(String name) {
	    return this.resolve(
	        name,
            ext -> ext.tagMap,
            i -> i,
            0,
            "tag"
        );
    }

    public RenderMaterial resolveMaterial(String name) {
        return this.resolve(
            name,
            ext -> ext.materialMap,
            MaterialLoader::loadMaterial,
            STANDARD_MATERIAL,
            "material"
        );
    }

    private <T, S> T resolve(String name, Function<JmxModelExt, Map<String, Either<String, S>>> getMap, Function<S, T> loader, T _default, String type) {
	    return resolveInner(name, getMap, loader, _default, type, new ResolutionContext(this)).right().orElse(_default);
    }

    private <T, S> Either<String, T> resolveInner(String name, Function<JmxModelExt, Map<String, Either<String, S>>> getMap, Function<S, T> loader, T _default, String type, ResolutionContext context) {
        if (context.current == this) {
            JsonModelExtensions.LOG.warn("Unable to resolve {}: {}", type, name);
            return Either.right(_default);
        }

        final Map<String, Either<String, S>> map = getMap.apply(this);
        @Nullable Either<String, S> found = map.get(name.substring(1));

        if (found == null) {
            if (parent != null) {
                return parent.resolveInner(name, getMap, loader, _default, type, context);
            } else {
                JsonModelExtensions.LOG.warn("Unable to resolve {} due to missing definition: {}", type, name.substring(1));
                return Either.right(_default);
            }
        }

        return found.map(
            (String nextReference) -> {
                context.current = this;
                return context.root.resolveInner(nextReference, getMap, loader, _default, type, context);
            },
            (S storedValue) -> Either.right(loader.apply(storedValue))
        );
    }

	public static void deserialize(JsonObject jsonObjIn) {
		if(FREX_RENDERER && jsonObjIn.has("frex")) {
			deserializeInner(jsonObjIn.getAsJsonObject("frex"));
		} else if(jsonObjIn.has("jmx")) {
			deserializeInner(jsonObjIn.getAsJsonObject("jmx"));
		} else {
			TRANSFER.set(new JmxModelExt(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), null));
		}
	}

	public static final class ResolutionContext {
		public final JmxModelExt root;
		public JmxModelExt current;

		private ResolutionContext(JmxModelExt root) {
			this.root = root;
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

	private static void deserializeInner(JsonObject obj) {
	    final Map<String, Either<String, Identifier>> materials = deserializeLayers(obj, "materials", el -> new Identifier(el.getAsString()), RenderMaterial.MATERIAL_STANDARD);
	    final Map<String, Either<String, Integer>> tags = deserializeLayers(obj, "tags", JsonElement::getAsInt, 0);
	    final Map<String, Either<String, Integer>> colors = deserializeLayers(obj, "colors", JmxModelExt::parseColor, 0xFFFFFFFF);

		final String idString = JsonHelper.getString(obj, "quad_transform", null);
		final Identifier quadTransformId;
		if (idString != null) {
			quadTransformId = Identifier.tryParse(idString);
		} else {
			quadTransformId = null;
		}

		TRANSFER.set(new JmxModelExt(materials, colors, tags, quadTransformId));
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
