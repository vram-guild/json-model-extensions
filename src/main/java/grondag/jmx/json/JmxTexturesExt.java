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

import java.util.Map;

import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;

import net.minecraft.client.resources.model.Material;

import grondag.jmx.json.v0.JmxTexturesExtV0;
import grondag.jmx.json.v1.JmxTexturesExtV1;

public abstract class JmxTexturesExt {
	public static void handleJmxTextures(JsonObject obj, Map<String, Either<Material, String>> map) {
		switch (JmxModelExt.VERSION.get()) {
			case 0:
			default:
				JmxTexturesExtV0.handleTexturesV0(obj, map);
				break;
			case 1:
				JmxTexturesExtV1.handleTexturesV1(obj, map);
		}
	}
}
