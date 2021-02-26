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

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;

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
	protected final Sprite particleSprite;
	protected final ModelTransformation transformation;
	protected final ModelOverrideList itemPropertyOverrides;
	protected final boolean hasDepth;
	protected final QuadTransformRegistry.QuadTransformSource quadTransformSource;

	public JmxBakedModel(Mesh mesh, boolean usesAo, boolean isSideLit, Sprite particleSprite, ModelTransformation transformation, ModelOverrideList itemPropertyOverrides, boolean hasDepth, QuadTransformRegistry.QuadTransformSource quadTransformSource) {
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
		final SpriteAtlasTexture atlas = MinecraftClient.getInstance().getBakedModelManager().method_24153(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
		final MeshBuilder meshBuilder = RendererAccess.INSTANCE.getRenderer().meshBuilder();
		final QuadEmitter emitter = meshBuilder.getEmitter();
		final Sprite newParticleSprite = context.spriteTransform().mapSprite(particleSprite, atlas);
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

	private ModelOverrideList transformItemProperties(TransformableModelContext context, SpriteAtlasTexture atlas, MeshBuilder meshBuilder) {
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

		final List<BakedQuad> result = lists[face == null ? 6 : face.getId()];
		return result == null ? ImmutableList.of() : result;
	}

	private static List<BakedQuad>[] toQuadLists(Mesh mesh, Sprite particleSprite) {
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
	private static List<BakedQuad>[] safeToQuadLists(Mesh mesh, Sprite particleSprite) {
		@SuppressWarnings("unchecked")
		final ImmutableList.Builder<BakedQuad>[] builders = new ImmutableList.Builder[7];

		for (int i = 0; i < 7; i++) {
			builders[i] = ImmutableList.builder();
		}

		if (mesh != null) {
			mesh.forEach(q -> {
				final Direction face = q.cullFace();
				builders[face == null ? 6 : face.getId()].add(q.toBakedQuad(0, particleSprite, false));
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
	public boolean hasDepth() {
		return hasDepth;
	}

	@Override
	public boolean isSideLit() {
		return isSideLit;
	}

	@Override
	public boolean isBuiltin() {
		return false;
	}

	@Override
	public Sprite getSprite() {
		return particleSprite;
	}

	@Override
	public ModelTransformation getTransformation() {
		return transformation;
	}

	@Override
	public ModelOverrideList getOverrides() {
		return itemPropertyOverrides;
	}

	@Override
	public boolean isVanillaAdapter() {
		return false;
	}

	@Override
	public void emitBlockQuads(BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, RenderContext context) {
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
		private final ModelOverrideList itemPropertyOverrides;
		public final boolean usesAo;
		private Sprite particleTexture;
		private final boolean isSideLit;
		private final ModelTransformation transformation;
		private final boolean hasDepth;
		@Nullable
		private final Identifier quadTransformId;

		public Builder(JsonUnbakedModel unbakedModel, ModelOverrideList itemPropertyOverrides, boolean hasDepth, @Nullable Identifier quadTransformId) {
			this(unbakedModel.useAmbientOcclusion(), unbakedModel.getGuiLight().isSide(), unbakedModel.getTransformations(), itemPropertyOverrides, hasDepth, quadTransformId);
		}

		private Builder(boolean usesAo, boolean isSideLit, ModelTransformation transformation, ModelOverrideList itemPropertyOverrides, boolean hasDepth, @Nullable Identifier quadTransformId) {
			meshBuilder = RENDERER.meshBuilder();
			emitter = meshBuilder.getEmitter();
			this.itemPropertyOverrides = itemPropertyOverrides;
			this.usesAo = usesAo;
			this.isSideLit = isSideLit;
			this.transformation = transformation;
			this.hasDepth = hasDepth;
			this.quadTransformId = quadTransformId;
		}

		public JmxBakedModel.Builder setParticle(Sprite sprite) {
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

