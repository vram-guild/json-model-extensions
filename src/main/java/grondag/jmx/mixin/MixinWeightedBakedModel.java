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

import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.WeightedBakedModel;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.WeightedPicker;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;

import grondag.jmx.impl.TransformableModel;
import grondag.jmx.impl.TransformableModelContext;
import grondag.jmx.json.ext.ModelEntryAccess;

@Environment(EnvType.CLIENT)
@Mixin(WeightedBakedModel.class)
public abstract class MixinWeightedBakedModel implements BakedModel, FabricBakedModel, TransformableModel {
	@SuppressWarnings("rawtypes")
	@Shadow private List models;
	@Shadow private int totalWeight;

	private boolean isVanilla = true;

	@SuppressWarnings("unchecked")
	@Override
	public BakedModel derive(TransformableModelContext context) {
		final WeightedBakedModel.Builder builder = new WeightedBakedModel.Builder();
		final MutableBoolean isVanilla = new MutableBoolean(true);

		models.forEach(m -> {
			final ModelEntryAccess me = (ModelEntryAccess) m;
			final BakedModel template = me.jmx_getModel();
			final BakedModel mNew = (template instanceof TransformableModel) ? ((TransformableModel) template).derive(context) : template;

			isVanilla.setValue(isVanilla.booleanValue() && ((FabricBakedModel) ((ModelEntryAccess) m).jmx_getModel()).isVanillaAdapter());

			builder.add(mNew, me.jmx_getWeight());
		});

		this.isVanilla = isVanilla.booleanValue();

		return builder.getFirst();
	}

	@Override
	public boolean isVanillaAdapter() {
		return this.isVanilla;
	}

	@Override
	public void emitBlockQuads(BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, RenderContext context) {
		final BakedModel model = getModel(randomSupplier.get());
		((FabricBakedModel) model).emitBlockQuads(blockView, state, pos, randomSupplier, context);
	}

	@Override
	public void emitItemQuads(ItemStack stack, Supplier<Random> randomSupplier, RenderContext context) {
		final BakedModel model = getModel(randomSupplier.get());
		((FabricBakedModel) model).emitItemQuads(stack, randomSupplier, context);
	}

	@SuppressWarnings("unchecked")
	private BakedModel getModel(Random random) {
		return ((ModelEntryAccess) WeightedPicker.getAt(models, Math.abs((int) random.nextLong()) % totalWeight)).jmx_getModel();
	}
}
