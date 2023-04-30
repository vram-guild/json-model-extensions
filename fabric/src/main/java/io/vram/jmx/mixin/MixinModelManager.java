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

package io.vram.jmx.mixin;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.client.resources.model.ModelManager;

@Mixin(ModelManager.class)
public class MixinModelManager {
	// @Inject(method = "apply", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/ModelBakery;getBakedTopLevelModels()Ljava/util/Map;"))
	// void logErrorPresence(ModelManager.ReloadState reloadState, ProfilerFiller profilerFiller, CallbackInfo ci) {
	// 	if (!Configurator.logResolutionErrors && JmxModelExtV1.HAS_ERROR) {
	// 		JsonModelExtensions.LOG.warn("One or more errors occurred in JMX model(s). Enable `log-resolution-errors` in config/jmx.properties to display all errors.");
	// 	}
	// }
}
