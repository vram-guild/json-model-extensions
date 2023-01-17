/*
 * This file is part of JSON Model Extensions and is licensed to the project under
 * terms that are compatible with the GNU Lesser General Public License.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership and licensing.
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
 */

package io.vram.jmx.mixin;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.datafixers.util.Either;

import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;

import io.vram.jmx.Configurator;
import io.vram.jmx.JsonModelExtensions;
import io.vram.jmx.json.FaceExtData;
import io.vram.jmx.json.JmxModelExt;
import io.vram.jmx.json.ext.JmxExtension;
import io.vram.jmx.json.ext.JsonBlockModelExt;

@Mixin(BlockModel.class)
public abstract class MixinBlockModel implements JsonBlockModelExt {
	@Shadow
	protected abstract ItemOverrides getItemOverrides(ModelBaker modelBaker, BlockModel blockModel);

	@Shadow
	public String name;

	@Shadow
	protected ResourceLocation parentLocation;

	@Shadow
	protected Map<String, Either<Material, String>> textureMap;

	private JsonBlockModelExt jmxParent;
	private JmxModelExt<?> jmxModelExt;

	@Override
	public JmxModelExt<?> jmx_modelExt() {
		return jmxModelExt;
	}

	@Override
	public JsonBlockModelExt jmx_parent() {
		return jmxParent;
	}

	@Override
	public ResourceLocation jmx_parentId() {
		return parentLocation;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void jmx_parent(JsonBlockModelExt parent) {
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
	@Inject(at = @At("RETURN"), method = "resolveParents")
	private void onResolveParents(Function<ResourceLocation, UnbakedModel> modelFunc, CallbackInfo ci) {
		// We don't need the collection of material dependencies - this is just to map
		// parent relationships.
		final Set<JsonBlockModelExt> set = Sets.newLinkedHashSet();
		for (JsonBlockModelExt model = this;
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

			model.jmx_parent((JsonBlockModelExt) parentModel);
		}
	}

	@SuppressWarnings("unchecked")
	@Inject(at = @At("HEAD"), method = "bake(Lnet/minecraft/client/resources/model/ModelBaker;Lnet/minecraft/client/renderer/block/model/BlockModel;Ljava/util/function/Function;Lnet/minecraft/client/resources/model/ModelState;Lnet/minecraft/resources/ResourceLocation;Z)Lnet/minecraft/client/resources/model/BakedModel;", cancellable = true)
	public void onBake(ModelBaker baker, BlockModel unbakedModel, Function<Material, TextureAtlasSprite> textureGetter,
					   ModelState bakeProps, ResourceLocation modelId, boolean hasDepth, CallbackInfoReturnable<BakedModel> ci) {
		final BlockModel me = (BlockModel) (Object) this;

		// leave vanilla logic for built-ins
		if (me.getRootModel() == ModelBakery.BLOCK_ENTITY_MARKER) {
			return;
		}

		// if no JMX extensions, cannot be a template model for transforms
		// and not using JMX for vanilla, then use vanilla builder
		if (jmxModelExt == null || (!Configurator.loadVanillaModels && jmxModelExt.hierarchyIsEmpty())) {
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
			getItemOverrides(baker, unbakedModel),
			hasDepth,
			particleSprite,
			bakeProps,
			modelId,
			me,
			textureGetter
		));
	}
}
