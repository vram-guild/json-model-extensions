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

package grondag.jmx.impl;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;

import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;

import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.model.SpriteFinder;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext.QuadTransform;

public class RetexturedModelTransformer implements ModelTransformer, TransformableModelContext {
	public final ResourceLocation targetModel;
	public final ResourceLocation sourceModel;

	public final ImmutableMap<ResourceLocation, ResourceLocation> textureMap;

	private Object2ObjectOpenHashMap<BlockState, BlockState> inverseStateMap = null;

	private RetexturedModelTransformer(ResourceLocation sourceModel, ResourceLocation targetModel, ImmutableMap<ResourceLocation, ResourceLocation> textureMap) {
		this.sourceModel = sourceModel;
		this.targetModel = targetModel;
		this.textureMap = textureMap;
	}

	@Override
	public Collection<ResourceLocation> textures() {
		return textureMap.values();
	}

	@Override
	public BakedModel transform(BakedModel model) {
		return model instanceof TransformableModel
			? ((TransformableModel) model).derive(this)
			: Minecraft.getInstance().getModelManager().getMissingModel();
	}

	public static Builder builder(ResourceLocation sourceModel, ResourceLocation targetModel) {
		return new Builder(sourceModel, targetModel);
	}

	public static class Builder {
		private final ImmutableMap.Builder<ResourceLocation, ResourceLocation> builder = ImmutableMap.builder();

		final ResourceLocation sourceModel;
		final ResourceLocation targetModel;

		private Builder(ResourceLocation sourceModel, ResourceLocation targetModel) {
			this.sourceModel = sourceModel;
			this.targetModel = targetModel;
		}

		public Builder mapSprite(ResourceLocation from, ResourceLocation to) {
			builder.put(from, to);
			return this;
		}

		public Builder mapSprite(String from, String to) {
			builder.put(new ResourceLocation(from), new ResourceLocation(to));
			return this;
		}

		public RetexturedModelTransformer build() {
			return new RetexturedModelTransformer(sourceModel, targetModel, builder.build());
		}
	}

	private static void remapSprite(MutableQuadView q, TextureAtlasSprite oldSprite, TextureAtlasSprite newSprite, int spriteIndex) {
		for (int i = 0; i < 4; i++) {
			final float u = q.spriteU(i, spriteIndex);
			final float v = q.spriteV(i, spriteIndex);

			final float uSpan = oldSprite.getU1() - oldSprite.getU0();
			final float x = (u - oldSprite.getU0()) / uSpan * 16.0F;

			final float vSpan = oldSprite.getV1() - oldSprite.getV0();
			final float y = (v - oldSprite.getV0()) / vSpan * 16.0F;

			q.sprite(i, spriteIndex, newSprite.getU(x), newSprite.getV(y));
		}
	}

	public boolean transform(MutableQuadView quad) {
		final TextureAtlas atlas = Minecraft.getInstance().getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS);
		final SpriteFinder sf = SpriteFinder.get(atlas);
		final TextureAtlasSprite oldSprite = sf.find(quad, 0);
		final TextureAtlasSprite newSprite = spriteTransform().mapSprite(oldSprite, atlas);
		remapSprite(quad, oldSprite, newSprite, 0);
		return true;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Object2ObjectOpenHashMap<BlockState, BlockState> inverseStateMapImpl() {
		Object2ObjectOpenHashMap<BlockState, BlockState> result = inverseStateMap;

		if (result == null) {
			final Object2ObjectOpenHashMap<BlockState, BlockState> newMap = new Object2ObjectOpenHashMap<>();
			final BlockState defaultState = Registry.BLOCK.get(targetModel).defaultBlockState();

			final StateDefinition<Block, BlockState> factory = Registry.BLOCK.get(sourceModel).getStateDefinition();
			factory.getPossibleStates().forEach(s -> {
				BlockState targetState = defaultState;
				final ImmutableMap<Property<?>, Comparable<?>> props = s.getValues();

				if (!props.isEmpty()) {
					final Iterator<Entry<Property<?>, Comparable<?>>> it = props.entrySet().iterator();

					while (it.hasNext()) {
						final Entry<Property<?>, Comparable<?>> prop = it.next();
						final Property p = prop.getKey();
						final Comparable v = prop.getValue();
						targetState = targetState.setValue(p, v);
					}
				}

				newMap.put(targetState, s);
			});

			result = newMap;
			inverseStateMap = newMap;
		}

		return result;
	}

	@Override
	public QuadTransform quadTransform() {
		return this::transform;
	}

	@Override
	public InverseStateMap inverseStateMap() {
		return inverseStateMapImpl()::get;
	}

	@Override
	public SpriteMap spriteTransform() {
		return textureMap::get;
	}
}
