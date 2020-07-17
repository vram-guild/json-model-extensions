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

import java.util.Locale;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.util.JsonHelper;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.util.TriState;

import javax.annotation.Nullable;

@Environment(EnvType.CLIENT)
public class JmxMaterial {
	private static class Properties {
		public static final Properties DEFAULT = new Properties();

		public final TriState diffuse;
		public final TriState ao;
		public final TriState emissive;
		public final TriState colorIndex;
		public final int color;
		public final BlendMode layer;

		private Properties() {
			diffuse = TriState.DEFAULT;
			ao = TriState.DEFAULT;
			emissive = TriState.DEFAULT;
			colorIndex = TriState.DEFAULT;
			color = 0xFFFFFFFF;
			layer = null;
		}

		public Properties(TriState diffuse, TriState ao, TriState emissive, TriState colorIndex, int color, BlendMode layer) {
			this.diffuse = diffuse;
			this.ao = ao;
			this.emissive = emissive;
			this.colorIndex = colorIndex;
			this.color = color;
			this.layer = layer;
		}
	}

	public static final JmxMaterial DEFAULT = new JmxMaterial();

	public final String id;
	public final String preset;

	private final Properties[] properties;

	public final int tag;

	private JmxMaterial() {
		id = "DEFAULT";
		preset = null;
		properties = new Properties[] {Properties.DEFAULT };
		tag = 0;
	}

	public JmxMaterial(String id, JsonObject jsonObject) {
		this.id = id;
		preset = JsonHelper.getString(jsonObject, "preset", null);
		tag = JsonHelper.getInt(jsonObject, "tag", 0);

		JsonArray properties = JsonHelper.getArray(jsonObject, "layers", null);

		if (properties == null) {
			this.properties = null;
		} else {
			int depth = properties.size();

			this.properties = new Properties[depth];

			for (int i = 0; i < depth; i++) {
				JsonObject propertyObj = properties.get(i).getAsJsonObject();
				this.properties[i] = new Properties(
						asTriState(JsonHelper.getString(propertyObj, "diffuse", null)),
						asTriState(JsonHelper.getString(propertyObj, "ambient_occlusion", null)),
						asTriState(JsonHelper.getString(propertyObj, "emissive", null)),
						asTriState(JsonHelper.getString(propertyObj, "colorIndex", null)),
						color(JsonHelper.getString(propertyObj, "color", "0xFFFFFFFF")),
						asLayer(JsonHelper.getString(propertyObj, "layer", null))
				);
			}
		}
	}

	public int getDepth() {
		if (properties == null) {
			return 0;
		}
		return this.properties.length;
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
		if (properties == null || spriteIndex >= properties.length) {
			return TriState.DEFAULT;
		}
		return properties[spriteIndex].diffuse;
	}

	public TriState getAo(int spriteIndex) {
		if (properties == null || spriteIndex >= properties.length) {
			return TriState.DEFAULT;
		}
		return properties[spriteIndex].ao;
	}

	public TriState getEmissive(int spriteIndex) {
		if (properties == null || spriteIndex >= properties.length) {
			return TriState.DEFAULT;
		}
		return properties[spriteIndex].emissive;
	}

	public TriState getColorIndex(int spriteIndex) {
		if (properties == null || spriteIndex >= properties.length) {
			return TriState.DEFAULT;
		}
		return properties[spriteIndex].colorIndex;
	}

	public int getColor(int spriteIndex) {
		if (properties == null || spriteIndex >= properties.length) {
			return 0xFFFFFFFF;
		}
		return properties[spriteIndex].color;
	}

	@Nullable
	public BlendMode getLayer(int spriteIndex) {
		if (properties == null || spriteIndex >= properties.length) {
			return null;
		}
		return properties[spriteIndex].layer;
	}
}
