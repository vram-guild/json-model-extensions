package grondag.jmx.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.render.model.BakedModel;

@Mixin(targets = {"net/minecraft/client/render/model/WeightedBakedModel$ModelEntry"})
public interface MixinWeightedBakedModelEntry {
    @Accessor
    BakedModel getModel();
}
