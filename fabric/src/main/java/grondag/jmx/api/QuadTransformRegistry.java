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

package grondag.jmx.api;

import java.util.Random;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;

import grondag.jmx.impl.QuadTransformRegistryImpl;

public interface QuadTransformRegistry {
	interface QuadTransformSource {
		@Nullable
		RenderContext.QuadTransform getForBlock(BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier);
		@Nullable
		RenderContext.QuadTransform getForItem(ItemStack stack, Supplier<Random> randomSupplier);
	}

	QuadTransformRegistry INSTANCE = new QuadTransformRegistryImpl();

	void register(ResourceLocation id, QuadTransformSource quadTransformSource);

	@Nullable QuadTransformSource getQuadTransform(ResourceLocation id);
}
