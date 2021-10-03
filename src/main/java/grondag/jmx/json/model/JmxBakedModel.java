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

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.MeshBuilder;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.model.ModelHelper;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext.QuadTransform;

import grondag.jmx.api.QuadTransformRegistry;
import grondag.jmx.impl.TransformableModel;
import grondag.jmx.impl.TransformableModelContext;

@Environment(EnvType.CLIENT)
public class JmxBakedModel implements BakedModel, FabricBakedModel, TransformableModel {
	protected static final Renderer RENDERER = RendererAccess.INSTANCE.getRenderer();

	protected final Mesh mesh;
	protected WeakReference<List<BakedQuad>[]> quadLists = null;
	protected final boolean usesAo;
	protected final boolean isSideLit;
	protected final TextureAtlasSprite particleSprite;
	protected final ItemTransforms transformation;
	protected final ItemOverrides itemPropertyOverrides;
	protected final boolean hasDepth;
	protected final QuadTransformRegistry.QuadTransformSource quadTransformSource;

	public JmxBakedModel(Mesh mesh, boolean usesAo, boolean isSideLit, TextureAtlasSprite particleSprite, ItemTransforms transformation, ItemOverrides itemPropertyOverrides, boolean hasDepth, QuadTransformRegistry.QuadTransformSource quadTransformSource) {
		this.mesh = mesh;
		this.usesAo = usesAo;
		this.isSideLit = isSideLit;
		this.particleSprite = particleSprite;
		this.transformation = transformation;
		this.itemPropertyOverrides = itemPropertyOverrides;
		this.hasDepth = hasDepth;
		this.quadTransformSource = quadTransformSource;
	}

	@Override
	public BakedModel derive(TransformableModelContext context) {
		final TextureAtlas atlas = Minecraft.getInstance().getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS);
		final MeshBuilder meshBuilder = RendererAccess.INSTANCE.getRenderer().meshBuilder();
		final QuadEmitter emitter = meshBuilder.getEmitter();
		final TextureAtlasSprite newParticleSprite = context.spriteTransform().mapSprite(particleSprite, atlas);
		final QuadTransform transform = context.quadTransform();

		mesh.forEach(q -> {
			emitter.material(q.material());
			q.copyTo(emitter);

			if (transform.transform(emitter)) {
				emitter.emit();
			}
		});

