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

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.MultiPartBakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;

import grondag.jmx.impl.InverseStateMap;
import grondag.jmx.impl.TransformableModel;
import grondag.jmx.impl.TransformableModelContext;

@Environment(EnvType.CLIENT)
@Mixin(MultiPartBakedModel.class)
public abstract class MixinMultipartBakedModel implements FabricBakedModel, TransformableModel {
	@Shadow protected List<Pair<Predicate<BlockState>, BakedModel>> selectors;
	@Shadow protected Map<BlockState, BitSet> selectorCache;

	private boolean isVanilla = true;

	@Inject(at = @At("RETURN"), method = "<init>")
	private void onInit(List<Pair<Predicate<BlockState>, BakedModel>> list, CallbackInfo ci) {
		for (final Pair<Predicate<BlockState>, BakedModel> pair : list) {
			isVanilla &= ((FabricBakedModel) pair.getRight()).isVanillaAdapter();

			if (!isVanilla) break;
		}
	}

	@Override
	public BakedModel derive(TransformableModelContext context) {
		final List<Pair<Predicate<BlockState>, BakedModel>> newComponents = new ArrayList<>();
		selectors.forEach(c -> {
			final BakedModel template = c.getRight();
			final BakedModel newModel = (template instanceof TransformableModel) ? ((TransformableModel) template).derive(context) : template;
			final Predicate<BlockState> oldPredicate = c.getLeft();
			final InverseStateMap stateInverter = context.inverseStateMap()::invert;
			final Predicate<BlockState> newPredicate = s -> oldPredicate.test(stateInverter.invert(s));
			newComponents.add(Pair.of(newPredicate, newModel));
		});

		return new MultiPartBakedModel(newComponents);
	}

	@Override
	public boolean isVanillaAdapter() {
		return isVanilla;
	}

	@Override
	public void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, RenderContext context) {
		if (state == null) {
		} else {
			BitSet bits = selectorCache.get(state);

			if (bits == null) {
				bits = new BitSet();

				for (int i = 0; i < selectors.size(); ++i) {
					final Pair<Predicate<BlockState>, BakedModel> pair = selectors.get(i);

					if (pair.getLeft().test(state)) {
						bits.set(i);
					}
				}

				selectorCache.put(state, bits);
			}

			final int limit = bits.length();

			for (int i = 0; i < limit; ++i) {
				if (bits.get(i)) {
					((FabricBakedModel) selectors.get(i).getRight()).emitBlockQuads(blockView, state, pos, randomSupplier, context);
				}
			}
		}
	}

	@Override
	public void emitItemQuads(ItemStack stack, Supplier<Random> randomSupplier, RenderContext context) {
		//NOOP
	}
}
