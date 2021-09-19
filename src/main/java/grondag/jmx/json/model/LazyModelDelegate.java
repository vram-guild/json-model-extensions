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
