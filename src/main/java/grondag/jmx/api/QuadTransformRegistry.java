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

package grondag.jmx.api;

import java.util.Random;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;

import grondag.jmx.impl.QuadTransformRegistryImpl;

public interface QuadTransformRegistry {
	interface QuadTransformSource {
		@Nullable
		RenderContext.QuadTransform getForBlock(BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier);
		@Nullable
		RenderContext.QuadTransform getForItem(ItemStack stack, Supplier<Random> randomSupplier);
	}

	QuadTransformRegistry INSTANCE = new QuadTransformRegistryImpl();

	void register(Identifier id, QuadTransformSource quadTransformSource);

	@Nullable QuadTransformSource getQuadTransform(Identifier id);
}
