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

package grondag.jmx.json.model;

import java.util.HashMap;
import java.util.Map;

import grondag.jmx.json.ext.JsonUnbakedModelExt;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.util.Identifier;

public class JsonUnbakedModelHelper {
	private JsonUnbakedModelHelper() {}
	
	public static JsonUnbakedModel remap(JsonUnbakedModel template, Map<Identifier, Identifier> textureMap) {
		JsonUnbakedModelExt ext = (JsonUnbakedModelExt)template;
		
		return new JsonUnbakedModel(
				ext.jmx_parentId(),
				template.getElements(),
				remapTextureMap(ext.jmx_textureMap(), textureMap),
				template.useAmbientOcclusion(),
				template.hasDepthInGui(),
				template.getTransformations(),
				template.getOverrides());
	}
	
	public static Map<String, String> remapTextureMap(Map<String, String> mapIn, Map<Identifier, Identifier> textureMap) {
		HashMap<String, String> result = new HashMap<>();
		
		for(Map.Entry<String, String> entry : mapIn.entrySet()) {
			Identifier newId = textureMap.get(new Identifier(entry.getValue()));
			result.put(entry.getKey(), newId == null ? entry.getValue() : newId.toString());
		}
		return result;
	}
}
