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

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import grondag.jmx.json.FaceExtData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.client.render.model.json.ModelElementTexture;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.util.JsonHelper;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public class FaceExtDataV1 extends FaceExtData {
	public static class LayerData {
		public static final LayerData EMPTY = new LayerData();

		public final String texture;
		public final String material;
		public final String tag;
		public final String color;
		public final ModelElementTexture texData;

		private LayerData() {
			texture = null;
			material = null;
			tag = null;
			color = null;
			texData = null;
		}

		public LayerData(String texture, String material, String tag, String color, ModelElementTexture texData) {
			this.texture = texture;
            this.material = material;
            this.tag = tag;
            this.color = color;
            this.texData = texData;
		}
	}

	public static final FaceExtDataV1 EMPTY = new FaceExtDataV1();

	private FaceExtDataV1() {
		layers = new LayerData[] {LayerData.EMPTY };
	}

	private FaceExtDataV1(JsonObject jsonObj, JsonDeserializationContext context) {
		final JsonArray layers = JsonHelper.getArray(jsonObj, "jmx_layers", null);

		if (layers == null) {
			int depth = -1;
			final int[] propertyIndices = new int[jsonObj.entrySet().size()];
			Arrays.fill(propertyIndices, -1);

			int entryIndex = 0;
			for (final Map.Entry<String, JsonElement> entry : jsonObj.entrySet()) {
				if ("preset".equals(entry.getKey()) || "tag".equals(entry.getKey())) {
					continue;
				}

				for (int i = 0; i < entry.getKey().length(); i++) {
					if (Character.isDigit(entry.getKey().charAt(i))) {
						propertyIndices[entryIndex] = Integer.parseInt(entry.getKey().substring(i));

						if (propertyIndices[entryIndex] + 1 > depth) {
							depth = propertyIndices[entryIndex] + 1;
						}

						break;
					}
				}

				entryIndex++;
			}

			if (JsonHelper.hasPrimitive(jsonObj, "depth") && depth > JsonHelper.getInt(jsonObj, "depth")) {
				throw new IllegalStateException("Model defines a depth of " + JsonHelper.getInt(jsonObj, "depth") + ", but uses a depth of " + depth + ".");
			}

			if (depth != -1) {
				this.layers = new LayerData[depth];

				for (int i = 0; i < depth; i++) {
					this.layers[i] = new LayerData(
						JsonHelper.getString(jsonObj, "jmx_tex" + i, null),
                            null,
                        null, null, deserializeJmxTexData(jsonObj, context, "jmx_uv_rot" + i)
					);
				}
			} else {
				this.layers = null;
			}
		} else {
			final int depth = layers.size();

			this.layers = new LayerData[depth];

			for (int i = 0; i < depth; i++) {
				final JsonObject propertyObj = layers.get(i).getAsJsonObject();
				@Nullable String texture = JsonHelper.getString(propertyObj, "texture", null);
				if (texture != null) {
					texture += i;
				}
				@Nullable String material = JsonHelper.getString(propertyObj, "material", null);
				if (material != null) {
				    material += i;
                }
				@Nullable String tag = JsonHelper.getString(propertyObj, "tag", null);
				if (tag != null) {
				    tag += i;
                }
				@Nullable String color = JsonHelper.getString(propertyObj, "color", null);
				if (color != null) {
				    color += i;
                }
				this.layers[i] = new LayerData(
					texture,
                    material,
                    tag,
                    color,
                    deserializeJmxTexData(propertyObj, context, "uv_rot")
				);
			}
		}
	}

	public static FaceExtDataV1 deserializeV1(JsonObject jsonObj, JsonDeserializationContext context) {
		return new FaceExtDataV1(jsonObj, context);
	}

	private static @Nullable ModelElementTexture deserializeJmxTexData(JsonObject jsonObj, JsonDeserializationContext context, String tag) {
		if(jsonObj.has(tag)) {
			final JsonObject texObj = JsonHelper.getObject(jsonObj, tag);

			if(!texObj.isJsonNull()) {
				return (ModelElementTexture)context.deserialize(jsonObj, ModelElementTexture.class);
			}
		}

		return null;
	}

	private final LayerData[] layers;

	public boolean isEmpty() {
		if (layers == null) {
			return true;
		}

		for (final LayerData layerData : layers) {
			if (layerData.texture != null && !layerData.texture.isEmpty() && layerData.texData != null) {
				return false;
			}
		}

		return true;
	}

	public int getDepth() {
		if (layers == null) {
			return 0;
		}
		return layers.length;
	}

	public LayerData getLayer(int i) {
	    if (layers == null || i >= layers.length) {
	        return null;
        }
	    return layers[i];
    }

    @Override
    public void getTextureDependencies(JsonUnbakedModel model, Supplier<HashSet<Pair<String, String>>> errors, Supplier<HashSet<SpriteIdentifier>> deps) {
        for (int i = 0; i < getDepth(); i++) {
            final String texStr = getLayer(i).texture;
            if (texStr != null && !texStr.isEmpty()) {
                final SpriteIdentifier tex = model.resolveSprite(texStr);

                if (Objects.equals(tex.getTextureId(), MissingSprite.getMissingSpriteId())) {
                    errors.get().add(Pair.of(texStr, model.id));
                } else {
                    deps.get().add(tex);
                }
            }
        }
    }
}
