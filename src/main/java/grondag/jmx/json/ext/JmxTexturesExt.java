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

package grondag.jmx.json.ext;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

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

@Environment(EnvType.CLIENT)
public final class JmxTexturesExt {
	private static final boolean FREX_RENDERER = FrexHolder.target().isFrexRendererAvailable();
	/** prevents "unable to resolve" errors when 2nd texture layer isn't used */
	private static final Either<SpriteIdentifier, String> DUMMY_TEX = Either.left(new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEX, new Identifier("minecraft:block/cobblestone")));

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
			final Iterator<Entry<String, JsonElement>> it = job.entrySet().iterator();

			while(it.hasNext()) {
				final Entry<String, JsonElement> entry = it.next();

				if(entry.getValue().isJsonNull()) {
					map.put(entry.getKey(), DUMMY_TEX);
				} else {
					final String val = entry.getValue().getAsString();

					if (val.charAt(0) == '#') {
						map.put(entry.getKey(), Either.right(val.substring(1)));
					} else {
						final Identifier id = Identifier.tryParse(val);

						if (id == null) {
							throw new JsonParseException(val + " is not valid resource location");
						} else {
							map.put(entry.getKey(), Either.left(new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEX, id)));
						}
					}
				}
			}
		}
	}
}
