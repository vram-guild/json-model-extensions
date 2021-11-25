/*
 * Copyright © Original Authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
 */

package grondag.jmx.json.v1;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import com.mojang.datafixers.util.Pair;

import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.util.GsonHelper;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import grondag.jmx.json.FaceExtData;

@Environment(EnvType.CLIENT)
public class FaceExtDataV1 extends FaceExtData {
	public static class LayerData {
		@SuppressWarnings("hiding")
		public static final LayerData EMPTY = new LayerData();

		public final String texture;
		public final String material;
		public final String tag;
		public final String color;
		public final BlockFaceUV texData;

		private LayerData() {
			texture = null;
			material = null;
			tag = null;
			color = null;
			texData = null;
		}

		public LayerData(String texture, String material, String tag, String color, BlockFaceUV texData) {
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
		final JsonArray layers = GsonHelper.getAsJsonArray(jsonObj, "jmx_layers", null);

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
				@Nullable String texture = GsonHelper.getAsString(propertyObj, "texture", null);

				if (texture != null) {
					texture += i;
				}

				@Nullable String material = GsonHelper.getAsString(propertyObj, "material", null);

				if (material != null) {
					material += i;
				}

				@Nullable String tag = GsonHelper.getAsString(propertyObj, "tag", null);

				if (tag != null) {
					tag += i;
				}

				@Nullable String color = GsonHelper.getAsString(propertyObj, "color", null);

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

	private static @Nullable BlockFaceUV deserializeJmxTexData(JsonObject jsonObj, JsonDeserializationContext context, String tag) {
		if (jsonObj.has(tag)) {
			final JsonObject texObj = GsonHelper.getAsJsonObject(jsonObj, tag);

			if (!texObj.isJsonNull()) {
				return context.deserialize(jsonObj, BlockFaceUV.class);
			}
		}

		return null;
	}

	private final LayerData[] layers;

	@Override
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
	public void getTextureDependencies(BlockModel model, Supplier<HashSet<Pair<String, String>>> errors, Supplier<HashSet<Material>> deps) {
		for (int i = 0; i < getDepth(); i++) {
			final String texStr = getLayer(i).texture;

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
}
