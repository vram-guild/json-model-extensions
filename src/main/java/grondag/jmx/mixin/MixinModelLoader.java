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

package grondag.jmx.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.render.model.SpriteAtlasManager;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.profiler.Profiler;

import grondag.jmx.Configurator;
import grondag.jmx.JsonModelExtensions;
import grondag.jmx.json.v1.JmxModelExtV1;

@Mixin(ModelLoader.class)
public class MixinModelLoader {
	@Inject(method = "upload", at = @At("TAIL"))
	void logErrorPresence(TextureManager textureManager, Profiler profiler, CallbackInfoReturnable<SpriteAtlasManager> cir) {
		if (!Configurator.logResolutionErrors && JmxModelExtV1.HAS_ERROR) {
			JsonModelExtensions.LOG.warn("One or more errors occurred in JMX model(s). Enable `log-resolution-errors` in config/jmx.properties to display all errors.");
		}
	}
}
