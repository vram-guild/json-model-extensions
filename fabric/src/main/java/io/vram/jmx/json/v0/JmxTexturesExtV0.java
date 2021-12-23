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

package io.vram.jmx.json.v0;

import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import com.mojang.datafixers.util.Either;

import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;

public final class JmxTexturesExtV0 {
	/** Prevents "unable to resolve" errors when 2nd texture layer isn't used. */
	private static final Either<Material, String> DUMMY_TEX = Either.left(new Material(TextureAtlas.LOCATION_BLOCKS, new ResourceLocation("minecraft:block/cobblestone")));

	public static void handleTexturesV0(JsonObject jsonObj, Map<String, Either<Material, String>> map) {
		if (jsonObj.has("frex")) {
			handleJmxTexturesInner(jsonObj.getAsJsonObject("frex"), map);
		} else if (jsonObj.has("jmx")) {
			handleJmxTexturesInner(jsonObj.getAsJsonObject("jmx"), map);
		}
	}

	private static void handleJmxTexturesInner(JsonObject jsonObj, Map<String, Either<Material, String>> map) {
		if (jsonObj.has("textures")) {
			final JsonObject job = jsonObj.getAsJsonObject("textures");

			for (final Entry<String, JsonElement> entry : job.entrySet()) {
				if (entry.getValue().isJsonArray()) {
					final JsonArray layeredTextures = entry.getValue().getAsJsonArray();

					for (int i = 0; i < layeredTextures.size(); i++) {
						final int i2 = i;

						for (final Entry<String, JsonElement> layerEntry : layeredTextures.get(i).getAsJsonObject().entrySet()) {
							handleTexture(layerEntry.getKey() + i, layerEntry.getValue(), map, x -> x + i2);
						}
					}
				} else {
					handleTexture(entry.getKey(), entry.getValue(), map, x -> x);
				}
			}
		}
	}

	private static void handleTexture(String key, JsonElement value, Map<String, Either<Material, String>> map, Function<String, String> getTexture) {
		if (value.isJsonNull()) {
			map.put(key, DUMMY_TEX);
		} else {
			final String texture = value.getAsString();

			if (isReference(texture)) {
				map.put(key, Either.right(getTexture.apply(texture.substring(1))));
			} else {
				final Material id = tryIdentifier(texture);
				map.put(key, Either.left(id));
			}
		}
	}

	private static boolean isReference(String s) {
		return s.charAt(0) == '#';
	}

	private static Material tryIdentifier(String s) {
		final ResourceLocation id = ResourceLocation.tryParse(s);

		if (id == null) {
			throw new JsonParseException(s + " is not a valid resource location.");
		} else {
			return new Material(TextureAtlas.LOCATION_BLOCKS, id);
		}
	}
}
