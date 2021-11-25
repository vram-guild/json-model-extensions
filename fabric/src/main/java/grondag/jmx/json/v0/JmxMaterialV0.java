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

package grondag.jmx.json.v0;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.GsonHelper;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.util.TriState;

@Environment(EnvType.CLIENT)
public class JmxMaterialV0 {
	private static class LayerData {
		@SuppressWarnings("hiding")
		public static final LayerData DEFAULT = new LayerData();

		public final TriState diffuse;
		public final TriState ao;
		public final TriState emissive;
		public final TriState colorIndex;
		public final int color;
		public final BlendMode layer;

		private LayerData() {
			diffuse = TriState.DEFAULT;
			ao = TriState.DEFAULT;
			emissive = TriState.DEFAULT;
			colorIndex = TriState.DEFAULT;
			color = 0xFFFFFFFF;
			layer = null;
		}

		private LayerData(TriState diffuse, TriState ao, TriState emissive, TriState colorIndex, int color, BlendMode layer) {
			this.diffuse = diffuse;
			this.ao = ao;
			this.emissive = emissive;
			this.colorIndex = colorIndex;
			this.color = color;
			this.layer = layer;
		}
	}

	public static final JmxMaterialV0 DEFAULT = new JmxMaterialV0();

	public final String id;
	public final String preset;

	private final LayerData[] layers;

	public final int tag;

	private JmxMaterialV0() {
		id = "DEFAULT";
		preset = null;
		layers = new LayerData[] {LayerData.DEFAULT };
		tag = 0;
	}

	public JmxMaterialV0(String id, JsonObject jsonObject) {
		this.id = id;
		preset = GsonHelper.getAsString(jsonObject, "preset", null);
		tag = GsonHelper.getAsInt(jsonObject, "tag", 0);

		final JsonArray layers = GsonHelper.getAsJsonArray(jsonObject, "layers", null);

		if (layers == null) {
			int depth = -1;
			final int[] propertyIndices = new int[jsonObject.entrySet().size()];
			Arrays.fill(propertyIndices, -1);

			int entryIndex = 0;

			for (final Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
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

			if (GsonHelper.isValidPrimitive(jsonObject, "depth") && depth > GsonHelper.getAsInt(jsonObject, "depth")) {
				throw new IllegalStateException("Model defines a depth of " + GsonHelper.getAsInt(jsonObject, "depth") + ", but uses a depth of " + depth + ".");
			}

			if (depth != -1) {
				this.layers = new LayerData[depth];

				for (int i = 0; i < depth; i++) {
					this.layers[i] = new LayerData(
							asTriState(GsonHelper.getAsString(jsonObject, "diffuse" + i, null)),
							asTriState(GsonHelper.getAsString(jsonObject, "ambient_occlusion" + i, null)),
							asTriState(GsonHelper.getAsString(jsonObject, "emissive" + i, null)),
							asTriState(GsonHelper.getAsString(jsonObject, "colorIndex" + i, null)),
							color(GsonHelper.getAsString(jsonObject, "color" + i, "0xFFFFFFFF")),
							asLayer(GsonHelper.getAsString(jsonObject, "layer" + i, null))
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
				this.layers[i] = new LayerData(
						asTriState(GsonHelper.getAsString(propertyObj, "diffuse", null)),
						asTriState(GsonHelper.getAsString(propertyObj, "ambient_occlusion", null)),
						asTriState(GsonHelper.getAsString(propertyObj, "emissive", null)),
						asTriState(GsonHelper.getAsString(propertyObj, "colorIndex", null)),
						color(GsonHelper.getAsString(propertyObj, "color", "0xFFFFFFFF")),
						asLayer(GsonHelper.getAsString(propertyObj, "layer", null))
						);
			}
		}
	}

	public int getDepth() {
		if (layers == null) {
			return 0;
		}

		return layers.length;
	}

	private static int color(String str) {
		return str.startsWith("0x") ? Integer.parseUnsignedInt(str.substring(2), 16) : Integer.parseInt(str);
	}

	private static BlendMode asLayer(String property) {
		if (property == null || property.isEmpty()) {
			return null;
		} else {
			switch (property.toLowerCase(Locale.ROOT)) {
				case "solid":
					return BlendMode.SOLID;
				case "cutout":
					return BlendMode.CUTOUT;
				case "cutout_mipped":
					return BlendMode.CUTOUT_MIPPED;
				case "translucent":
					return BlendMode.TRANSLUCENT;
				default:
					return null;
			}
		}
	}

	private static TriState asTriState(String property) {
		if (property == null || property.isEmpty()) {
			return TriState.DEFAULT;
		} else {
			switch (property.toLowerCase(Locale.ROOT)) {
				case "true":
				case "yes":
				case "1":
				case "y":
					return TriState.TRUE;
				case "false":
				case "no":
				case "0":
				case "n":
					return TriState.FALSE;
				default:
					return TriState.DEFAULT;
			}
		}
	}

	public TriState getDiffuse(int spriteIndex) {
		if (layers == null || spriteIndex >= layers.length) {
			return TriState.DEFAULT;
		}

		return layers[spriteIndex].diffuse;
	}

	public TriState getAo(int spriteIndex) {
		if (layers == null || spriteIndex >= layers.length) {
			return TriState.DEFAULT;
		}

		return layers[spriteIndex].ao;
	}

	public TriState getEmissive(int spriteIndex) {
		if (layers == null || spriteIndex >= layers.length) {
			return TriState.DEFAULT;
		}

		return layers[spriteIndex].emissive;
	}

	public TriState getColorIndex(int spriteIndex) {
		if (layers == null || spriteIndex >= layers.length) {
			return TriState.DEFAULT;
		}

		return layers[spriteIndex].colorIndex;
	}

	public int getColor(int spriteIndex) {
		if (layers == null || spriteIndex >= layers.length) {
			return 0xFFFFFFFF;
		}

		return layers[spriteIndex].color;
	}

	@Nullable
	public BlendMode getLayer(int spriteIndex) {
		if (layers == null || spriteIndex >= layers.length) {
			return null;
		}

		return layers[spriteIndex].layer;
	}
}
