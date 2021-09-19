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

package grondag.jmx.json.v0;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.MaterialFinder;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.util.TriState;

import grondag.jmx.JsonModelExtensions;
import grondag.jmx.json.JmxModelExt;
import grondag.jmx.json.ext.JmxExtension;
import grondag.jmx.json.model.JmxBakedModel;
import grondag.jmx.target.FrexHolder;

public class JmxModelExtV0 extends JmxModelExt<JmxModelExtV0> {
	private static final boolean FREX_RENDERER = FrexHolder.target().isFrexRendererAvailable();

	private final Map<String, Object> materialMap;
	@Nullable
	private final ResourceLocation quadTransformId;

	private JmxModelExtV0(Map<String, Object> materialMap, @Nullable ResourceLocation quadTransformId) {
		this.materialMap = materialMap;
		this.quadTransformId = quadTransformId;
	}

	public static JmxModelExtV0 deserializeV0(JsonObject obj) {
		if (FREX_RENDERER && obj.has("frex")) {
			return deserializeInner(obj.getAsJsonObject("frex"));
		} else if (obj.has("jmx")) {
			return deserializeInner(obj.getAsJsonObject("jmx"));
		} else {
			return new JmxModelExtV0(Collections.emptyMap(), null);
		}
	}

	@Override
	public int version() {
		return 0;
	}

	@Override
	public boolean selfIsEmpty() {
		return getQuadTransformId() == null && materialMap.isEmpty();
	}

	@Nullable
	private ResourceLocation getQuadTransformId() {
		return quadTransformId == null && parent != null ? parent.getQuadTransformId() : quadTransformId;
	}

	public JmxMaterialV0 resolveMaterial(String matName) {
		return matName == null || materialMap == null ? JmxMaterialV0.DEFAULT : resolveMaterialInner(matName);
	}

	private JmxMaterialV0 resolveMaterialInner(String matName) {
		if (!isMaterialReference(matName)) {
			matName = '#' + matName;
		}

		final Object result = resolveMaterial(matName, new MaterialResolutionContext(this));
		return result instanceof JmxMaterialV0 ? (JmxMaterialV0) result : JmxMaterialV0.DEFAULT;
	}

	private Object resolveMaterial(Object val, MaterialResolutionContext context) {
		if (isMaterialReference(val)) {
			if (this == context.current) {
				JsonModelExtensions.LOG.warn("Unable to resolve material due to upward reference: {}", val);
				return JmxMaterialV0.DEFAULT;
			} else {
				Object result = materialMap.get(((String) val).substring(1));

				if (result instanceof JmxMaterialV0) {
					return result;
				}

				if (result == null && parent != null) {
					result = parent.resolveMaterial(val, context);
				}

				if (isMaterialReference(result)) {
					context.current = this;
					result = context.root.resolveMaterial(result, context);
				}

				return result;
			}
		} else {
			return val;
		}
	}

	public static boolean isMaterialReference(Object val) {
		return val instanceof String && ((String) val).charAt(0) == '#';
	}

	public static final class MaterialResolutionContext {
		public final JmxModelExtV0 root;
		public JmxModelExtV0 current;

		private MaterialResolutionContext(JmxModelExtV0 root) {
			this.root = root;
		}
	}

	private static JmxModelExtV0 deserializeInner(JsonObject jsonObj) {
		final Object2ObjectOpenHashMap<String, Object> map = new Object2ObjectOpenHashMap<>();

		if (jsonObj.has("materials")) {
			final JsonObject job = jsonObj.getAsJsonObject("materials");

			for (final Entry<String, JsonElement> e : job.entrySet()) {
				if (e.getValue().isJsonObject()) {
					map.put(e.getKey(), new JmxMaterialV0(e.getKey(), e.getValue().getAsJsonObject()));
				} else {
					map.put(e.getKey(), e.getValue().getAsString());
				}
			}
		}

		final String idString = GsonHelper.getAsString(jsonObj, "quad_transform", null);
		final ResourceLocation quadTransformId;

		if (idString != null) {
			quadTransformId = ResourceLocation.tryParse(idString);
		} else {
			quadTransformId = null;
		}

		return new JmxModelExtV0(map, quadTransformId);
	}

