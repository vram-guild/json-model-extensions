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

package io.vram.jmx.json.v1;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.Nullable;

import com.mojang.datafixers.util.Either;

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
import io.vram.frex.api.material.MaterialLoader;
import io.vram.frex.api.material.RenderMaterial;
import io.vram.jmx.Configurator;
import io.vram.jmx.JsonModelExtensions;
import io.vram.jmx.json.JmxModelExt;
import io.vram.jmx.json.ext.JmxExtension;
import io.vram.jmx.json.model.BakedQuadFactoryExt;
import io.vram.jmx.json.model.JmxBakedModel;

public class JmxModelExtV1 extends JmxModelExt<JmxModelExtV1> {
	public static boolean HAS_ERROR = false;

	public final Map<String, Either<String, ResourceLocation>> materialMap;
	private final Map<String, Either<String, Integer>> colorMap;
	private final Map<String, Either<String, Integer>> tagMap;
	@Nullable
	private final ResourceLocation quadTransformId;

	private JmxModelExtV1(Map<String, Either<String, ResourceLocation>> materialMap, Map<String, Either<String, Integer>> colorMap, Map<String, Either<String, Integer>> tagMap, @Nullable ResourceLocation quadTransformId) {
		this.materialMap = materialMap;
		this.colorMap = colorMap;
		this.tagMap = tagMap;
		this.quadTransformId = quadTransformId;
	}

	@Override
	public int version() {
		return 1;
	}

	@Override
	public boolean selfIsEmpty() {
		return getQuadTransformId() == null && materialMap.isEmpty() && colorMap.isEmpty() && tagMap.isEmpty();
	}

	@Override
	public BakedModel buildModel(ItemOverrides modelOverrideList, boolean hasDepth, TextureAtlasSprite particleSprite, ModelState bakeProps, ResourceLocation modelId, BlockModel me, Function<Material, TextureAtlasSprite> textureGetter) {
		final JmxBakedModel.Builder builder = (new JmxBakedModel.Builder(me, modelOverrideList, hasDepth, getQuadTransformId()))
				.setParticle(particleSprite);

		for (final BlockElement element : me.getElements()) {
			for (final Direction face : element.faces.keySet()) {
				final BlockElementFace elementFace = element.faces.get(face);
				final Direction cullFace;

				if (elementFace.cullForDirection != null) {
					cullFace = Direction.rotate(bakeProps.getRotation().getMatrix(), elementFace.cullForDirection);
				} else {
					cullFace = null;
				}

				emitFace(builder.emitter, QUADFACTORY_EXT, cullFace, me::getMaterial, textureGetter, element, elementFace, face, bakeProps, modelId);
			}
		}

		return builder.build();
	}

	@Nullable
	private ResourceLocation getQuadTransformId() {
		if (quadTransformId != null) {
			return quadTransformId;
		}

		if (parent != null) {
			return parent.getQuadTransformId();
		}

		return null;
	}

	public Optional<Integer> resolveColor(String name) {
		return this.resolve(
			name,
			ext -> ext.colorMap,
			i -> i
		);
	}

	public Optional<Integer> resolveTag(String name) {
		return this.resolve(
			name,
			ext -> ext.tagMap,
			i -> i
		);
	}

	public Optional<RenderMaterial> resolveMaterial(String name) {
		return this.resolve(
			name,
			ext -> ext.materialMap,
			MaterialLoader::getOrLoadMaterial
		);
	}

	private <T, S> Optional<T> resolve(String name, Function<JmxModelExtV1, Map<String, Either<String, S>>> getMap, Function<S, T> loader) {
		return resolveInner(name, getMap, loader, new ResolutionContext(this)).right().flatMap(x -> x);
	}

	public <T, S> Either<String, Optional<T>> resolveInner(String name, Function<JmxModelExtV1, Map<String, Either<String, S>>> getMap, Function<S, T> loader, ResolutionContext context) {
		if (context.current == this) {
			return Either.right(Optional.empty());
		}

		final Map<String, Either<String, S>> map = getMap.apply(this);
		@Nullable
		final Either<String, S> found = map.get(name.substring(1));

		if (found == null) {
			//noinspection ConstantConditions // vanilla parent models will technically be V0, this should be removed when V0 is removed
			if (parent instanceof JmxModelExtV1) {
				return parent.resolveInner(name, getMap, loader, context);
			} else {
				return Either.right(Optional.empty());
			}
		}

		return found.map(
			(String nextReference) -> {
				context.current = this;
				return context.root.resolveInner(nextReference, getMap, loader, context);
			},
			(S storedValue) -> Either.right(Optional.ofNullable(loader.apply(storedValue)))
		);
	}

	protected static final class ResolutionContext {
		public final JmxModelExtV1 root;
		public JmxModelExtV1 current;

		public ResolutionContext(JmxModelExtV1 root) {
			this.root = root;
		}
	}

