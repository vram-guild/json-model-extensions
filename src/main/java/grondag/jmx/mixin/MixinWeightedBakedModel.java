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
import java.util.Random;
import java.util.function.Supplier;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.WeightedBakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandom;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;

import grondag.jmx.impl.TransformableModel;
import grondag.jmx.impl.TransformableModelContext;

@Environment(EnvType.CLIENT)
@Mixin(WeightedBakedModel.class)
public abstract class MixinWeightedBakedModel implements BakedModel, FabricBakedModel, TransformableModel {
	@Shadow private List<WeightedEntry.Wrapper<BakedModel>> list;
	@Shadow private int totalWeight;

	private boolean isVanilla = true;

	@Override
	public BakedModel derive(TransformableModelContext context) {
		final WeightedBakedModel.Builder builder = new WeightedBakedModel.Builder();
		final MutableBoolean isVanilla = new MutableBoolean(true);

		list.forEach(m -> {
			final BakedModel template = m.getData();
			final BakedModel mNew = (template instanceof TransformableModel) ? ((TransformableModel) template).derive(context) : template;

			isVanilla.setValue(isVanilla.booleanValue() && ((FabricBakedModel) template).isVanillaAdapter());

			builder.add(mNew, m.getWeight().asInt());
		});

		this.isVanilla = isVanilla.booleanValue();

		return builder.build();
	}

	@Override
	public boolean isVanillaAdapter() {
		return isVanilla;
	}

	@Override
	public void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, RenderContext context) {
		final BakedModel model = getModel(randomSupplier.get());
		((FabricBakedModel) model).emitBlockQuads(blockView, state, pos, randomSupplier, context);
	}

	@Override
	public void emitItemQuads(ItemStack stack, Supplier<Random> randomSupplier, RenderContext context) {
		final BakedModel model = getModel(randomSupplier.get());
		((FabricBakedModel) model).emitItemQuads(stack, randomSupplier, context);
	}

	private BakedModel getModel(Random random) {
		return WeightedRandom.getWeightedItem(list, Math.abs((int) random.nextLong()) % totalWeight).get().getData();
	}
}
