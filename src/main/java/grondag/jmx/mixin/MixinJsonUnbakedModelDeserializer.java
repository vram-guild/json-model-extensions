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

import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import grondag.jmx.json.JmxModelExt;
import grondag.jmx.json.JmxTexturesExt;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.client.util.SpriteIdentifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Environment(EnvType.CLIENT)
@Mixin(JsonUnbakedModel.Deserializer.class)
public class MixinJsonUnbakedModelDeserializer {
	@Inject(at = @At("RETURN"), method = "deserializeTextures")
	private void onDeserializeTextures(JsonObject jsonObj, CallbackInfoReturnable<Map<String, Either<SpriteIdentifier, String>>> ci) {
		JmxTexturesExt.handleJmxTextures(jsonObj, ci.getReturnValue());
	}

	@ModifyVariable(method = "deserialize", at = @At(value = "STORE", ordinal = 0), allow = 1, require = 1)
	private JsonObject hookDeserialize(JsonObject jsonObj) {
		JmxModelExt.deserialize(jsonObj);
		return jsonObj;
	}
}
