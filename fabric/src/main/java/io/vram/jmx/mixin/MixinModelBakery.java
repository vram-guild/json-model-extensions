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

package io.vram.jmx.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.renderer.texture.AtlasSet;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.util.profiling.ProfilerFiller;

import io.vram.jmx.Configurator;
import io.vram.jmx.JsonModelExtensions;
import io.vram.jmx.json.v1.JmxModelExtV1;

@Mixin(ModelBakery.class)
public class MixinModelBakery {
	@Inject(method = "uploadTextures", at = @At("TAIL"))
	void logErrorPresence(TextureManager textureManager, ProfilerFiller profiler, CallbackInfoReturnable<AtlasSet> cir) {
		if (!Configurator.logResolutionErrors && JmxModelExtV1.HAS_ERROR) {
			JsonModelExtensions.LOG.warn("One or more errors occurred in JMX model(s). Enable `log-resolution-errors` in config/jmx.properties to display all errors.");
		}
	}
}
