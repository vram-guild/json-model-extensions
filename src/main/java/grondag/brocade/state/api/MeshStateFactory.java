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

package grondag.brocade.state.api;

import java.util.function.Consumer;

import grondag.brocade.dispatch.api.MeshTransformer;
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ExtendedBlockView;

/**
 * Each base model has one of these that derives model state.
 */
public interface MeshStateFactory {
    // TODO: Should block state be here?  Will all factories have a fixed block state?
    MeshState compute(BlockState state, ExtendedBlockView world, BlockPos pos, Object renderData);
    
    void forEachTransform(Consumer<MeshTransformer> consumer);
    
    Mesh baseMesh();
}
