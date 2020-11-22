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

package grondag.jmx.mixin;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
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

import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.ModelBakeSettings;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.client.render.model.json.ModelElement;
import net.minecraft.client.render.model.json.ModelElementFace;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import grondag.jmx.Configurator;
import grondag.jmx.JsonModelExtensions;
import grondag.jmx.impl.DerivedModelRegistryImpl;
import grondag.jmx.json.ext.FaceExtData;
import grondag.jmx.json.ext.JmxExtension;
import grondag.jmx.json.ext.JmxModelExt;
import grondag.jmx.json.ext.JsonUnbakedModelExt;
import grondag.jmx.json.model.JmxBakedModel;

@Environment(EnvType.CLIENT)
@Mixin(JsonUnbakedModel.class)
public abstract class MixinJsonUnbakedModel implements JsonUnbakedModelExt {
	@Shadow
	protected abstract ModelOverrideList compileOverrides(ModelLoader modelLoader,
			JsonUnbakedModel jsonUnbakedModel);

	@Shadow
	protected Identifier parentId;

	@Shadow
	protected Map<String, Either<SpriteIdentifier, String>> textureMap;

	private JsonUnbakedModelExt jmxParent;
	private JmxModelExt jmxModelExt;

	@Override
	public JmxModelExt jmx_modelExt() {
		return jmxModelExt;
	}

	@Override
	public JsonUnbakedModelExt jmx_parent() {
		return jmxParent;
	}

	@Override
	public Identifier jmx_parentId() {
		return parentId;
	}

	@Override
	public void jmx_parent(JsonUnbakedModelExt parent) {
		jmxParent = parent;

		if (jmxModelExt != null) {
			jmxModelExt.parent = parent.jmx_modelExt();
		}
	}

