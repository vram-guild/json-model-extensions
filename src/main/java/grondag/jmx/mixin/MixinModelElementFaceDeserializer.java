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

package grondag.jmx.mixin;

import java.lang.reflect.Type;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import grondag.jmx.json.ext.FaceExtData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.model.json.ModelElementFace;
import net.minecraft.util.math.Direction;

@Environment(EnvType.CLIENT)
@Mixin(ModelElementFace.Deserializer.class)
public class MixinModelElementFaceDeserializer {
	@Inject(method = "method_3397", allow = 1, require = 1, locals = LocalCapture.CAPTURE_FAILEXCEPTION,
			at = @At(
					value = "INVOKE",
					ordinal = 0,
					target = "Lnet/minecraft/client/render/model/json/ModelElementFace$Deserializer;deserializeCullFace(Lcom/google/gson/JsonObject;)Lnet/minecraft/util/math/Direction;"))
	private void wut(JsonElement jsonElement, Type type, JsonDeserializationContext context, CallbackInfoReturnable<Direction> ci, JsonObject jsonObj) {
		FaceExtData.deserialize(jsonObj, context);
	}
}
