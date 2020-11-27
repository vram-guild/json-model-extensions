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

package grondag.jmx.json.v1;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.datafixers.util.Either;
import grondag.jmx.target.FrexHolder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

@Environment(EnvType.CLIENT)
public final class JmxTexturesExtV1 {
	private static final boolean FREX_RENDERER = FrexHolder.target().isFrexRendererAvailable();
	/** prevents "unable to resolve" errors when 2nd texture layer isn't used */
	public static final Identifier DUMMY_ID = new Identifier("jmx", "dummy");
	public static final SpriteIdentifier DUMMY_SPRITE = new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, DUMMY_ID);
	private static final Either<SpriteIdentifier, String> DUMMY_EITHER = Either.left(DUMMY_SPRITE);

	public static void handleJmxTextures(JsonObject jsonObj, Map<String, Either<SpriteIdentifier, String>> map) {
		if(FREX_RENDERER && jsonObj.has("frex")) {
			handleJmxTexturesInner(jsonObj.getAsJsonObject("frex"), map);
		} else if(jsonObj.has("jmx")) {
			handleJmxTexturesInner(jsonObj.getAsJsonObject("jmx"), map);
		}
	}

	private static void handleJmxTexturesInner(JsonObject jsonObj, Map<String, Either<SpriteIdentifier, String>> map) {
		if(jsonObj.has("textures")) {
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

	private static boolean handleTexture(String key, JsonElement value, Map<String, Either<SpriteIdentifier, String>> map, Function<String, String> getTexture) {
		if (value.isJsonNull()) {
			map.put(key, DUMMY_EITHER);
			return true;
		} else {
			final String texture = value.getAsString();

			if (isReference(texture)) {
				map.put(key, Either.right(getTexture.apply(texture.substring(1))));
			} else {
				final SpriteIdentifier id = tryIdentifier(texture);
				map.put(key, Either.left(id));
			}
			return false;
		}
	}

	private static boolean isReference(String s) {
		return s.charAt(0) == '#';
	}

	private static SpriteIdentifier tryIdentifier(String s) {
		final Identifier id = Identifier.tryParse(s);
		if (id == null) {
			throw new JsonParseException(s + " is not a valid resource location.");
		} else {
			return new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, id);
		}
	}
}
