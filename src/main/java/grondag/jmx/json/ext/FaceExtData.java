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

import java.util.Arrays;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.render.model.json.ModelElementTexture;
import net.minecraft.util.JsonHelper;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class FaceExtData {
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

	public static final ThreadLocal<FaceExtData> TRANSFER  = new ThreadLocal<>();

	public static final FaceExtData EMPTY = new FaceExtData();

	private FaceExtData() {
		layers = new LayerData[] {LayerData.EMPTY };
	}

	private FaceExtData(JsonObject jsonObj, JsonDeserializationContext context) {
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

	public static void deserialize(JsonObject jsonObj, JsonDeserializationContext context) {
		TRANSFER.set(new FaceExtData(jsonObj, context));
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
}
