package grondag.jmx.api;

import java.util.Random;
import java.util.function.Supplier;

import grondag.jmx.impl.QuadTransformRegistryImpl;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;

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
