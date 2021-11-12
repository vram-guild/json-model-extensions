/*
 * Copyright © Contributing Authors
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

package io.vram.jmx.json;

import java.util.HashSet;
import java.util.function.Supplier;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;

import com.mojang.datafixers.util.Pair;

import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.resources.model.Material;

import io.vram.jmx.json.v0.FaceExtDataV0;
import io.vram.jmx.json.v1.FaceExtDataV1;

public abstract class FaceExtData {
	public static FaceExtData empty() {
		switch (JmxModelExt.VERSION.get()) {
			case 0:
			default:
				return FaceExtDataV0.EMPTY;
			case 1:
				return FaceExtDataV1.EMPTY;
		}
	}

	public static final ThreadLocal<FaceExtData> TRANSFER = new ThreadLocal<>();

	public static void deserialize(JsonObject jsonObj, JsonDeserializationContext context) {
		final FaceExtData faceExt;

		switch (JmxModelExt.VERSION.get()) {
			case 0:
			default:
				faceExt = FaceExtDataV0.deserializeV0(jsonObj, context);
				break;
			case 1:
				faceExt = FaceExtDataV1.deserializeV1(jsonObj, context);
				break;
		}

		TRANSFER.set(faceExt);
	}

	/**
	 * If all <code>FaceExtData</code>s in a model are empty, the model will be formed by vanilla.
	 */
	public abstract boolean isEmpty();

	public abstract void getTextureDependencies(BlockModel model, Supplier<HashSet<Pair<String, String>>> errors, Supplier<HashSet<Material>> deps);
}
