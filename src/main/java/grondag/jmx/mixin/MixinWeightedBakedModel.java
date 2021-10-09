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

package grondag.jmx.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.WeightedBakedModel;
import net.minecraft.util.random.WeightedEntry;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import grondag.jmx.impl.TransformableModel;
import grondag.jmx.impl.TransformableModelContext;

@Environment(EnvType.CLIENT)
@Mixin(WeightedBakedModel.class)
public abstract class MixinWeightedBakedModel implements TransformableModel {
	@Shadow private List<WeightedEntry.Wrapper<BakedModel>> list;
	@Shadow private int totalWeight;

	@Override
	public BakedModel derive(TransformableModelContext context) {
		final WeightedBakedModel.Builder builder = new WeightedBakedModel.Builder();

		list.forEach(m -> {
			final BakedModel template = m.getData();
			final BakedModel mNew = (template instanceof TransformableModel) ? ((TransformableModel) template).derive(context) : template;
			builder.add(mNew, m.getWeight().asInt());
		});

		return builder.build();
	}
}
