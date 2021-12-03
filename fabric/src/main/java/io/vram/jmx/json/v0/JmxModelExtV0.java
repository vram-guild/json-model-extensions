/*
 * Copyright © Original Authors
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

package io.vram.jmx.json.v0;

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

import io.vram.frex.api.buffer.QuadEmitter;
import io.vram.frex.api.material.MaterialFinder;
import io.vram.frex.api.material.RenderMaterial;
import io.vram.frex.api.renderer.Renderer;
import io.vram.jmx.JsonModelExtensions;
import io.vram.jmx.json.JmxModelExt;
import io.vram.jmx.json.ext.JmxExtension;
import io.vram.jmx.json.model.JmxBakedModel;

public class JmxModelExtV0 extends JmxModelExt<JmxModelExtV0> {
	private final Map<String, Object> materialMap;
	@Nullable
	private final ResourceLocation quadTransformId;

	private JmxModelExtV0(Map<String, Object> materialMap, @Nullable ResourceLocation quadTransformId) {
		this.materialMap = materialMap;
		this.quadTransformId = quadTransformId;
	}

	public static JmxModelExtV0 deserializeV0(JsonObject obj) {
		if (obj.has("frex")) {
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

		final MaterialFinder finder = Renderer.get().materials().materialFinder();

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
			emitter.vertexColor(color, color, color, color);

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

		final Boolean diffuse = jmxMat.getDiffuse(spriteIndex);
		final boolean disableDiffuse = diffuse == null ? !element.shade : !diffuse;
		finder.disableDiffuse(disableDiffuse);

		final Boolean ao = jmxMat.getAo(spriteIndex);
		final boolean disableAo = ao == null ? !usesAo : !ao;
		finder.disableAo(disableAo);

		final Boolean emissive = jmxMat.getEmissive(spriteIndex);
		finder.emissive(emissive == null ? false : emissive);

		final Boolean colorIndex = jmxMat.getColorIndex(spriteIndex);

		if (colorIndex != null && !colorIndex) {
			finder.disableColorIndex(true);
		}

		finder.preset(jmxMat.getLayer(spriteIndex));

		return finder.find();
	}
}
