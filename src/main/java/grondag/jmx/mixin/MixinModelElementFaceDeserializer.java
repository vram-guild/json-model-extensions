package grondag.jmx.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.google.gson.JsonObject;

import grondag.jmx.json.ext.FaceExtData;
import net.minecraft.client.render.model.json.ModelElementFace;

@Mixin(ModelElementFace.Deserializer.class)
public class MixinModelElementFaceDeserializer {
    @ModifyVariable(method = "method_3397", at = @At(value = "STORE", ordinal = 0), allow = 1, require = 1)
    private JsonObject hookDeserialize(JsonObject jsonObj) {
        FaceExtData.deserialize(jsonObj);
        return jsonObj;
    }
}
