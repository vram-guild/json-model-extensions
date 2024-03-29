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

import java.util.Map;

import com.google.gson.JsonObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.datafixers.util.Either;

import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.resources.model.Material;

import io.vram.jmx.json.JmxModelExt;
import io.vram.jmx.json.JmxTexturesExt;

@Mixin(BlockModel.Deserializer.class)
public class MixinBlockModelDeserializer {
	@Inject(at = @At("RETURN"), method = "getTextureMap")
	private void onGetTextureMap(JsonObject jsonObj, CallbackInfoReturnable<Map<String, Either<Material, String>>> ci) {
		JmxTexturesExt.handleJmxTextures(jsonObj, ci.getReturnValue());
	}

	@ModifyVariable(method = "deserialize", at = @At(value = "STORE", ordinal = 0), allow = 1, require = 1)
	private JsonObject hookDeserialize(JsonObject jsonObj) {
		JmxModelExt.deserialize(jsonObj);
		return jsonObj;
	}
}
