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
import java.util.function.Predicate;

import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.MultiPartBakedModel;
import net.minecraft.world.level.block.state.BlockState;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import grondag.jmx.impl.InverseStateMap;
import grondag.jmx.impl.TransformableModel;
import grondag.jmx.impl.TransformableModelContext;

@Environment(EnvType.CLIENT)
@Mixin(MultiPartBakedModel.class)
public abstract class MixinMultipartBakedModel implements TransformableModel {
	@Shadow protected List<Pair<Predicate<BlockState>, BakedModel>> selectors;
	@Shadow protected Map<BlockState, BitSet> selectorCache;

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
}