	@Override
	public BakedModel buildModel(ItemOverrides modelOverrideList, boolean hasDepth, TextureAtlasSprite particleSprite, ModelState bakeProps, ResourceLocation modelId, BlockModel me, Function<Material, TextureAtlasSprite> textureGetter) {
		final Function<String, TextureAtlasSprite> innerSpriteFunc = s -> textureGetter.apply(me.getMaterial(s));

		final JmxBakedModel.Builder builder = (new JmxBakedModel.Builder(me, modelOverrideList, hasDepth, getQuadTransformId()))
				.setParticle(particleSprite);

		final MaterialFinder finder = RendererAccess.INSTANCE.getRenderer().materialFinder();

		for (final BlockElement element : me.getElements()) {
			for (final Direction face : element.faces.keySet()) {
				final BlockElementFace elementFace = element.faces.get(face);
				@SuppressWarnings("unchecked")
				//noinspection unchecked
				final FaceExtDataV0 extData = ((JmxExtension<FaceExtDataV0>) elementFace).jmx_ext();

				final String extTex = extData.getTex(0);
				final String tex = extTex == null ? elementFace.texture : extTex;

				final TextureAtlasSprite sprite = textureGetter.apply(me.getMaterial(tex));

				final Direction cullFace = elementFace.cullForDirection == null ? null : Direction.rotate(bakeProps.getRotation().getMatrix(), elementFace.cullForDirection);

				addQuad(bakeProps, modelId, innerSpriteFunc, builder, finder, element, face, elementFace, extData, sprite, cullFace);
			}
		}

		return builder.build();
	}

	private void addQuad(ModelState bakeProps, ResourceLocation modelId, Function<String, TextureAtlasSprite> innerSpriteFunc, JmxBakedModel.Builder builder, MaterialFinder finder, BlockElement element, Direction face, BlockElementFace elementFace, FaceExtDataV0 extData, TextureAtlasSprite sprite, Direction cullFace) {
		final JmxMaterialV0 jmxMat = resolveMaterial(extData.jmx_material);

		final QuadEmitter emitter = builder.emitter;

		final int depth = Math.max(extData.getDepth(), jmxMat.getDepth());

		for (int spriteIndex = 0; spriteIndex < depth; spriteIndex++) {
			if (spriteIndex != 0) {
				sprite = getSprite(spriteIndex, extData, innerSpriteFunc);

				if (sprite == null) {
					continue; // don't add quads with no sprite
				}
			}

			emitter.material(loadMaterial(finder, jmxMat, element, builder.usesAo, spriteIndex));
			emitter.cullFace(cullFace);

			if (jmxMat.tag != 0) {
				emitter.tag(jmxMat.tag);
			}

			final BlockFaceUV texData = extData.getTexData(spriteIndex, elementFace.uv);

			QUADFACTORY_EXT.jmx_bake(emitter, 0, element, elementFace, texData, sprite, face, bakeProps, modelId);

			final int color = jmxMat.getColor(spriteIndex);
			emitter.spriteColor(0, color, color, color, color);

			emitter.colorIndex(elementFace.tintIndex);

			emitter.emit();
		}
	}

	@Nullable
	private TextureAtlasSprite getSprite(int spriteIndex, FaceExtDataV0 extData, Function<String, TextureAtlasSprite> spriteFunc) {
		final String tex = extData.getTex(spriteIndex);

		if (tex == null) {
			return null;
		}

		final TextureAtlasSprite sprite = spriteFunc.apply(tex);

		if (sprite.getName().equals(MissingTextureAtlasSprite.getLocation())) {
			return null;
		}

		return sprite;
	}

	private static RenderMaterial loadMaterial(MaterialFinder finder, JmxMaterialV0 jmxMat, BlockElement element, boolean usesAo, int spriteIndex) {
		finder.clear();

		final TriState diffuse = jmxMat.getDiffuse(spriteIndex);
		final boolean disableDiffuse = diffuse == TriState.DEFAULT ? !element.shade : !diffuse.get();
		finder.disableDiffuse(0, disableDiffuse);

		final TriState ao = jmxMat.getAo(spriteIndex);
		final boolean disableAo = ao == TriState.DEFAULT ? !usesAo : !ao.get();
		finder.disableAo(0, disableAo);

		finder.emissive(0, jmxMat.getEmissive(spriteIndex).get());

		if (jmxMat.getColorIndex(spriteIndex) == TriState.FALSE) {
			finder.disableColorIndex(0, true);
		}

		final BlendMode layer = jmxMat.getLayer(spriteIndex);

		if (layer != null) {
			finder.blendMode(0, layer);
		}

		return finder.find();
	}
}
