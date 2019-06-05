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

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.MultipartBakedModel;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SystemUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ExtendedBlockView;

@Mixin(MultipartBakedModel.class)
public class MixinMultipartBakedModel implements FabricBakedModel {
    @Shadow private List<Pair<Predicate<BlockState>, BakedModel>> components;
    @Shadow private Map<BlockState, BitSet> field_5431 = new Object2ObjectOpenCustomHashMap<>(SystemUtil.identityHashStrategy());
    
    private boolean isVanilla = true;

    @Inject(at = @At("RETURN"), method = "<init>")
    private void onInit(List<Pair<Predicate<BlockState>, BakedModel>> list, CallbackInfo ci) {
        BakedModel defaultModel = list.iterator().next().getRight();
        isVanilla = ((FabricBakedModel)defaultModel).isVanillaAdapter();
    }
    
    @Override
    public boolean isVanillaAdapter() {
        return isVanilla;
    }

    @Override
    public void emitBlockQuads(ExtendedBlockView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, RenderContext context) {
        if (state == null) {
            return;
         } else {
            BitSet bits = field_5431.get(state);
            
            if (bits == null) {
               bits = new BitSet();

               for(int i = 0; i < this.components.size(); ++i) {
                  Pair<Predicate<BlockState>, BakedModel> pair = components.get(i);
                  if (pair.getLeft().test(state)) {
                     bits.set(i);
                  }
               }
               this.field_5431.put(state, bits);
            }

            final int limit = bits.length();
            for(int i = 0; i < limit; ++i) {
               if (bits.get(i)) {
                  ((FabricBakedModel)components.get(i).getRight()).emitBlockQuads(blockView, state, pos, randomSupplier, context);
               }
            }
         }        
    }

    @Override
    public void emitItemQuads(ItemStack stack, Supplier<Random> randomSupplier, RenderContext context) {
        //NOOP
    }
}
