/*
 * Copyright © Contributing Authors
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

package grondag.jmx.json.model;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Either;

import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;

import grondag.jmx.json.ext.JsonBlockModelExt;

public class JsonUnbakedModelHelper {
	private JsonUnbakedModelHelper() { }

	public static final ItemModelGenerator ITEM_MODEL_GENERATOR = new ItemModelGenerator();

	public static BlockModel remap(BlockModel template, ImmutableMap<ResourceLocation, ResourceLocation> textureMap) {
		final JsonBlockModelExt ext = (JsonBlockModelExt) template;

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
