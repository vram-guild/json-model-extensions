package grondag.jmx.api;

import grondag.jmx.impl.QuadTransformRegistryImpl;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

import javax.annotation.Nullable;
import java.util.Random;
import java.util.function.Supplier;

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
