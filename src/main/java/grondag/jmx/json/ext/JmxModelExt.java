package grondag.jmx.json.ext;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import grondag.jmx.JsonModelExtensions;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class JmxModelExt {
    public static final ThreadLocal<JmxModelExt> TRANSFER  = new ThreadLocal<>();

    public JmxModelExt parent;

    private final Map<String, Object> map; 

    private JmxModelExt(Map<String, Object> map) {
        this.map = map;
    }

    public JmxMaterial resolveMaterial(String matName) {
        return matName == null || map == null ? JmxMaterial.DEFAULT : resolveMaterialInner(matName);
    }

    private JmxMaterial resolveMaterialInner(String matName) {
        if (!isMaterialReference(matName)) {
            matName = '#' + matName;
        }
        Object result = resolveMaterial(matName, new MaterialResolutionContext(this));
        return result != null && result instanceof JmxMaterial ? (JmxMaterial) result : JmxMaterial.DEFAULT;
    }

    private Object resolveMaterial(Object val, MaterialResolutionContext context) {

        if (isMaterialReference(val)) {
            if (this == context.current) {
                JsonModelExtensions.LOG.warn("Unable to resolve material due to upward reference: {}", val);
                return JmxMaterial.DEFAULT;
            } else {
                Object result = this.map.get(((String)val).substring(1));
                if(result != null && result instanceof JmxMaterial) {
                    return result;
                }

                if (result == null && parent != null) {
                    result = parent.resolveMaterial(val, context);
                }

                if (isMaterialReference(result)) {
                    context.current = this;
                    result = context.root.resolveMaterial((String)result, context);
                }

                return result;
            }
        } else {
            return val;
        }
    }

    public static boolean isMaterialReference(Object val) {
        return val instanceof String && ((String)val).charAt(0) == '#';
    }

    public static void deserialize(JsonObject jsonObj) {
        if(jsonObj.has("jmx_materials")) {
            final Object2ObjectOpenHashMap<String, Object> map = new Object2ObjectOpenHashMap<>();
            JsonObject job = jsonObj.getAsJsonObject("jmx_materials");
            for(Entry<String, JsonElement> e : job.entrySet()) {
                if(e.getValue().isJsonObject()) {
                    map.put(e.getKey(), new JmxMaterial(e.getKey(), e.getValue().getAsJsonObject()));
                } else {
                    map.put(e.getKey(), e.getValue().getAsString());
                }
            }
            TRANSFER.set(new JmxModelExt(map));
        } else {
            TRANSFER.set(new JmxModelExt(Collections.emptyMap()));
        }
    }

    public static final class MaterialResolutionContext {
        public final JmxModelExt root;
        public JmxModelExt current;

        private MaterialResolutionContext(JmxModelExt root) {
            this.root = root;
        }
    }
    
    //TODO: remove
    public static void boop() {
        System.out.print(false);
    }
}
