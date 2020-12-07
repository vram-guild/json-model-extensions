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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.MultipartBakedModel;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;

import grondag.jmx.impl.InverseStateMap;
import grondag.jmx.impl.TransformableModel;
import grondag.jmx.impl.TransformableModelContext;

@Environment(EnvType.CLIENT)
@Mixin(MultipartBakedModel.class)
public abstract class MixinMultipartBakedModel implements FabricBakedModel, TransformableModel {
	@Shadow protected List<Pair<Predicate<BlockState>, BakedModel>> components;
	@Shadow protected Map<BlockState, BitSet> stateCache;

	private boolean isVanilla = true;

	@Inject(at = @At("RETURN"), method = "<init>")
	private void onInit(List<Pair<Predicate<BlockState>, BakedModel>> list, CallbackInfo ci) {
		final BakedModel defaultModel = list.iterator().next().getRight();
		isVanilla = ((FabricBakedModel) defaultModel).isVanillaAdapter();
	}

	@Override
	public BakedModel derive(TransformableModelContext context) {
		final List<Pair<Predicate<BlockState>, BakedModel>> newComponents = new ArrayList<>();
		components.forEach(c -> {
			final BakedModel template = c.getRight();
			final BakedModel newModel = (template instanceof TransformableModel) ? ((TransformableModel) template).derive(context) : template;
			final Predicate<BlockState> oldPredicate = c.getLeft();
			final InverseStateMap stateInverter = context.inverseStateMap()::invert;
			final Predicate<BlockState> newPredicate = s -> oldPredicate.test(stateInverter.invert(s));
			newComponents.add(Pair.of(newPredicate, newModel));
		});

		return new MultipartBakedModel(newComponents);
	}

	@Override
	public boolean isVanillaAdapter() {
		return isVanilla;
	}

	@Override
	public void emitBlockQuads(BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, RenderContext context) {
		if (state == null) {
			return;
		} else {
			BitSet bits = stateCache.get(state);

			if (bits == null) {
				bits = new BitSet();

				for (int i = 0; i < components.size(); ++i) {
					final Pair<Predicate<BlockState>, BakedModel> pair = components.get(i);

					if (pair.getLeft().test(state)) {
						bits.set(i);
					}
				}

				stateCache.put(state, bits);
			}

			final int limit = bits.length();

			for (int i = 0; i < limit; ++i) {
				if (bits.get(i)) {
					((FabricBakedModel) components.get(i).getRight()).emitBlockQuads(blockView, state, pos, randomSupplier, context);
				}
			}
		}
	}

	@Override
	public void emitItemQuads(ItemStack stack, Supplier<Random> randomSupplier, RenderContext context) {
		//NOOP
	}
}
