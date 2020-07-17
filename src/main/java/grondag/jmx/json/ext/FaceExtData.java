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

import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.model.json.ModelElementTexture;
import net.minecraft.util.JsonHelper;

@Environment(EnvType.CLIENT)
public class FaceExtData {
	private static class Properties {
		public static final Properties EMPTY = new Properties();

		public final String tex;
		public final ModelElementTexture texData;

		private Properties() {
			tex = null;
			texData = null;
		}

		public Properties(String tex, ModelElementTexture texData) {
			this.tex = tex;
			this.texData = texData;
		}
	}

	public static final ThreadLocal<FaceExtData> TRANSFER  = new ThreadLocal<>();

	public static final FaceExtData EMPTY = new FaceExtData();

	private FaceExtData() {
		jmx_material = null;
		properties = new Properties[] { Properties.EMPTY };
	}

	private FaceExtData(JsonObject jsonObj, JsonDeserializationContext context) {
		jmx_material = JsonHelper.getString(jsonObj, "jmx_material", null);

		JsonArray properties = JsonHelper.getArray(jsonObj, "layered_textures", null);

		if (properties == null) {
			this.properties = null;
		} else {
			int depth = properties.size();

			this.properties = new Properties[depth];

			for (int i = 0; i < depth; i++) {
				JsonObject propertyObj = properties.get(i).getAsJsonObject();
				@Nullable String tex = JsonHelper.getString(propertyObj, "jmx_tex", null);
				if (tex != null) {
					tex += i;
				}
				this.properties[i] = new Properties(
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

	private final Properties[] properties;

	public boolean isEmpty() {
		if (jmx_material != null && !jmx_material.isEmpty()) {
			return false;
		}

		if (properties == null) {
			return true;
		}

		for (Properties properties : this.properties) {
			if (properties.tex != null && !properties.tex.isEmpty() && properties.texData != null) {
				return false;
			}
		}

		return true;
	}

	public int getDepth() {
		if (this.properties == null) {
			return 0;
		}
		return this.properties.length;
	}

	@Nullable
	public String getTex(int spriteIndex) {
		if (properties == null || spriteIndex >= properties.length) {
			return null;
		}
		return properties[spriteIndex].tex;
	}

	@Nullable
	public ModelElementTexture getTexData(int spriteIndex) {
		if (properties == null || spriteIndex >= properties.length) {
			return null;
		}
		return properties[spriteIndex].texData;
	}

	public ModelElementTexture getTexData(int spriteIndex, ModelElementTexture backup) {
		ModelElementTexture texData = getTexData(spriteIndex);
		return texData == null ? backup : texData;
	}
}
