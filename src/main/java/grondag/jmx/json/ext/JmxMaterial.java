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

import java.util.Locale;

import com.google.gson.JsonObject;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.block.BlockRenderLayer;
import net.minecraft.util.JsonHelper;

@Environment(EnvType.CLIENT)
public class JmxMaterial {
    public static final JmxMaterial DEFAULT = new JmxMaterial();
    
    public final String id;
    public final String preset;
    public final TriState diffuse0;
    public final TriState ao0;
    public final TriState emissive0;
    public final TriState colorIndex0;
    
    public final TriState diffuse1;
    public final TriState ao1;
    public final TriState emissive1;
    public final TriState colorIndex1;
    public final int color0;
    public final int color1;
    
    public final BlockRenderLayer layer0;
    public final BlockRenderLayer layer1;
    
    public final int depth;
    public final int tag;
    
    private JmxMaterial() {
        id = "DEFAULT";
        preset = null;
        ao0 = TriState.DEFAULT;
        ao1 = TriState.DEFAULT;
        diffuse0 = TriState.DEFAULT;
        diffuse1 = TriState.DEFAULT;
        emissive0 = TriState.DEFAULT;
        emissive1 = TriState.DEFAULT;
        colorIndex0 = TriState.DEFAULT;
        colorIndex1 = TriState.DEFAULT;
        color0 = 0xFFFFFFFF;
        color1 = 0xFFFFFFFF;
        layer0 = null;
        layer1 = null;
        depth = 1;
        tag = 0;
    };
    
    public JmxMaterial(String id, JsonObject jsonObj) {
        this.id = id;
        preset = JsonHelper.getString(jsonObj, "preset", null);
        tag = JsonHelper.getInt(jsonObj, "tag", 0);
        ao0 = asTriState(JsonHelper.getString(jsonObj, "ambient_occlusion0", null));
        ao1 = asTriState(JsonHelper.getString(jsonObj, "ambient_occlusion1", null));
        diffuse0 = asTriState(JsonHelper.getString(jsonObj, "diffuse0", null));
        diffuse1 = asTriState(JsonHelper.getString(jsonObj, "diffuse1", null));
        emissive0 = asTriState(JsonHelper.getString(jsonObj, "emissive0", null));
        emissive1 = asTriState(JsonHelper.getString(jsonObj, "emissive1", null));
        colorIndex0 = asTriState(JsonHelper.getString(jsonObj, "colorIndex0", null));
        colorIndex1 = asTriState(JsonHelper.getString(jsonObj, "colorIndex1", null));
        color0 = color(JsonHelper.getString(jsonObj, "color0", "0xFFFFFFFF"));
        color1 = color(JsonHelper.getString(jsonObj, "color1", "0xFFFFFFFF"));
        layer0 = asLayer(JsonHelper.getString(jsonObj, "layer0", null));
        layer1 = asLayer(JsonHelper.getString(jsonObj, "layer1", null));
        
        int depth = JsonHelper.getInt(jsonObj, "depth", 1);
        // force depth to 2 if attributes for 2nd layer are given
        if(depth == 1 && (ao1 != TriState.DEFAULT || diffuse1 != TriState.DEFAULT || emissive1 != TriState.DEFAULT || color1 != -1)) {
            depth = 2;
        }
        this.depth = depth;
    };
    
    private static int color(String str) {
        return str.startsWith("0x") ? Integer.parseUnsignedInt(str.substring(2), 16) : Integer.parseInt(str);
    }
    
    private static BlockRenderLayer asLayer(String property) {
        if (property == null || property.isEmpty()) {
            return null;
        } else {
            switch (property.toLowerCase(Locale.ROOT)) {
            case "solid":
                return BlockRenderLayer.SOLID;
            case "cutout":
                return BlockRenderLayer.CUTOUT;
            case "cutout_mipped":
                return BlockRenderLayer.CUTOUT_MIPPED;
            case "translucent":
                return BlockRenderLayer.TRANSLUCENT;
            default:
                return null;
        }
        }
    }
    
    private static TriState asTriState(String property) {
        if (property == null || property.isEmpty()) {
            return TriState.DEFAULT;
        } else {
            switch (property.toLowerCase(Locale.ROOT)) {
                case "true":
                case "yes":
                case "1":
                case "y":
                    return TriState.TRUE;
                case "false":
                case "no":
                case "0":
                case "n":
                    return TriState.FALSE;
                default:
                    return TriState.DEFAULT;
            }
        }
    }
}