	@Override
	public Map<String, Either<SpriteIdentifier, String>> jmx_textureMap() {
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
	@Inject(at = @At("RETURN"), method = "getTextureDependencies")
	private void onGetTextureDependencies(Function<Identifier, UnbakedModel> modelFunc, Set<Pair<String, String>> errors,
			CallbackInfoReturnable<Collection<SpriteIdentifier>> ci) {

		if (jmxTextureDeps != null) {
			ci.getReturnValue().addAll(jmxTextureDeps);
		}

		if (jmxTextureErrors != null) {
			errors.addAll(jmxTextureErrors);
		}

		// We don't need the collection of material dependencies - this is just to map
		// parent relationships.
		final Set<JsonUnbakedModelExt> set = Sets.newLinkedHashSet();
		for (JsonUnbakedModelExt model = this; model.jmx_parentId() != null
				&& model.jmx_parent() == null; model = model.jmx_parent()) {
			set.add(model);
			UnbakedModel parentModel = modelFunc.apply(model.jmx_parentId());
			if (parentModel == null) {
				JsonModelExtensions.LOG.warn("No parent '{}' while loading model '{}'", parentId, model);
			}

			if (set.contains(parentModel)) {
				JsonModelExtensions.LOG.warn("Found 'parent' loop while loading model '{}' in chain: {} -> {}", model,
						set.stream().map(Object::toString).collect(Collectors.joining(" -> ")), parentId);
				parentModel = null;
			}

			if (parentModel != null && !(parentModel instanceof JsonUnbakedModel)) {
				throw new IllegalStateException("BlockModel parent has to be a block model.");
			}

			model.jmx_parent((JsonUnbakedModelExt) parentModel);
		}
	}


	private HashSet<SpriteIdentifier> jmxTextureDeps = null;

	private HashSet<SpriteIdentifier> getOrCreateJmxTextureDeps() {
		HashSet<SpriteIdentifier> result = jmxTextureDeps;
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

	@ModifyVariable(method = "getTextureDependencies", at = @At(value = "STORE", ordinal = 0), allow = 1, require = 1)
	private ModelElementFace hookTextureDeps(ModelElementFace face) {
		@SuppressWarnings("unchecked")
		final
		FaceExtData jmxData = ((JmxExtension<FaceExtData>) face).jmx_ext();
		final JsonUnbakedModel me = (JsonUnbakedModel) (Object) this;

		for (int i = 0; i < jmxData.getDepth(); i++) {
			final String texStr = jmxData.getLayer(i).texture;
			if (texStr != null && !texStr.isEmpty()) {
				final SpriteIdentifier tex = me.resolveSprite(texStr);

				if (Objects.equals(tex.getTextureId(), MissingSprite.getMissingSpriteId())) {
					getOrCreateJmxTextureErrors().add(Pair.of(texStr, me.id));
				} else {
					getOrCreateJmxTextureDeps().add(tex);
				}
			}
		}

		return face;
	}

	@SuppressWarnings("unchecked")
	@Inject(at = @At("HEAD"), method = "Lnet/minecraft/client/render/model/json/JsonUnbakedModel;bake(Lnet/minecraft/client/render/model/ModelLoader;Lnet/minecraft/client/render/model/json/JsonUnbakedModel;Ljava/util/function/Function;Lnet/minecraft/client/render/model/ModelBakeSettings;Lnet/minecraft/util/Identifier;Z)Lnet/minecraft/client/render/model/BakedModel;", cancellable = true)
	public void onBake(ModelLoader modelLoader, JsonUnbakedModel unbakedModel, Function<SpriteIdentifier, Sprite> spriteFunc,
			ModelBakeSettings bakeProps, Identifier modelId, boolean hasDepth, CallbackInfoReturnable<BakedModel> ci) {
		final JsonUnbakedModel me = (JsonUnbakedModel) (Object) this;

		// leave vanilla logic for built-ins
		if (me.getRootModel() == ModelLoader.BLOCK_ENTITY_MARKER) {
			return;
		}

		// if no JMX extensions, cannot be a template model for transforms
		// and not using JMX for vanilla, then use vanilla builder
		if (jmxModelExt == null || (!Configurator.loadVanillaModels && DerivedModelRegistryImpl.INSTANCE.isEmpty() && jmxModelExt.isEmpty())) {
			boolean isVanilla = true;
			final Iterator<ModelElement> elements = me.getElements().iterator();

			while (isVanilla && elements.hasNext()) {
				final ModelElement element = elements.next();
				final Iterator<ModelElementFace> faces = element.faces.values().iterator();

				while (faces.hasNext()) {
					final ModelElementFace face = faces.next();
					final FaceExtData faceExt = ((JmxExtension<FaceExtData>) face).jmx_ext();

					if(faceExt != null && !faceExt.isEmpty()) {
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
		final Sprite particleSprite = spriteFunc.apply(me.resolveSprite("particle"));
		final Function<String, Sprite> innerSpriteFunc = s -> spriteFunc.apply(me.resolveSprite(s));

		final JmxBakedModel.Builder builder = (new JmxBakedModel.Builder(me, compileOverrides(modelLoader, unbakedModel), hasDepth, jmxModelExt.getQuadTransformId()))
				.setParticle(particleSprite);
		final Iterator<ModelElement> elements = me.getElements().iterator();
		while (elements.hasNext()) {
			final ModelElement element = elements.next();
			final Iterator<Direction> faces = element.faces.keySet().iterator();

			while (faces.hasNext()) {
				final Direction face = faces.next();
				final ModelElementFace elementFace = element.faces.get(face);
				final FaceExtData extData = ((JmxExtension<FaceExtData>) elementFace).jmx_ext();

				final FaceExtData.LayerData layer = extData.getLayer(0);
				final String extTex = layer == null ? null : layer.texture;
				final String tex = extTex == null ? elementFace.textureId : extTex;

				final Sprite sprite = spriteFunc.apply(me.resolveSprite(tex));

				if (elementFace.cullFace == null) {
					builder.addQuad(null, jmxModelExt, innerSpriteFunc, element, elementFace, sprite, face, bakeProps, modelId);
				} else {
					final Direction roatedCullFace = Direction.transform(bakeProps.getRotation().getMatrix(), elementFace.cullFace);
					builder.addQuad(roatedCullFace, jmxModelExt, innerSpriteFunc, element, elementFace, sprite, face, bakeProps, modelId);
				}
			}
		}

		ci.setReturnValue(builder.build());
	}
}