	public void emitFace(
			QuadEmitter emitter,
			BakedQuadFactoryExt quadFactoryExt,
			Direction cullFace,
			Function<String, Material> resolver,
			Function<Material, TextureAtlasSprite> textureGetter,
			BlockElement element,
			BlockElementFace elementFace,
			Direction face,
			ModelState bakeProps,
			ResourceLocation modelId
	) {
		@SuppressWarnings("unchecked")
		final FaceExtDataV1 extData = ObjectUtils.defaultIfNull(((JmxExtension<FaceExtDataV1>) elementFace).jmx_ext(), FaceExtDataV1.EMPTY);

		final int depth = Math.max(extData.getDepth(), 1);

		for (int i = 0; i < depth; i++) {
			final @Nullable FaceExtDataV1.LayerData layer = extData.getLayer(i);
			final TextureAtlasSprite sprite;

			if (layer != null) {
				final Material spriteId = resolver.apply(layer.texture);

				// workaround for having different # of quads in "jmx" and "frex"
				// reference equals is OK because JmxTexturesExt inserts this same field
				if (spriteId == JmxTexturesExtV1.DUMMY_SPRITE) {
					continue;
				} else if (spriteId.texture() == MissingTextureAtlasSprite.getLocation()) {
					continue;
				}

				sprite = textureGetter.apply(spriteId);

				if (sprite == null) {
					continue; // don't add quads with no sprite
				}
			} else {
				sprite = textureGetter.apply(resolver.apply(elementFace.texture));
			}

			final BlockFaceUV texData;

			if (layer == null) {
				texData = elementFace.uv;
			} else {
				texData = ObjectUtils.defaultIfNull(layer.texData, elementFace.uv);
			}

			quadFactoryExt.jmx_bake(emitter, 0, element, elementFace, texData, sprite, face, bakeProps, modelId);

			if (layer != null) {
				if (layer.material != null) {
					final Optional<RenderMaterial> material = resolveMaterial(layer.material);

					if (material.isPresent()) {
						emitter.material(material.get());
					} else {
						HAS_ERROR = true;

						if (Configurator.logResolutionErrors) {
							JsonModelExtensions.LOG.warn("Unable to resolve material {} in {}", layer.material, modelId);
						}
					}
				}

				if (layer.tag != null) {
					final Optional<Integer> tag = resolveTag(layer.tag);

					if (tag.isPresent()) {
						emitter.tag(tag.get());
					} else {
						HAS_ERROR = true;

						if (Configurator.logResolutionErrors) {
							JsonModelExtensions.LOG.warn("Unable to resolve tag {} in {}", layer.material, modelId);
						}
					}
				}

				if (layer.color != null) {
					final Optional<Integer> color = resolveColor(layer.color);

					if (color.isPresent()) {
						emitter.vertexColor(color.get(), color.get(), color.get(), color.get());
					} else {
						HAS_ERROR = true;

						if (Configurator.logResolutionErrors) {
							JsonModelExtensions.LOG.warn("Unable to resolve color {} in {}", layer.material, modelId);
						}
					}
				}
			}

			emitter.cullFace(cullFace);
			emitter.colorIndex(elementFace.tintIndex);

			emitter.emit();
		}
	}

	public static JmxModelExtV1 deserializeV1(JsonObject jsonObjIn) {
		if (jsonObjIn.has("frex")) {
			return deserializeInner(jsonObjIn.getAsJsonObject("frex"));
		} else if (jsonObjIn.has("jmx")) {
			return deserializeInner(jsonObjIn.getAsJsonObject("jmx"));
		} else {
			return new JmxModelExtV1(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), null);
		}
	}

	private static boolean isReference(JsonElement element) {
		if (!element.isJsonPrimitive()) {
			return false;
		}

		final JsonPrimitive primitive = element.getAsJsonPrimitive();

		if (!primitive.isString()) {
			return false;
		}

		return primitive.getAsString().charAt(0) == '#';
	}

	private static <S> Map<String, Either<String, S>> deserializeLayers(JsonObject obj, String key, Function<JsonElement, S> deserializer, S _default) {
		final Object2ObjectOpenHashMap<String, Either<String, S>> map = new Object2ObjectOpenHashMap<>();

		if (obj.has(key)) {
			final JsonArray arr = obj.getAsJsonArray(key);

			for (int i = 0, size = arr.size(); i < size; i++) {
				final JsonObject layer = arr.get(i).getAsJsonObject();

				for (final Entry<String, JsonElement> e : layer.entrySet()) {
					if (e.getValue().isJsonNull()) {
						map.put(e.getKey() + i, Either.right(_default));
					} else if (isReference(e.getValue())) {
						map.put(e.getKey() + i, Either.left(e.getValue().getAsString() + i));
					} else {
						map.put(e.getKey() + i, Either.right(deserializer.apply(e.getValue())));
					}
				}
			}
		}

		return map;
	}

	private static JmxModelExtV1 deserializeInner(JsonObject obj) {
		final Map<String, Either<String, ResourceLocation>> materials = deserializeLayers(obj, "materials", el -> new ResourceLocation(el.getAsString()), RenderMaterial.STANDARD_MATERIAL_KEY);
		final Map<String, Either<String, Integer>> tags = deserializeLayers(obj, "tags", JsonElement::getAsInt, 0);
		final Map<String, Either<String, Integer>> colors = deserializeLayers(obj, "colors", JmxModelExtV1::parseColor, 0xFFFFFFFF);

		final String idString = GsonHelper.getAsString(obj, "quad_transform", null);
		final ResourceLocation quadTransformId;

		if (idString != null) {
			quadTransformId = ResourceLocation.tryParse(idString);
		} else {
			quadTransformId = null;
		}

		return new JmxModelExtV1(materials, colors, tags, quadTransformId);
	}

	private static int parseColor(JsonElement element) {
		if (element.isJsonPrimitive()) {
			final JsonPrimitive primitive = element.getAsJsonPrimitive();

			if (primitive.isNumber()) {
				return primitive.getAsInt();
			} else if (primitive.isString()) {
				final String colorStr = primitive.getAsString();
				return colorStr.startsWith("0x") ? Integer.parseUnsignedInt(colorStr.substring(2), 16) : Integer.parseInt(colorStr);
			} else {
				throw new JsonParseException("Invalid color: " + element);
			}
		} else {
			throw new JsonParseException("Invalid color: " + element);
		}
	}
}
