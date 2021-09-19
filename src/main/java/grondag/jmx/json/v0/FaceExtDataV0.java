/*
 *  Copyright 2019, 2020 grondag
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License.  You may obtain a copy
 *  of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

package grondag.jmx.json.v0;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.util.GsonHelper;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import grondag.jmx.json.FaceExtData;

@Environment(EnvType.CLIENT)
public class FaceExtDataV0 extends FaceExtData {
	private static class LayerData {
		public static final LayerData EMPTY = new LayerData();

		public final String tex;
		public final BlockFaceUV texData;

		private LayerData() {
			tex = null;
			texData = null;
		}

		private LayerData(String tex, BlockFaceUV texData) {
			this.tex = tex;
			this.texData = texData;
		}
	}

	public static final FaceExtDataV0 EMPTY = new FaceExtDataV0();

	private FaceExtDataV0() {
		jmx_material = null;
		layers = new LayerData[] {LayerData.EMPTY };
	}

	private FaceExtDataV0(JsonObject jsonObj, JsonDeserializationContext context) {
		jmx_material = GsonHelper.getAsString(jsonObj, "jmx_material", null);

		final JsonArray layers = GsonHelper.getAsJsonArray(jsonObj, "layered_textures", null);

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

			if (GsonHelper.isValidPrimitive(jsonObj, "depth") && depth > GsonHelper.getAsInt(jsonObj, "depth")) {
				throw new IllegalStateException("Model defines a depth of " + GsonHelper.getAsInt(jsonObj, "depth") + ", but uses a depth of " + depth + ".");
			}

			if (depth != -1) {
				this.layers = new LayerData[depth];

				for (int i = 0; i < depth; i++) {
					this.layers[i] = new LayerData(
							GsonHelper.getAsString(jsonObj, "jmx_tex" + i, null),
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
				@Nullable String tex = GsonHelper.getAsString(propertyObj, "jmx_tex", null);

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

	public static FaceExtDataV0 deserializeV0(JsonObject jsonObj, JsonDeserializationContext context) {
		return new FaceExtDataV0(jsonObj, context);
	}

	private static @Nullable BlockFaceUV deserializeJmxTexData(JsonObject jsonObj, JsonDeserializationContext context, String tag) {
		if (jsonObj.has(tag)) {
			final JsonObject texObj = GsonHelper.getAsJsonObject(jsonObj, tag);

			if (!texObj.isJsonNull()) {
				return context.deserialize(jsonObj, BlockFaceUV.class);
			}
		}

		return null;
	}

	public final String jmx_material;

	private final LayerData[] layers;

	@Override
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

	@Override
	public void getTextureDependencies(BlockModel model, Supplier<HashSet<Pair<String, String>>> errors, Supplier<HashSet<Material>> deps) {
		for (int spriteIndex = 0; spriteIndex < getDepth(); spriteIndex++) {
			final String texStr = getTex(spriteIndex);

			if (texStr != null && !texStr.isEmpty()) {
				final Material tex = model.getMaterial(texStr);

				if (Objects.equals(tex.texture(), MissingTextureAtlasSprite.getLocation())) {
					errors.get().add(Pair.of(texStr, model.name));
				} else {
					deps.get().add(tex);
				}
			}
		}
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
	public BlockFaceUV getTexData(int spriteIndex) {
		if (layers == null || spriteIndex >= layers.length) {
			return null;
		}

		return layers[spriteIndex].texData;
	}

	public BlockFaceUV getTexData(int spriteIndex, BlockFaceUV backup) {
		final BlockFaceUV texData = getTexData(spriteIndex);
		return texData == null ? backup : texData;
	}
}
