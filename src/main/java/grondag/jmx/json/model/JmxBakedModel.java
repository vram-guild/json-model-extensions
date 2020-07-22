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
import org.apache.commons.lang3.ObjectUtils;

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
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.MaterialFinder;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.MeshBuilder;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.model.ModelHelper;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext.QuadTransform;
import net.fabricmc.fabric.api.util.TriState;

import grondag.jmx.impl.TransformableModel;
import grondag.jmx.impl.TransformableModelContext;
import grondag.jmx.json.ext.FaceExtData;
import grondag.jmx.json.ext.JmxExtension;
import grondag.jmx.json.ext.JmxMaterial;
import grondag.jmx.json.ext.JmxModelExt;
import grondag.jmx.target.FrexHolder;

import javax.annotation.Nullable;

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
		final SpriteAtlasTexture atlas = MinecraftClient.getInstance().getBakedModelManager().method_24153(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
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
			lists = ModelHelper.toQuadLists(mesh);
			quadLists = new WeakReference<>(lists);
		}
		final List<BakedQuad> result = lists[face == null ? 6 : face.getId()];
		return result == null ? ImmutableList.of() : result;
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
			if (this.quadTransformSource != null) {
				quadTransform = this.quadTransformSource.getForBlock(blockView, state, pos, randomSupplier);
			} else {
				quadTransform = null;
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
			if (this.quadTransformSource != null) {
				quadTransform = this.quadTransformSource.getForItem(stack, randomSupplier);
			} else {
				quadTransform = null;
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
			QuadTransformRegistry.QuadTransformSource quadTransformSupplier;
			if (quadTransformId != null) {
				quadTransformSupplier = QuadTransformRegistry.INSTANCE.getQuadTransform(quadTransformId);
				if (quadTransformSupplier == null) {
					throw new IllegalStateException("No quad transform is registered with ID " + quadTransformId);
				}
			} else {
				quadTransformSupplier = null;
			}

			return new JmxBakedModel(meshBuilder.build(), usesAo, isSideLit, particleTexture, transformation, itemPropertyOverrides, hasDepth, quadTransformSupplier);
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

			int depth = Math.max(extData.getDepth(), jmxMat.getDepth());

			if (FREX_RENDERER) {
				int maxDepth = ((grondag.frex.api.Renderer) RENDERER).maxSpriteDepth();
				if (depth > maxDepth) {
					throw new IllegalStateException("Model is using " + depth + " layers when only up to " + maxDepth + " are supported.");
				}

				emitter.material(getFrexMaterial(jmxMat, element, jmxMat.getDepth()));
				emitter.cullFace(cullFace);

				if(jmxMat.tag != 0) {
					emitter.tag(jmxMat.tag);
				}

				emitter.colorIndex(elementFace.tintIndex);

				for (int spriteIndex = 0; spriteIndex < depth; spriteIndex++) {
					if (spriteIndex != 0) {
						sprite = getSprite(spriteIndex, extData, spriteFunc);

						if (sprite == null) {
							continue;
						}
					}

					ModelElementTexture texData = extData.getTexData(spriteIndex, elementFace.textureData);

					QUADFACTORY_EXT.bake(emitter, spriteIndex, element, elementFace, texData, sprite, face, bakeProps, modelId);

					int color = jmxMat.getColor(spriteIndex);
					emitter.spriteColor(spriteIndex, color, color, color, color);
				}

				// With FREX will emit all sprites as one quad
				emitter.emit();
			} else {
				for (int spriteIndex = 0; spriteIndex < depth; spriteIndex++) {
					if (spriteIndex != 0) {
						sprite = getSprite(spriteIndex, extData, spriteFunc);

						if (sprite == null) {
							continue; // don't add quads with no sprite
						}
					}

					emitter.material(getNonFrexMaterial(jmxMat, element, spriteIndex));
					emitter.cullFace(cullFace);

					if(jmxMat.tag != 0) {
						emitter.tag(jmxMat.tag);
					}

					ModelElementTexture texData = extData.getTexData(spriteIndex, elementFace.textureData);

					QUADFACTORY_EXT.bake(emitter, 0, element, elementFace, texData, sprite, face, bakeProps, modelId);

					int color = jmxMat.getColor(spriteIndex);
					emitter.spriteColor(0, color, color, color, color);

					emitter.colorIndex(elementFace.tintIndex);

					emitter.emit();
				}
			}
		}

		@Nullable
		private Sprite getSprite(int spriteIndex, FaceExtData extData, Function<String, Sprite> spriteFunc) {
			String tex = extData.getTex(spriteIndex);

			if (tex == null) {
				return null;
			}

			Sprite sprite = spriteFunc.apply(tex);

			if (sprite.getId().equals(MissingSprite.getMissingSpriteId())) {
				return null;
			}

			return sprite;
		}

		private RenderMaterial getFrexMaterial(JmxMaterial jmxMat, ModelElement element, int depth) {
			if (jmxMat.preset != null) {
				RenderMaterial mat = FrexHolder.target().loadFrexMaterial(new Identifier(jmxMat.preset));

				if (mat != null) {
					return mat;
				}
			}

			final MaterialFinder finder = this.finder.clear();

			finder.spriteDepth(depth);

			for (int spriteIndex = 0; spriteIndex < depth; spriteIndex++) {
				TriState diffuse = jmxMat.getDiffuse(spriteIndex);
				finder.disableDiffuse(spriteIndex, diffuse == TriState.DEFAULT ? !element.shade : !diffuse.get());

				TriState ao = jmxMat.getAo(spriteIndex);
				finder.disableAo(spriteIndex, ao == TriState.DEFAULT ? !usesAo : !ao.get());

				finder.emissive(spriteIndex, jmxMat.getEmissive(spriteIndex).get());

				if (jmxMat.getColorIndex(spriteIndex) == TriState.FALSE) {
					finder.disableColorIndex(spriteIndex, true);
				}

				BlendMode layer = jmxMat.getLayer(spriteIndex);
				if (layer != null) {
					finder.blendMode(spriteIndex, layer);
				}
			}

			return finder.find();
		}

		private RenderMaterial getNonFrexMaterial(JmxMaterial jmxMat, ModelElement element, int spriteIndex) {
			final MaterialFinder finder = this.finder.clear();

			TriState diffuse = jmxMat.getDiffuse(spriteIndex);
			boolean disableDiffuse = diffuse == TriState.DEFAULT ? !element.shade : !diffuse.get();
			finder.disableDiffuse(0, disableDiffuse);

			TriState ao = jmxMat.getAo(spriteIndex);
			boolean disableAo = ao == TriState.DEFAULT ? !usesAo : !ao.get();
			finder.disableAo(0, disableAo);

			finder.emissive(0, jmxMat.getEmissive(spriteIndex).get());

			if (jmxMat.getColorIndex(spriteIndex) == TriState.FALSE) {
				finder.disableColorIndex(0, true);
			}

			BlendMode layer = jmxMat.getLayer(spriteIndex);
			if (layer != null) {
				finder.blendMode(0, layer);
			}

			return finder.find();
		}
	}
}

