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

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.model.json.ModelElementTexture;
import net.minecraft.util.JsonHelper;

@Environment(EnvType.CLIENT)
public class FaceExtData {
	public static final ThreadLocal<FaceExtData> TRANSFER  = new ThreadLocal<>();

	public static final FaceExtData EMPTY = new FaceExtData();

	private FaceExtData() {
		jmx_tex0 = null;
		jmx_tex1 = null;
		jmx_material = null;
		jmx_texData0 = null;
		jmx_texData1 = null;
	}

	private FaceExtData(JsonObject jsonObj, JsonDeserializationContext context) {
		jmx_tex0 = JsonHelper.getString(jsonObj, "jmx_tex0", null);
		jmx_tex1 = JsonHelper.getString(jsonObj, "jmx_tex1", null);
		jmx_material = JsonHelper.getString(jsonObj, "jmx_material", null);
		jmx_texData0 = deserializeJmxTexData(jsonObj, context, "jmx_uv_rot0");
		jmx_texData1 = deserializeJmxTexData(jsonObj, context, "jmx_uv_rot1");
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

	public final String jmx_tex0;
	public final String jmx_tex1;
	public final String jmx_material;
	public final ModelElementTexture jmx_texData0;
	public final ModelElementTexture jmx_texData1;

	public boolean isEmpty() {
		return (jmx_tex0 == null || jmx_tex0.isEmpty())
				&& (jmx_tex1 == null || jmx_tex1.isEmpty())
				&& (jmx_material == null || jmx_material.isEmpty())
				&& jmx_texData0 == null
				&& jmx_texData1 == null;
	}
}
