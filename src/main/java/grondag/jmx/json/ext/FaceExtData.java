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
	private static class LayerData {
		public static final LayerData EMPTY = new LayerData();

		public final String tex;
		public final ModelElementTexture texData;

		private LayerData() {
			tex = null;
			texData = null;
		}

		public LayerData(String tex, ModelElementTexture texData) {
			this.tex = tex;
			this.texData = texData;
		}
	}

	public static final ThreadLocal<FaceExtData> TRANSFER  = new ThreadLocal<>();

	public static final FaceExtData EMPTY = new FaceExtData();

	private FaceExtData() {
		jmx_material = null;
		layers = new LayerData[] {LayerData.EMPTY };
	}

	private FaceExtData(JsonObject jsonObj, JsonDeserializationContext context) {
		jmx_material = JsonHelper.getString(jsonObj, "jmx_material", null);

		final JsonArray layers = JsonHelper.getArray(jsonObj, "layered_textures", null);

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
						deserializeJmxTexData(jsonObj, context, "jmx_uv_rot" + i)
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
				@Nullable String tex = JsonHelper.getString(propertyObj, "jmx_tex", null);
				if (tex != null) {
					tex += i;
				}
				this.layers[i] = new LayerData(
					tex,
					deserializeJmxTexData(propertyObj, context, "jmx_uv_rot")
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

	public final String jmx_material;

	private final LayerData[] layers;

	public boolean isEmpty() {
		if (jmx_material != null && !jmx_material.isEmpty()) {
			return false;
		}

		if (layers == null) {
			return true;
		}

		for (final LayerData layerData : layers) {
			if (layerData.tex != null && !layerData.tex.isEmpty() && layerData.texData != null) {
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

	@Nullable
	public String getTex(int spriteIndex) {
		if (layers == null || spriteIndex >= layers.length) {
			return null;
		}
		return layers[spriteIndex].tex;
	}

	@Nullable
	public ModelElementTexture getTexData(int spriteIndex) {
		if (layers == null || spriteIndex >= layers.length) {
			return null;
		}
		return layers[spriteIndex].texData;
	}

	public ModelElementTexture getTexData(int spriteIndex, ModelElementTexture backup) {
		final ModelElementTexture texData = getTexData(spriteIndex);
		return texData == null ? backup : texData;
	}
}
