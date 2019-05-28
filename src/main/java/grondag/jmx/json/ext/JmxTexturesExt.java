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
