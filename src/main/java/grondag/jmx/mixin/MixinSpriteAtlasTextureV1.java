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

import java.util.concurrent.ConcurrentLinkedQueue;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import grondag.jmx.json.v1.JmxTexturesExtV1;

@Mixin(SpriteAtlasTexture.class)
public class MixinSpriteAtlasTextureV1 {
	@Inject(
		method = "method_18160",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/texture/SpriteAtlasTexture;getTexturePath(Lnet/minecraft/util/Identifier;)Lnet/minecraft/util/Identifier;"),
		cancellable = true
	)
	void blockDummySpriteLoad(Identifier id, ResourceManager resourceManager, ConcurrentLinkedQueue<Sprite.Info> queue, CallbackInfo ci) {
		if (id == JmxTexturesExtV1.DUMMY_ID) {
			ci.cancel();
		}
	}
}
