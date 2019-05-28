package grondag.jmx.mixin;

import org.apache.commons.lang3.ObjectUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import grondag.jmx.json.ext.FaceExtData;
import grondag.jmx.json.ext.JmxExtension;
import net.minecraft.client.render.model.json.ModelElementFace;

@Mixin(ModelElementFace.class)
public class MixinModelElementFace implements JmxExtension<FaceExtData> {
    private FaceExtData jmx_ext;
    
    @Override
    public FaceExtData jmx_ext() {
        return jmx_ext;
    }

    @Override
    public void jmx_ext(FaceExtData val) {
        jmx_ext = val;
    }
    
    @Inject(at = @At("RETURN"), method = "<init>") 
    private void onInit(CallbackInfo ci) {
        jmx_ext = ObjectUtils.defaultIfNull(FaceExtData.TRANSFER.get(), FaceExtData.EMPTY);
    }
}
