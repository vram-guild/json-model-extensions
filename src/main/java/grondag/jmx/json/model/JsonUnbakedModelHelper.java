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

package grondag.jmx.json.model;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Either;

import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;

import grondag.jmx.json.ext.JsonUnbakedModelExt;

public class JsonUnbakedModelHelper {
	private JsonUnbakedModelHelper() { }

	public static final ItemModelGenerator ITEM_MODEL_GENERATOR = new ItemModelGenerator();

	public static BlockModel remap(BlockModel template, ImmutableMap<ResourceLocation, ResourceLocation> textureMap) {
		final JsonUnbakedModelExt ext = (JsonUnbakedModelExt) template;

		return new BlockModel(
				ext.jmx_parentId(),
				template.getElements(),
				remapTextureMap(ext.jmx_textureMap(), textureMap),
				template.hasAmbientOcclusion(),
				template.getGuiLight(),
				template.getTransforms(),
				template.getOverrides());
	}

	public static Map<String, Either<Material, String>> remapTextureMap(Map<String, Either<Material, String>> mapIn, Map<ResourceLocation, ResourceLocation> textureMap) {
		final Map<String, Either<Material, String>> result = new HashMap<>();

		for (final Map.Entry<String, Either<Material, String>> entry : mapIn.entrySet()) {
			if (entry.getValue().left().isPresent()) {
				final Material oldId = entry.getValue().left().get();

				if (oldId != null) {
					final ResourceLocation remapId = textureMap.get(oldId.texture());

					if (remapId != null) {
						result.put(entry.getKey(), Either.left(new Material(oldId.atlasLocation(), remapId)));
						continue;
					}
				}
			}

			result.put(entry.getKey(), entry.getValue());
		}

		return result;
	}
}
