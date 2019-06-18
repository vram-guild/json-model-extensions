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

import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;

/**
 * An actual value.
 */
public interface MeshState {
    /** Use when certain instance will no longer be used - returns it to pool for reuse. */
    void release();
    
    /** Factory that produced this state */
    MeshStateFactory owner();
    
    /**
     *  Will get base and transforms from owner and
     *  then apply them to produce a new mesh.
     */
    Mesh produceMesh(); 
}
