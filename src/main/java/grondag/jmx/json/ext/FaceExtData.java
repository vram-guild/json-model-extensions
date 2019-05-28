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
