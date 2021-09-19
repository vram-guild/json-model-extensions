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

package grondag.jmx.mixin;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import grondag.jmx.Configurator;
import grondag.jmx.JsonModelExtensions;
import grondag.jmx.impl.DerivedModelRegistryImpl;
import grondag.jmx.json.FaceExtData;
import grondag.jmx.json.JmxModelExt;
import grondag.jmx.json.ext.JmxExtension;
import grondag.jmx.json.ext.JsonUnbakedModelExt;

@Environment(EnvType.CLIENT)
@Mixin(BlockModel.class)
public abstract class MixinBlockModel implements JsonUnbakedModelExt {
	@Shadow
	protected abstract ItemOverrides getItemOverrides(ModelBakery modelBakery, BlockModel blockModel);

	@Shadow
	public String name;

	@Shadow
	protected ResourceLocation parentLocation;

	@Shadow
	protected Map<String, Either<Material, String>> textureMap;

	private JsonUnbakedModelExt jmxParent;
	private JmxModelExt<?> jmxModelExt;

	@Override
	public JmxModelExt<?> jmx_modelExt() {
		return jmxModelExt;
	}

	@Override
	public JsonUnbakedModelExt jmx_parent() {
		return jmxParent;
	}

	@Override
	public ResourceLocation jmx_parentId() {
		return parentLocation;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void jmx_parent(JsonUnbakedModelExt parent) {
		jmxParent = parent;

		if (jmxModelExt != null) {
			if (parent.jmx_modelExt().version() != jmxModelExt.version()) {
				JsonModelExtensions.LOG.warn(String.format("Model %s is v%d, but its parent (%s) is v%d", name, jmxModelExt.version(), parentLocation, parent.jmx_modelExt().version()));
			} else {
				//noinspection RedundantCast,rawtypes // rawtypes are the only thing keeping javac ok with this mess
				((JmxModelExt) jmxModelExt).parent = parent.jmx_modelExt();
			}
		}
	}

	@Override
	public Map<String, Either<Material, String>> jmx_textureMap() {
		return textureMap;
	}

	/**
	 * We use a threadlocal populated just before initialization vs trying to hook
	 * initialization directly.
	 */
	@Inject(at = @At("RETURN"), method = "<init>")
	private void onInit(CallbackInfo ci) {
		jmxModelExt = JmxModelExt.TRANSFER.get();
	}

	/**
	 * Appends JMX texture dependencies and computes material dependencies.
	 */
	@SuppressWarnings("unlikely-arg-type")
	@Inject(at = @At("RETURN"), method = "getMaterials")
	private void onGetMaterials(Function<ResourceLocation, UnbakedModel> modelFunc, Set<Pair<String, String>> errors, CallbackInfoReturnable<Collection<Material>> ci) {
		if (jmxTextureDeps != null) {
			ci.getReturnValue().addAll(jmxTextureDeps);
		}

		if (jmxTextureErrors != null) {
			errors.addAll(jmxTextureErrors);
		}

		// We don't need the collection of material dependencies - this is just to map
		// parent relationships.
		final Set<JsonUnbakedModelExt> set = Sets.newLinkedHashSet();
		for (JsonUnbakedModelExt model = this;
				model.jmx_parentId() != null && model.jmx_parent() == null;
				model = model.jmx_parent()
		) {
			set.add(model);
			UnbakedModel parentModel = modelFunc.apply(model.jmx_parentId());

			if (parentModel == null) {
				JsonModelExtensions.LOG.warn("No parent '{}' while loading model '{}'", parentLocation, model);
			}

			if (set.contains(parentModel)) {
				JsonModelExtensions.LOG.warn("Found 'parent' loop while loading model '{}' in chain: {} -> {}", model,
						set.stream().map(Object::toString).collect(Collectors.joining(" -> ")), parentLocation);
				parentModel = null;
			}

			if (parentModel != null && !(parentModel instanceof BlockModel)) {
				throw new IllegalStateException("BlockModel parent has to be a block model.");
			}

			model.jmx_parent((JsonUnbakedModelExt) parentModel);
		}
	}

	private HashSet<Material> jmxTextureDeps = null;

	private HashSet<Material> getOrCreateJmxTextureDeps() {
		HashSet<Material> result = jmxTextureDeps;

		if (result == null) {
			result = new HashSet<>();
			jmxTextureDeps = result;
		}

		return result;
	}

	private HashSet<Pair<String, String>> jmxTextureErrors = null;

	private HashSet<Pair<String, String>> getOrCreateJmxTextureErrors() {
		HashSet<Pair<String, String>> result = jmxTextureErrors;

		if (result == null) {
			result = new HashSet<>();
			jmxTextureErrors = result;
		}

		return result;
	}

	@ModifyVariable(method = "getMaterials", at = @At(value = "STORE", ordinal = 0), allow = 1, require = 1)
	private BlockElementFace hookTextureDeps(BlockElementFace face) {
		@SuppressWarnings("unchecked")
		final FaceExtData jmxData = ((JmxExtension<FaceExtData>) face).jmx_ext();
		final BlockModel me = (BlockModel) (Object) this;
		jmxData.getTextureDependencies(me, this::getOrCreateJmxTextureErrors, this::getOrCreateJmxTextureDeps);

		return face;
	}

	@SuppressWarnings("unchecked")
	@Inject(at = @At("HEAD"), method = "Lnet/minecraft/client/renderer/block/model/BlockModel;bake(Lnet/minecraft/client/resources/model/ModelBakery;Lnet/minecraft/client/renderer/block/model/BlockModel;Ljava/util/function/Function;Lnet/minecraft/client/resources/model/ModelState;Lnet/minecraft/resources/ResourceLocation;Z)Lnet/minecraft/client/resources/model/BakedModel;", cancellable = true)
	public void onBake(ModelBakery bakery, BlockModel unbakedModel, Function<Material, TextureAtlasSprite> textureGetter,
			ModelState bakeProps, ResourceLocation modelId, boolean hasDepth, CallbackInfoReturnable<BakedModel> ci) {
		final BlockModel me = (BlockModel) (Object) this;

		// leave vanilla logic for built-ins
		if (me.getRootModel() == ModelBakery.BLOCK_ENTITY_MARKER) {
			return;
		}

		// if no JMX extensions, cannot be a template model for transforms
		// and not using JMX for vanilla, then use vanilla builder
		if (jmxModelExt == null || (!Configurator.loadVanillaModels && DerivedModelRegistryImpl.INSTANCE.isEmpty() && jmxModelExt.hierarchyIsEmpty())) {
			boolean isVanilla = true;
			final Iterator<BlockElement> elements = me.getElements().iterator();

			while (isVanilla && elements.hasNext()) {
				final BlockElement element = elements.next();
				final Iterator<BlockElementFace> faces = element.faces.values().iterator();

				while (faces.hasNext()) {
					final BlockElementFace face = faces.next();
					final FaceExtData faceExt = ((JmxExtension<FaceExtData>) face).jmx_ext();

					if (faceExt != null && !faceExt.isEmpty()) {
						isVanilla = false;
						break;
					}
				}
			}

			if (isVanilla) {
				return;
			}
		}

		// build and return JMX model
		final TextureAtlasSprite particleSprite = textureGetter.apply(me.getMaterial("particle"));

		ci.setReturnValue(jmxModelExt.buildModel(
			getItemOverrides(bakery, unbakedModel),
			hasDepth,
			particleSprite,
			bakeProps,
			modelId,
			me,
			textureGetter
		));
	}
}
