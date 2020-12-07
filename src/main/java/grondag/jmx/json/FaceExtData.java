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

package grondag.jmx.json;

import java.util.HashSet;
import java.util.function.Supplier;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;

import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.client.util.SpriteIdentifier;

import grondag.jmx.json.v0.FaceExtDataV0;
import grondag.jmx.json.v1.FaceExtDataV1;

public abstract class FaceExtData {
	public static FaceExtData empty() {
		switch (JmxModelExt.VERSION.get()) {
			case 0:
			default:
				return FaceExtDataV0.EMPTY;
			case 1:
				return FaceExtDataV1.EMPTY;
		}
	}

	public static final ThreadLocal<FaceExtData> TRANSFER = new ThreadLocal<>();

	public static void deserialize(JsonObject jsonObj, JsonDeserializationContext context) {
		final FaceExtData faceExt;

		switch (JmxModelExt.VERSION.get()) {
			case 0:
			default:
				faceExt = FaceExtDataV0.deserializeV0(jsonObj, context);
				break;
			case 1:
				faceExt = FaceExtDataV1.deserializeV1(jsonObj, context);
				break;
		}

		TRANSFER.set(faceExt);
	}

	/**
	 * If all <code>FaceExtData</code>s in a model are empty, the model will be formed by vanilla.
	 */
	public abstract boolean isEmpty();

	public abstract void getTextureDependencies(JsonUnbakedModel model, Supplier<HashSet<Pair<String, String>>> errors, Supplier<HashSet<SpriteIdentifier>> deps);
}
