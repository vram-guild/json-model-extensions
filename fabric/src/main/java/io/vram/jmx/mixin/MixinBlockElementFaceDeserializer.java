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

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import net.minecraft.core.Direction;

import io.vram.jmx.json.FaceExtData;

@Mixin(targets = "net.minecraft.client.renderer.block.model.BlockElementFace$Deserializer")
public class MixinBlockElementFaceDeserializer {
	@Inject(method = "deserialize(Lcom/google/gson/JsonElement;Ljava/lang/reflect/Type;Lcom/google/gson/JsonDeserializationContext;)Lnet/minecraft/client/renderer/block/model/BlockElementFace;", allow = 1, require = 1, locals = LocalCapture.CAPTURE_FAILEXCEPTION,
			at = @At(
					value = "INVOKE",
					ordinal = 0,
					target = "Lnet/minecraft/client/renderer/block/model/BlockElementFace$Deserializer;getCullFacing(Lcom/google/gson/JsonObject;)Lnet/minecraft/core/Direction;"))
	private void hookDeserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context, CallbackInfoReturnable<Direction> ci, JsonObject jsonObj) {
		FaceExtData.deserialize(jsonObj, context);
	}
}
