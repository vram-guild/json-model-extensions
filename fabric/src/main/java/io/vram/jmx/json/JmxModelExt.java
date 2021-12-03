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

package io.vram.jmx.json;

import java.util.function.Function;

import com.google.gson.JsonObject;

import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import io.vram.jmx.json.model.BakedQuadFactoryExt;
import io.vram.jmx.json.v0.JmxModelExtV0;
import io.vram.jmx.json.v1.JmxModelExtV1;

public abstract class JmxModelExt<Self extends JmxModelExt<Self>> {
	public static final ThreadLocal<JmxModelExt<?>> TRANSFER = new ThreadLocal<>();
	public static final ThreadLocal<Integer> VERSION = new ThreadLocal<>();

	public Self parent;

	public static void deserialize(JsonObject obj) {
		final JmxModelExt<?> modelExt;
		final int version = GsonHelper.getAsInt(obj, "jmx_version", 0);
		VERSION.set(version);

		switch (version) {
			case 0:
			default:
				modelExt = JmxModelExtV0.deserializeV0(obj);
				break;
			case 1:
				modelExt = JmxModelExtV1.deserializeV1(obj);
				break;
		}

		TRANSFER.set(modelExt);
	}

	public abstract int version();

	/**
	 * Checks whether any model in the hierarchy has any JMX data.
	 * If a ModelExt is empty, its associated model will be formed by vanilla.
	 */
	public boolean hierarchyIsEmpty() {
		return selfIsEmpty() && (parent == null || parent.hierarchyIsEmpty());
	}

	/**
	 * Checks whether only the current model has any JMX data.
	 */
	public abstract boolean selfIsEmpty();

	public abstract BakedModel buildModel(ItemOverrides modelOverrideList, boolean hasDepth, TextureAtlasSprite particleSprite, ModelState bakeProps, ResourceLocation modelId, BlockModel me, Function<Material, TextureAtlasSprite> textureGetter);

	protected static final FaceBakery QUADFACTORY = new FaceBakery();
	protected static final BakedQuadFactoryExt QUADFACTORY_EXT = (BakedQuadFactoryExt) QUADFACTORY;
}
