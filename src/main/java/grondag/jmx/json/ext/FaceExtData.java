package grondag.jmx.json.ext;

import com.google.gson.JsonObject;

import net.minecraft.client.render.model.json.ModelElementTexture;
import net.minecraft.util.JsonHelper;

public class FaceExtData {
    public static final ThreadLocal<FaceExtData> TRANSFER  = new ThreadLocal<>();
    
    public static void deserialize(JsonObject jsonObj) {
        FaceExtData result = new FaceExtData();
        result.jmx_tag = JsonHelper.getInt(jsonObj, "jmx_tag", 0);
        result.jmx_tex0 = JsonHelper.getString(jsonObj, "jmx_tex0", null);
        result.jmx_tex1 = JsonHelper.getString(jsonObj, "jmx_tex1", null);
        result.jmx_material = JsonHelper.getString(jsonObj, "jmx_material", null);
        
        TRANSFER.set(result);
    }
    
    public int jmx_tag;
    public String jmx_tex0;
    public String jmx_tex1;
    public String jmx_material;
    
    //TODO
    public ModelElementTexture jmx_texData0;
    public ModelElementTexture jmx_texData1;
}
