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

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.atlas.sources.SingleFile;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import io.vram.jmx.json.v1.JmxTexturesExtV1;

@Mixin(SingleFile.class)
public class MixinSingleFile {
	@Shadow private ResourceLocation resourceId;
	@Shadow private Optional<ResourceLocation> spriteId;

	@Inject(method = "<init>", at = @At("RETURN"))
	void blockDummySpriteLoad (ResourceLocation resourceLocation, Optional<Resource>  optional, CallbackInfo ci) {
		if (resourceId.equals(JmxTexturesExtV1.DUMMY_ID)) {
			resourceId = MissingTextureAtlasSprite.getLocation();
			spriteId = Optional.of(MissingTextureAtlasSprite.getLocation());
		}
	}
}