		return new JmxBakedModel(
			meshBuilder.build(), usesAo, isSideLit, newParticleSprite, transformation,
			transformItemProperties(context, atlas, meshBuilder), hasDepth, quadTransformSource
		);
	}

	private ItemOverrides transformItemProperties(TransformableModelContext context, TextureAtlas atlas, MeshBuilder meshBuilder) {
		//TODO: Implement
		return itemPropertyOverrides;
	}

	@Override
	public List<BakedQuad> getQuads(BlockState state, Direction face, Random rand) {
		List<BakedQuad>[] lists = quadLists == null ? null : quadLists.get();

		if (lists == null) {
			lists = toQuadLists(mesh, particleSprite);
			quadLists = new WeakReference<>(lists);
		}

		final List<BakedQuad> result = lists[face == null ? 6 : face.get3DDataValue()];
		return result == null ? ImmutableList.of() : result;
	}

	private static List<BakedQuad>[] toQuadLists(Mesh mesh, TextureAtlasSprite particleSprite) {
		try {
			return ModelHelper.toQuadLists(mesh);
		} catch (final Exception e) {
			return safeToQuadLists(mesh, particleSprite);
		}
	}

	/**
	 * Workaround for Fabric helper breaking when called before the sprite atlas is created.
	 * Triggered by AE2 when running with JSON mesh loading active.
	 *
	 * <p>Only difference is we use our particle sprite instead of looking one up.
	 */
	private static List<BakedQuad>[] safeToQuadLists(Mesh mesh, TextureAtlasSprite particleSprite) {
		@SuppressWarnings("unchecked")
		final ImmutableList.Builder<BakedQuad>[] builders = new ImmutableList.Builder[7];

		for (int i = 0; i < 7; i++) {
			builders[i] = ImmutableList.builder();
		}

		if (mesh != null) {
			mesh.forEach(q -> {
				final Direction face = q.cullFace();
				builders[face == null ? 6 : face.get3DDataValue()].add(q.toBakedQuad(0, particleSprite, false));
			});
		}

		@SuppressWarnings("unchecked")
		final List<BakedQuad>[] result = new List[7];

		for (int i = 0; i < 7; i++) {
			result[i] = builders[i].build();
		}

		return result;
	}

	@Override
	public boolean useAmbientOcclusion() {
		return usesAo;
	}

	@Override
	public boolean isGui3d() {
		return hasDepth;
	}

	@Override
	public boolean usesBlockLight() {
		return isSideLit;
	}

	@Override
	public boolean isCustomRenderer() {
		return false;
	}

	@Override
	public TextureAtlasSprite getParticleIcon() {
		return particleSprite;
	}

	@Override
	public ItemTransforms getTransforms() {
		return transformation;
	}

	@Override
	public ItemOverrides getOverrides() {
		return itemPropertyOverrides;
	}

	@Override
	public boolean isVanillaAdapter() {
		return false;
	}

	@Override
	public void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, RenderContext context) {
		if (mesh != null) {
			QuadTransform quadTransform;

			if (quadTransformSource != null) {
				quadTransform = quadTransformSource.getForBlock(blockView, state, pos, randomSupplier);
			} else {
				context.meshConsumer().accept(mesh);
				return;
			}

			if (quadTransform != null) {
				context.pushTransform(quadTransform);
				context.meshConsumer().accept(mesh);
				context.popTransform();
			} else {
				context.meshConsumer().accept(mesh);
			}
		}
	}

	@Override
	public void emitItemQuads(ItemStack stack, Supplier<Random> randomSupplier, RenderContext context) {
		if (mesh != null) {
			QuadTransform quadTransform;

			if (quadTransformSource != null) {
				quadTransform = quadTransformSource.getForItem(stack, randomSupplier);
			} else {
				context.meshConsumer().accept(mesh);
				return;
			}

			if (quadTransform != null) {
				context.pushTransform(quadTransform);
				context.meshConsumer().accept(mesh);
				context.popTransform();
			} else {
				context.meshConsumer().accept(mesh);
			}
		}
	}

	@Environment(EnvType.CLIENT)
	public static class Builder {
		private final MeshBuilder meshBuilder;
		public final QuadEmitter emitter;
		private final ItemOverrides itemPropertyOverrides;
		public final boolean usesAo;
		private TextureAtlasSprite particleTexture;
		private final boolean isSideLit;
		private final ItemTransforms transformation;
		private final boolean hasDepth;
		@Nullable
		private final ResourceLocation quadTransformId;

		public Builder(BlockModel unbakedModel, ItemOverrides itemPropertyOverrides, boolean hasDepth, @Nullable ResourceLocation quadTransformId) {
			this(unbakedModel.hasAmbientOcclusion(), unbakedModel.getGuiLight().lightLikeBlock(), unbakedModel.getTransforms(), itemPropertyOverrides, hasDepth, quadTransformId);
		}

		private Builder(boolean usesAo, boolean isSideLit, ItemTransforms transformation, ItemOverrides itemPropertyOverrides, boolean hasDepth, @Nullable ResourceLocation quadTransformId) {
			meshBuilder = RENDERER.meshBuilder();
			emitter = meshBuilder.getEmitter();
			this.itemPropertyOverrides = itemPropertyOverrides;
			this.usesAo = usesAo;
			this.isSideLit = isSideLit;
			this.transformation = transformation;
			this.hasDepth = hasDepth;
			this.quadTransformId = quadTransformId;
		}

		public JmxBakedModel.Builder setParticle(TextureAtlasSprite sprite) {
			particleTexture = sprite;
			return this;
		}

		public BakedModel build() {
			if (particleTexture == null) {
				throw new RuntimeException("Missing particle!");
			}

			final QuadTransformRegistry.QuadTransformSource quadTransformSource = QuadTransformRegistry.INSTANCE.getQuadTransform(quadTransformId);

			if (quadTransformId != null && quadTransformSource == null) {
				throw new IllegalStateException("No quad transform is registered with ID " + quadTransformId);
			}

			return new JmxBakedModel(meshBuilder.build(), usesAo, isSideLit, particleTexture, transformation, itemPropertyOverrides, hasDepth, quadTransformSource);
		}
	}
}

