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

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.mojang.datafixers.util.Pair;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;

import grondag.jmx.impl.ModelTransformer;
import grondag.jmx.impl.RetexturedModelTransformer;

public class LazyModelDelegate extends LazyForwardingModel implements UnbakedModel {
	private final ModelTransformer transformer;
	private final ModelResourceLocation templateId;

	public LazyModelDelegate(ModelResourceLocation templateId, ModelTransformer transformer) {
		this.templateId = templateId;
		this.transformer = transformer;
	}

	@Override
	public Collection<ResourceLocation> getDependencies() {
		return Collections.emptyList();
	}

	@Override
	public Collection<Material> getMaterials(Function<ResourceLocation, UnbakedModel> modelFunc, Set<Pair<String, String>> errors) {
		return transformer.textures().stream().map(id -> new Material(TextureAtlas.LOCATION_BLOCKS, id)).collect(Collectors.toList());
	}

	@Override
	public BakedModel bake(ModelBakery modelLoader, Function<Material, TextureAtlasSprite> spriteFunc, ModelState bakeProps, ResourceLocation modelId) {
		if (transformer instanceof RetexturedModelTransformer) {
			final UnbakedModel template = modelLoader.getModel(templateId);

			if (template instanceof BlockModel jsonTemplate) {
				if (((BlockModel) template).getRootModel() == ModelBakery.GENERATION_MARKER) {
					final BlockModel remapped = JsonUnbakedModelHelper.remap(jsonTemplate, ((RetexturedModelTransformer) transformer).textureMap);
					return JsonUnbakedModelHelper.ITEM_MODEL_GENERATOR.generateBlockModel(spriteFunc, remapped).bake(modelLoader, spriteFunc, bakeProps, modelId);
				}
			}
		}

		return this;
	}

	@Override
	protected BakedModel createWrapped() {
		return transformer.transform(Minecraft.getInstance().getModelManager().getModel(templateId));
	}
}
