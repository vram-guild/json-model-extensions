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

package grondag.jmx.json.ext;

import java.util.Map;

import com.mojang.datafixers.util.Either;

import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import grondag.jmx.json.JmxModelExt;

@Environment(EnvType.CLIENT)
public interface JsonUnbakedModelExt {
	JmxModelExt<?> jmx_modelExt();

	JsonUnbakedModelExt jmx_parent();

	ResourceLocation jmx_parentId();

	void jmx_parent(JsonUnbakedModelExt parent);

	Map<String, Either<Material, String>> jmx_textureMap();
}
