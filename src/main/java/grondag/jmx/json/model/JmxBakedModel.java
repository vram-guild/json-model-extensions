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

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableList;
import grondag.jmx.api.QuadTransformRegistry;
import grondag.jmx.impl.TransformableModel;
import grondag.jmx.impl.TransformableModelContext;
import grondag.jmx.json.ext.FaceExtData;
import grondag.jmx.json.ext.JmxExtension;
import grondag.jmx.json.ext.JmxMaterial;
import grondag.jmx.json.ext.JmxModelExt;
import grondag.jmx.target.FrexHolder;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.BakedQuadFactory;
import net.minecraft.client.render.model.ModelBakeSettings;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.client.render.model.json.ModelElement;
import net.minecraft.client.render.model.json.ModelElementFace;
import net.minecraft.client.render.model.json.ModelElementTexture;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.MissingSprite;
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
import net.fabricmc.fabric.api.renderer.v1.material.MaterialFinder;
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.MeshBuilder;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext.QuadTransform;

@Environment(EnvType.CLIENT)
public class JmxBakedModel implements BakedModel, FabricBakedModel, TransformableModel {
	protected static final Renderer RENDERER = RendererAccess.INSTANCE.getRenderer();
	protected static final boolean FREX_RENDERER = FrexHolder.target().isFrexRendererAvailable();

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
			if(transform.transform(emitter)) {
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

		if(lists == null) {
			lists = safeToQuadLists(mesh, particleSprite);
			quadLists = new WeakReference<>(lists);
		}

		final List<BakedQuad> result = lists[face == null ? 6 : face.getId()];
		return result == null ? ImmutableList.of() : result;
	}

	/**
	 * Workaround for Fabric helper breaking when called before the sprite atlas is created.
	 * Triggered by AE2 when running with JSON mesh loading active.
	 *
	 * Only difference is we use our particle sprite instead of looking one up.
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
		if(mesh != null) {
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
		if(mesh != null) {
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
		private final MaterialFinder finder;
		private final QuadEmitter emitter;
		private final ModelOverrideList itemPropertyOverrides;
		private final boolean usesAo;
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
			finder = RENDERER.materialFinder();
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

		private static final BakedQuadFactory QUADFACTORY = new BakedQuadFactory();
		private static final BakedQuadFactoryExt QUADFACTORY_EXT = (BakedQuadFactoryExt)QUADFACTORY;

		/**
		 * Intent here is to duplicate vanilla baking exactly.  Code is adapted from BakedQuadFactory.
		 */
		public void addQuad(Direction cullFace, JmxModelExt modelExt, Function<String, Sprite> spriteFunc, ModelElement element, ModelElementFace elementFace, Sprite sprite, Direction face, ModelBakeSettings bakeProps, Identifier modelId) {
			@SuppressWarnings("unchecked")
			final
			FaceExtData extData = ObjectUtils.defaultIfNull(((JmxExtension<FaceExtData>)elementFace).jmx_ext(), FaceExtData.EMPTY);
			final JmxMaterial jmxMat = modelExt == null ? JmxMaterial.DEFAULT : modelExt.resolveMaterial(extData.jmx_material);

			final QuadEmitter emitter = this.emitter;

			final int depth = Math.max(extData.getDepth(), jmxMat.getDepth());

			for (int spriteIndex = 0; spriteIndex < depth; spriteIndex++) {
				if (spriteIndex != 0) {
					sprite = getSprite(spriteIndex, extData, spriteFunc);

					if (sprite == null) {
						continue; // don't add quads with no sprite
					}
				}

				emitter.material(FrexHolder.target().loadMaterial(finder, jmxMat, element, usesAo, spriteIndex));
				emitter.cullFace(cullFace);

				if(jmxMat.tag != 0) {
					emitter.tag(jmxMat.tag);
				}

				final ModelElementTexture texData = extData.getTexData(spriteIndex, elementFace.textureData);

				QUADFACTORY_EXT.jmx_bake(emitter, 0, element, elementFace, texData, sprite, face, bakeProps, modelId);

				final int color = jmxMat.getColor(spriteIndex);
				emitter.spriteColor(0, color, color, color, color);

				emitter.colorIndex(elementFace.tintIndex);

				emitter.emit();
			}
		}

		@Nullable
		private Sprite getSprite(int spriteIndex, FaceExtData extData, Function<String, Sprite> spriteFunc) {
			final String tex = extData.getTex(spriteIndex);

			if (tex == null) {
				return null;
			}

			final Sprite sprite = spriteFunc.apply(tex);

			if (sprite.getId().equals(MissingSprite.getMissingSpriteId())) {
				return null;
			}

			return sprite;
		}
	}
}

