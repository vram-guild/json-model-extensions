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

import grondag.jmx.json.FaceExtData;
import org.apache.commons.lang3.ObjectUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import grondag.jmx.json.ext.JmxExtension;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.model.json.ModelElementFace;

@Environment(EnvType.CLIENT)
@Mixin(ModelElementFace.class)
public class MixinModelElementFace implements JmxExtension<FaceExtData> {
	private FaceExtData jmx_ext;

	@Override
	public FaceExtData jmx_ext() {
		return jmx_ext;
	}

	@Override
	public void jmx_ext(FaceExtData val) {
		jmx_ext = val;
	}

	@Inject(at = @At("RETURN"), method = "<init>")
	private void onInit(CallbackInfo ci) {
		jmx_ext = ObjectUtils.defaultIfNull(FaceExtData.TRANSFER.get(), FaceExtData.empty());
	}
}
