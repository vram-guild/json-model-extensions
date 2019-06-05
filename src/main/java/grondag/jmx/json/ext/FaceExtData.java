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

package grondag.jmx.json.ext;

import com.google.gson.JsonObject;

import net.minecraft.client.render.model.json.ModelElementTexture;
import net.minecraft.util.JsonHelper;

public class FaceExtData {
    public static final ThreadLocal<FaceExtData> TRANSFER  = new ThreadLocal<>();
    
    public static final FaceExtData EMPTY = new FaceExtData();
    
    private FaceExtData() {
        jmx_tag = 0;
        jmx_tex0 = null;
        jmx_tex1 = null;
        jmx_material = null;
        jmx_texData0 = null;
        jmx_texData1 = null;
        
    }
    
    private FaceExtData(JsonObject jsonObj) {
        jmx_tag = JsonHelper.getInt(jsonObj, "jmx_tag", 0);
        jmx_tex0 = JsonHelper.getString(jsonObj, "jmx_tex0", null);
        jmx_tex1 = JsonHelper.getString(jsonObj, "jmx_tex1", null);
        jmx_material = JsonHelper.getString(jsonObj, "jmx_material", null);
        jmx_texData0 = null;
        jmx_texData1 = null;
    }

    public static void deserialize(JsonObject jsonObj) {
        TRANSFER.set(new FaceExtData(jsonObj));
    }
    
    public final int jmx_tag;
    public final String jmx_tex0;
    public final String jmx_tex1;
    public final String jmx_material;
    
    //TODO
    public final ModelElementTexture jmx_texData0;
    public final ModelElementTexture jmx_texData1;
}
