package grondag.jmx.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.google.gson.JsonObject;

import grondag.jmx.json.ext.JmxModelExt;
import grondag.jmx.json.ext.JmxTexturesExt;
import net.minecraft.client.render.model.json.JsonUnbakedModel;

@Mixin(JsonUnbakedModel.Deserializer.class)
public class MixinJsonUnbakedModelDeserializer {
    @Inject(at = @At("RETURN"), method = "deserializeTextures")
    private void onDeserializeTextures(JsonObject jsonObj, CallbackInfoReturnable<Map<String, String>> ci) {
        JmxTexturesExt.handleJmxTextures(jsonObj, ci.getReturnValue());
    }
    
    @ModifyVariable(method = "method_3451", at = @At(value = "STORE", ordinal = 0), allow = 1, require = 1)
    private JsonObject hookDeserialize(JsonObject jsonObj) {
        JmxModelExt.deserialize(jsonObj);
        return jsonObj;
    }
}
