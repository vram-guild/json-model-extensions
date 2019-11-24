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

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Either;

import grondag.jmx.json.ext.JsonUnbakedModelExt;
import net.minecraft.client.render.SpriteIdentifier;
import net.minecraft.client.render.model.json.ItemModelGenerator;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.util.Identifier;

public class JsonUnbakedModelHelper {
	private JsonUnbakedModelHelper() {}

	public static final ItemModelGenerator ITEM_MODEL_GENERATOR = new ItemModelGenerator();

	public static JsonUnbakedModel remap(JsonUnbakedModel template, ImmutableMap<Identifier, Identifier> textureMap) {
		final JsonUnbakedModelExt ext = (JsonUnbakedModelExt)template;

		return new JsonUnbakedModel(
				ext.jmx_parentId(),
				template.getElements(),
				remapTextureMap(ext.jmx_textureMap(), textureMap),
				template.useAmbientOcclusion(),
				template.hasDepthInGui(),
				template.getTransformations(),
				template.getOverrides());
	}

	public static Map<String, Either<SpriteIdentifier, String>> remapTextureMap(Map<String, Either<SpriteIdentifier, String>> mapIn, Map<Identifier, Identifier> textureMap) {
		final Map<String, Either<SpriteIdentifier, String>> result = new HashMap<>();

		for(final Map.Entry<String, Either<SpriteIdentifier, String>> entry : mapIn.entrySet()) {

			if(entry.getValue().left().isPresent()) {
				final SpriteIdentifier oldId  = entry.getValue().left().get();

				if(oldId != null) {
					final Identifier remapId = textureMap.get(oldId.textureId());

					if (remapId != null) {
						result.put(entry.getKey(), Either.left(new SpriteIdentifier(oldId.atlasId(), remapId)));
						continue;
					}
				}
			}

			result.put(entry.getKey(), entry.getValue());
		}

		return result;
	}
}
