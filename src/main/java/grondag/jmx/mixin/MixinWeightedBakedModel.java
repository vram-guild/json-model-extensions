/*******************************************************************************
 * Copyright 2019 grondag
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/

package grondag.jmx.mixin;

import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import grondag.jmx.impl.TransformableModel;
import grondag.jmx.impl.TransformableModelContext;
import grondag.jmx.json.ext.ModelEntryAccess;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.WeightedBakedModel;
import net.minecraft.item.ItemStack;
import net.minecraft.util.WeightedPicker;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

@Environment(EnvType.CLIENT)
@Mixin(WeightedBakedModel.class)
public abstract class MixinWeightedBakedModel implements BakedModel, FabricBakedModel, TransformableModel {
    @SuppressWarnings("rawtypes")
    @Shadow private List models;
    @Shadow private int totalWeight;
    @Shadow private BakedModel defaultModel;
    
    @SuppressWarnings("unchecked")
    @Override
    public BakedModel derive(TransformableModelContext context) {
        WeightedBakedModel.Builder builder = new WeightedBakedModel.Builder();
        models.forEach(m -> {
            ModelEntryAccess me = (ModelEntryAccess)m;
            BakedModel template = me.jmx_getModel();
            BakedModel mNew = (template instanceof TransformableModel) ? ((TransformableModel)template).derive(context) : template;
            builder.add(mNew, me.jmx_getWeight());
        });
        return builder.getFirst();
    }
    
    @Override
    public boolean isVanillaAdapter() {
        return ((FabricBakedModel)defaultModel).isVanillaAdapter();
    }

    @Override
    public void emitBlockQuads(BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, RenderContext context) {
        final BakedModel model = getModel(randomSupplier.get());
        ((FabricBakedModel)model).emitBlockQuads(blockView, state, pos, randomSupplier, context);
    }

    @Override
    public void emitItemQuads(ItemStack stack, Supplier<Random> randomSupplier, RenderContext context) {
        final BakedModel model = getModel(randomSupplier.get());
        ((FabricBakedModel)model).emitItemQuads(stack, randomSupplier, context);
    }

    @SuppressWarnings("unchecked")
    private BakedModel getModel(Random random) {
        return ((ModelEntryAccess)WeightedPicker.getAt(models, Math.abs((int)random.nextLong()) % totalWeight)).jmx_getModel();
    }
}
