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

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public final class JmxTexturesExt {
    public static void handleJmxTextures(JsonObject jsonObj, Map<String,String> map) {
        if(jsonObj.has("jmx_textures")) {
            JsonObject job = jsonObj.getAsJsonObject("jmx_textures");
            Iterator<Entry<String, JsonElement>> it = job.entrySet().iterator();
            while(it.hasNext()) {
               Entry<String, JsonElement> entry = it.next();
               map.put(entry.getKey(), entry.getValue().getAsString());
            }
        }
    }
}
